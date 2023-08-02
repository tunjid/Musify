package com.example.musify.ui.screens.podcastshowdetailscreen

import com.example.musify.data.repositories.podcastsrepository.PodcastQuery
import com.example.musify.data.repositories.podcastsrepository.PodcastsRepository
import com.example.musify.data.tiling.Page
import com.example.musify.data.tiling.PagedItem
import com.example.musify.data.tiling.toNetworkBackedTiledList
import com.example.musify.data.tiling.withPlaceholders
import com.example.musify.data.utils.FetchedResource
import com.example.musify.domain.PodcastEpisode
import com.example.musify.domain.PodcastShow
import com.example.musify.musicplayer.MusicPlaybackMonitor
import com.example.musify.musicplayer.currentlyPlayingEpisodePlaybackStateStream
import com.tunjid.mutator.Mutation
import com.tunjid.mutator.coroutines.SuspendingStateHolder
import com.tunjid.mutator.coroutines.actionStateFlowProducer
import com.tunjid.mutator.coroutines.mapToMutation
import com.tunjid.mutator.coroutines.toMutationStream
import com.tunjid.tiler.TiledList
import com.tunjid.tiler.distinctBy
import com.tunjid.tiler.emptyTiledList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map

sealed class PodcastShowDetailAction {
    object Retry : PodcastShowDetailAction()
    data class LoadAround(val podcastQuery: PodcastQuery?) : PodcastShowDetailAction()
}

data class PodcastShowDetailUiState(
    val currentQuery: PodcastQuery,
    val podcastShow: PodcastShow? = null,
    val currentlyPlayingEpisode: PodcastEpisode? = null,
    val isCurrentlyPlayingEpisodePaused: Boolean? = null,
    val loadingState: LoadingState = LoadingState.LOADING,
    val items: TiledList<PodcastQuery, ShowItem> = emptyTiledList()
) {
    enum class LoadingState { IDLE, LOADING, PLAYBACK_LOADING, ERROR }
}

sealed interface ShowItem: PagedItem {
    data class Loaded(
        override val pagedIndex: Int,
        val episode: PodcastEpisode
    ) : ShowItem

    data class Placeholder(
        override val pagedIndex: Int,
    ) : ShowItem
}

private val ShowItem.internalKey
    get() = when (this) {
        is ShowItem.Loaded -> episode.id
        is ShowItem.Placeholder -> pagedIndex
    }

fun CoroutineScope.podcastShowDetailStateProducer(
    showId: String,
    countryCode: String,
    podcastsRepository: PodcastsRepository,
    musicPlaybackMonitor: MusicPlaybackMonitor,
) = actionStateFlowProducer<PodcastShowDetailAction, PodcastShowDetailUiState>(
    initialState = PodcastShowDetailUiState(
        currentQuery = PodcastQuery(
            showId = showId,
            countryCode = countryCode,
            page = Page(offset = 0)
        )
    ),
    mutationFlows = listOf(
        musicPlaybackMonitor.playbackStateMutations(),
        podcastsRepository.fetchShowMutations(
            showId = showId,
            countryCode = countryCode
        )
    ),
    actionTransform = { actions ->
        actions.toMutationStream {
            when (val action = type()) {
                is PodcastShowDetailAction.LoadAround -> action.flow.episodeLoadMutations(
                    podcastsRepository = podcastsRepository
                )

                is PodcastShowDetailAction.Retry -> action.flow.retryMutations(
                    podcastsRepository = podcastsRepository,
                    showId = showId,
                    countryCode = countryCode
                )
            }
        }
    }
)

private fun MusicPlaybackMonitor.playbackStateMutations(): Flow<Mutation<PodcastShowDetailUiState>> =
    currentlyPlayingEpisodePlaybackStateStream
        .mapToMutation {
            when (it) {
                is MusicPlaybackMonitor.PlaybackState.Ended -> copy(
                    isCurrentlyPlayingEpisodePaused = null,
                    currentlyPlayingEpisode = null
                )

                is MusicPlaybackMonitor.PlaybackState.Loading -> copy(
                    loadingState = PodcastShowDetailUiState.LoadingState.PLAYBACK_LOADING
                )

                is MusicPlaybackMonitor.PlaybackState.Paused -> copy(
                    currentlyPlayingEpisode = it.pausedEpisode,
                    isCurrentlyPlayingEpisodePaused = true
                )

                is MusicPlaybackMonitor.PlaybackState.Playing -> copy(
                    loadingState = PodcastShowDetailUiState.LoadingState.IDLE,
                    isCurrentlyPlayingEpisodePaused = when (isCurrentlyPlayingEpisodePaused) {
                        null, true -> false
                        else -> isCurrentlyPlayingEpisodePaused
                    },
                    currentlyPlayingEpisode = it.playingEpisode
                )
            }
        }

private fun Flow<PodcastShowDetailAction.Retry>.retryMutations(
    podcastsRepository: PodcastsRepository,
    showId: String,
    countryCode: String
): Flow<Mutation<PodcastShowDetailUiState>> =
    flatMapLatest {
        podcastsRepository.fetchShowMutations(
            showId = showId,
            countryCode = countryCode
        )
    }

context(SuspendingStateHolder<PodcastShowDetailUiState>)
private suspend fun Flow<PodcastShowDetailAction.LoadAround>.episodeLoadMutations(
    podcastsRepository: PodcastsRepository
): Flow<Mutation<PodcastShowDetailUiState>> =
    map { it.podcastQuery ?: state().currentQuery }
        .toNetworkBackedTiledList(
            startQuery = state().currentQuery,
            fetcher = podcastsRepository::podcastsFor.withPlaceholders(
                placeholderMapper = ShowItem::Placeholder,
                loadedMapper = ShowItem::Loaded
            )
        )
        .mapToMutation { items ->
            copy(
                items = items.distinctBy(ShowItem::internalKey)
            )
        }

private fun PodcastsRepository.fetchShowMutations(
    showId: String,
    countryCode: String
) = flow<Mutation<PodcastShowDetailUiState>> {
    val result = fetchPodcastShow(
        showId = showId,
        countryCode = countryCode,
    )
    if (result is FetchedResource.Success) emit {
        copy(
            loadingState = PodcastShowDetailUiState.LoadingState.IDLE,
            podcastShow = result.data
        )
    } else emit {
        copy(loadingState = PodcastShowDetailUiState.LoadingState.ERROR)
    }
}
