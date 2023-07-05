package com.example.musify.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.example.musify.data.repositories.podcastsrepository.PodcastQuery
import com.example.musify.data.repositories.podcastsrepository.PodcastsRepository
import com.example.musify.data.tiling.Page
import com.example.musify.data.tiling.toTiledList
import com.example.musify.data.utils.FetchedResource
import com.example.musify.domain.PodcastEpisode
import com.example.musify.domain.PodcastShow
import com.example.musify.ui.navigation.MusifyNavigationDestinations
import com.example.musify.usecases.getCurrentlyPlayingEpisodePlaybackStateUseCase.GetCurrentlyPlayingEpisodePlaybackStateUseCase
import com.tunjid.mutator.Mutation
import com.tunjid.mutator.coroutines.SuspendingStateHolder
import com.tunjid.mutator.coroutines.actionStateFlowProducer
import com.tunjid.mutator.coroutines.mapToMutation
import com.tunjid.mutator.coroutines.toMutationStream
import com.tunjid.tiler.TiledList
import com.tunjid.tiler.distinctBy
import com.tunjid.tiler.emptyTiledList
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import com.example.musify.usecases.getCurrentlyPlayingEpisodePlaybackStateUseCase.GetCurrentlyPlayingEpisodePlaybackStateUseCase.PlaybackState as UseCasePlaybackState

data class PodcastShowDetailState(
    val currentQuery: PodcastQuery,
    val podcastShow: PodcastShow? = null,
    val currentlyPlayingEpisode: PodcastEpisode? = null,
    val isCurrentlyPlayingEpisodePaused: Boolean? = null,
    val loadingState: PodcastShowDetailViewModel.LoadingState = PodcastShowDetailViewModel.LoadingState.LOADING,
    val episodesForShow: TiledList<PodcastQuery, PodcastEpisode> = emptyTiledList()
)

sealed class PodcastShowDetailAction {
    object Retry : PodcastShowDetailAction()
    data class LoadAround(val podcastQuery: PodcastQuery) : PodcastShowDetailAction()
}

@HiltViewModel
class PodcastShowDetailViewModel @Inject constructor(
    application: Application,
    savedStateHandle: SavedStateHandle,
    getCurrentlyPlayingEpisodePlaybackStateUseCase: GetCurrentlyPlayingEpisodePlaybackStateUseCase,
    podcastsRepository: PodcastsRepository
) : AndroidViewModel(application) {

    enum class LoadingState { IDLE, LOADING, PLAYBACK_LOADING, ERROR }

    private val showId =
        savedStateHandle.get<String>(MusifyNavigationDestinations.PodcastShowDetailScreen.NAV_ARG_PODCAST_SHOW_ID)!!

    private val stateProducer =
        viewModelScope.actionStateFlowProducer<PodcastShowDetailAction, PodcastShowDetailState>(
            initialState = PodcastShowDetailState(
                currentQuery = PodcastQuery(
                    showId = showId,
                    countryCode = getCountryCode(),
                    page = Page(offset = 0)
                )
            ),
            mutationFlows = listOf(
                getCurrentlyPlayingEpisodePlaybackStateUseCase.playbackStateMutations(),
                podcastsRepository.fetchShowMutations(
                    showId = showId,
                    countryCode = getCountryCode()
                )
            ),
            actionTransform = { actions ->
                actions.toMutationStream {
                    when (val action = type()) {
                        is PodcastShowDetailAction.LoadAround -> action.flow.episodeMutations(
                            podcastsRepository = podcastsRepository
                        )

                        is PodcastShowDetailAction.Retry -> action.flow.retryMutations(
                            podcastsRepository = podcastsRepository,
                            showId = showId,
                            countryCode = getCountryCode()
                        )
                    }
                }
            }
        )

    val state = stateProducer.state
    val actions = stateProducer.accept
}

private fun GetCurrentlyPlayingEpisodePlaybackStateUseCase.playbackStateMutations(): Flow<Mutation<PodcastShowDetailState>> =
    currentlyPlayingEpisodePlaybackStateStream
        .mapToMutation {
            when (it) {
                is UseCasePlaybackState.Ended -> copy(
                    isCurrentlyPlayingEpisodePaused = null,
                    currentlyPlayingEpisode = null
                )

                is UseCasePlaybackState.Loading -> copy(
                    loadingState = PodcastShowDetailViewModel.LoadingState.PLAYBACK_LOADING
                )

                is UseCasePlaybackState.Paused -> copy(
                    currentlyPlayingEpisode = it.pausedEpisode,
                    isCurrentlyPlayingEpisodePaused = true
                )

                is UseCasePlaybackState.Playing -> copy(
                    loadingState =
                    if (loadingState != PodcastShowDetailViewModel.LoadingState.IDLE) PodcastShowDetailViewModel.LoadingState.IDLE
                    else loadingState,
                    isCurrentlyPlayingEpisodePaused =
                    if (isCurrentlyPlayingEpisodePaused == null || isCurrentlyPlayingEpisodePaused == true) false
                    else isCurrentlyPlayingEpisodePaused,
                    currentlyPlayingEpisode = it.playingEpisode
                )
            }
        }

private fun Flow<PodcastShowDetailAction.Retry>.retryMutations(
    podcastsRepository: PodcastsRepository,
    showId: String,
    countryCode: String
): Flow<Mutation<PodcastShowDetailState>> =
    flatMapLatest {
        podcastsRepository.fetchShowMutations(
            showId = showId,
            countryCode = countryCode
        )
    }

context(SuspendingStateHolder<PodcastShowDetailState>)
private suspend fun Flow<PodcastShowDetailAction.LoadAround>.episodeMutations(
    podcastsRepository: PodcastsRepository
): Flow<Mutation<PodcastShowDetailState>> =
    map { it.podcastQuery }
        .toTiledList(
            startQuery = state().currentQuery,
            queryFor = { copy(page = it) },
            fetcher = podcastsRepository::podcastsFor
        )
        .mapToMutation {
            copy(episodesForShow = it.distinctBy(PodcastEpisode::id))
        }

private fun PodcastsRepository.fetchShowMutations(
    showId: String,
    countryCode: String
) = flow<Mutation<PodcastShowDetailState>> {
    val result = fetchPodcastShow(
        showId = showId,
        countryCode = countryCode,
    )
    if (result is FetchedResource.Success) emit {
        copy(
            loadingState = PodcastShowDetailViewModel.LoadingState.IDLE,
            podcastShow = result.data
        )
    } else emit {
        copy(loadingState = PodcastShowDetailViewModel.LoadingState.ERROR)
    }
}
