package com.example.musify.ui.screens.podcastepisodedetail

import com.example.musify.data.repositories.podcastsrepository.PodcastsRepository
import com.example.musify.data.utils.FetchedResource
import com.example.musify.domain.PodcastEpisode
import com.example.musify.domain.equalsIgnoringImageSize
import com.example.musify.usecases.getCurrentlyPlayingEpisodePlaybackStateUseCase.GetCurrentlyPlayingEpisodePlaybackStateUseCase
import com.tunjid.mutator.Mutation
import com.tunjid.mutator.coroutines.actionStateFlowProducer
import com.tunjid.mutator.coroutines.mapToMutation
import com.tunjid.mutator.coroutines.toMutationStream
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow

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
    podcastsRepository: PodcastsRepository,
    getCurrentlyPlayingEpisodePlaybackStateUseCase: GetCurrentlyPlayingEpisodePlaybackStateUseCase
) = actionStateFlowProducer<PodcastEpisodeAction, PodcastEpisodeDetailUiState>(
    initialState = PodcastEpisodeDetailUiState(),
    mutationFlows = listOf(
        getCurrentlyPlayingEpisodePlaybackStateUseCase.playbackStateMutations(),
        podcastsRepository.fetchEpisodeMutations(
            episodeId = episodeId,
            countryCode = countryCode
        )
    ),
    actionTransform = { actions ->
        actions.toMutationStream {
            when (val action = type()) {
                is PodcastEpisodeAction.Retry -> action.flow.flatMapLatest {
                    podcastsRepository.fetchEpisodeMutations(
                        episodeId = episodeId,
                        countryCode = countryCode
                    )
                }
            }
        }
    }
)

private fun GetCurrentlyPlayingEpisodePlaybackStateUseCase.playbackStateMutations(): Flow<Mutation<PodcastEpisodeDetailUiState>> =
    currentlyPlayingEpisodePlaybackStateStream
        .mapToMutation {
            when (it) {
                is GetCurrentlyPlayingEpisodePlaybackStateUseCase.PlaybackState.Paused,
                is GetCurrentlyPlayingEpisodePlaybackStateUseCase.PlaybackState.Ended -> copy(
                    currentlyPlayingEpisode = null
                )

                is GetCurrentlyPlayingEpisodePlaybackStateUseCase.PlaybackState.Loading -> copy(
                    loadingState = PodcastEpisodeDetailUiState.LoadingState.PLAYBACK_LOADING
                )

                is GetCurrentlyPlayingEpisodePlaybackStateUseCase.PlaybackState.Playing -> copy(
                    loadingState = PodcastEpisodeDetailUiState.LoadingState.IDLE,
                    // Initially this.podcastEpisode might be null when the
                    // flow sends it's first emission. This makes it impossible
                    // to compare this.podcastEpisode and it.playingEpisode.
                    // Therefore, assign the property to a state variable.
                    currentlyPlayingEpisode = it.playingEpisode
                )
            }
        }

private fun PodcastsRepository.fetchEpisodeMutations(
    episodeId: String,
    countryCode: String,
): Flow<Mutation<PodcastEpisodeDetailUiState>> = flow {
    emit {
        copy(loadingState = PodcastEpisodeDetailUiState.LoadingState.LOADING)
    }
    val fetchedResource = fetchPodcastEpisode(
        episodeId = episodeId,
        countryCode = countryCode
    )
    val episode = if (fetchedResource is FetchedResource.Success) fetchedResource.data else null
    emit {
        copy(
            podcastEpisode = episode,
            loadingState = if (episode == null) PodcastEpisodeDetailUiState.LoadingState.ERROR else PodcastEpisodeDetailUiState.LoadingState.IDLE
        )
    }
}
