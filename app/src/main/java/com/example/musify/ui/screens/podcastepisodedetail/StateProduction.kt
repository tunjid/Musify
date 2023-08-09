package com.example.musify.ui.screens.podcastepisodedetail

import com.example.musify.data.repositories.podcastsrepository.PodcastsRepository
import com.example.musify.data.utils.FetchedResource
import com.example.musify.data.utils.NetworkMonitor
import com.example.musify.data.utils.onConnected
import com.example.musify.domain.PodcastEpisode
import com.example.musify.domain.equalsIgnoringImageSize
import com.example.musify.musicplayer.MusicPlaybackMonitor
import com.example.musify.musicplayer.currentlyPlayingEpisodePlaybackStateStream
import com.tunjid.mutator.Mutation
import com.tunjid.mutator.coroutines.actionStateFlowProducer
import com.tunjid.mutator.coroutines.mapToMutation
import com.tunjid.mutator.coroutines.toMutationStream
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map

sealed class PodcastEpisodeAction {
    object Retry : PodcastEpisodeAction()
}

data class PodcastEpisodeDetailUiState(
    val loadingState: LoadingState = LoadingState.LOADING,
    val currentlyPlayingEpisode: PodcastEpisode? = null,
    val podcastEpisode: PodcastEpisode? = null,
) {
    enum class LoadingState { IDLE, LOADING, PLAYBACK_LOADING, ERROR }
}

val PodcastEpisodeDetailUiState.isEpisodeCurrentlyPlaying
    get() = currentlyPlayingEpisode.equalsIgnoringImageSize(podcastEpisode)

fun CoroutineScope.podcastEpisodeDetailStateProducer(
    episodeId: String,
    countryCode: String,
    networkMonitor: NetworkMonitor,
    musicPlaybackMonitor: MusicPlaybackMonitor,
    podcastsRepository: PodcastsRepository,
) = actionStateFlowProducer<PodcastEpisodeAction, PodcastEpisodeDetailUiState>(
    initialState = PodcastEpisodeDetailUiState(),
    mutationFlows = listOf(
        musicPlaybackMonitor.playbackStateMutations(),
        podcastsRepository.fetchEpisodeMutations(
            episodeId = episodeId,
            countryCode = countryCode,
            networkMonitor = networkMonitor,
        )
    ),
    actionTransform = { actions ->
        actions.toMutationStream {
            when (val action = type()) {
                is PodcastEpisodeAction.Retry -> action.flow.flatMapLatest {
                    podcastsRepository.fetchEpisodeMutations(
                        episodeId = episodeId,
                        countryCode = countryCode,
                        networkMonitor = networkMonitor,
                    )
                }
            }
        }
    }
)

private fun MusicPlaybackMonitor.playbackStateMutations(): Flow<Mutation<PodcastEpisodeDetailUiState>> =
    currentlyPlayingEpisodePlaybackStateStream
        .mapToMutation { playbackState ->
            when (playbackState) {
                is MusicPlaybackMonitor.PlaybackState.Paused,
                is MusicPlaybackMonitor.PlaybackState.Ended -> copy(
                    currentlyPlayingEpisode = null
                )

                is MusicPlaybackMonitor.PlaybackState.Loading -> copy(
                    loadingState = PodcastEpisodeDetailUiState.LoadingState.PLAYBACK_LOADING
                )

                is MusicPlaybackMonitor.PlaybackState.Playing -> copy(
                    loadingState = PodcastEpisodeDetailUiState.LoadingState.IDLE,
                    // Initially this.podcastEpisode might be null when the
                    // flow sends it's first emission. This makes it impossible
                    // to compare this.podcastEpisode and it.playingEpisode.
                    // Therefore, assign the property to a state variable.
                    currentlyPlayingEpisode = playbackState.playingEpisode
                )
            }
        }

private fun PodcastsRepository.fetchEpisodeMutations(
    episodeId: String,
    countryCode: String,
    networkMonitor: NetworkMonitor,
): Flow<Mutation<PodcastEpisodeDetailUiState>> =
    networkMonitor.onConnected()
        .map {
            fetchPodcastEpisode(
                episodeId = episodeId,
                countryCode = countryCode
            )
        }
        .mapToMutation { fetchedResource ->
            val episode = when (fetchedResource) {
                is FetchedResource.Success -> fetchedResource.data
                else -> null
            }
            copy(
                podcastEpisode = episode,
                loadingState = when (episode) {
                    null -> PodcastEpisodeDetailUiState.LoadingState.ERROR
                    else -> PodcastEpisodeDetailUiState.LoadingState.IDLE
                }
            )
        }
