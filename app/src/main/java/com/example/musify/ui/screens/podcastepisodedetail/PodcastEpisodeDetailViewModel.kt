package com.example.musify.ui.screens.podcastepisodedetail

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.example.musify.data.repositories.podcastsrepository.PodcastsRepository
import com.example.musify.data.utils.FetchedResource
import com.example.musify.domain.PodcastEpisode
import com.example.musify.domain.equalsIgnoringImageSize
import com.example.musify.ui.navigation.MusifyNavigationDestinations
import com.example.musify.usecases.getCurrentlyPlayingEpisodePlaybackStateUseCase.GetCurrentlyPlayingEpisodePlaybackStateUseCase
import com.example.musify.viewmodels.getCountryCode
import com.tunjid.mutator.Mutation
import com.tunjid.mutator.coroutines.actionStateFlowProducer
import com.tunjid.mutator.coroutines.mapToMutation
import com.tunjid.mutator.coroutines.toMutationStream
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import com.example.musify.usecases.getCurrentlyPlayingEpisodePlaybackStateUseCase.GetCurrentlyPlayingEpisodePlaybackStateUseCase.PlaybackState as UseCasePlaybackState

data class PodcastEpisodeDetailState(
    val loadingState: PodcastEpisodeDetailViewModel.LoadingState = PodcastEpisodeDetailViewModel.LoadingState.LOADING,
    val currentlyPlayingEpisode: PodcastEpisode? = null,
    val podcastEpisode: PodcastEpisode? = null,
)

sealed class PodcastEpisodeAction {
    object Retry : PodcastEpisodeAction()
}

val PodcastEpisodeDetailState.isEpisodeCurrentlyPlaying
    get() = currentlyPlayingEpisode.equalsIgnoringImageSize(podcastEpisode)

@HiltViewModel
class PodcastEpisodeDetailViewModel @Inject constructor(
    application: Application,
    private val podcastsRepository: PodcastsRepository,
    private val savedStateHandle: SavedStateHandle,
    getCurrentlyPlayingEpisodePlaybackStateUseCase: GetCurrentlyPlayingEpisodePlaybackStateUseCase,
) : AndroidViewModel(application) {

    enum class LoadingState { IDLE, LOADING, PLAYBACK_LOADING, ERROR }

    private val episodeId = savedStateHandle.get<String>(MusifyNavigationDestinations.PodcastEpisodeDetailScreen.NAV_ARG_PODCAST_EPISODE_ID)!!
    private val stateProducer =
        viewModelScope.actionStateFlowProducer<PodcastEpisodeAction, PodcastEpisodeDetailState>(
            initialState = PodcastEpisodeDetailState(),
            mutationFlows = listOf(
                getCurrentlyPlayingEpisodePlaybackStateUseCase.playbackStateMutations(),
                podcastsRepository.fetchEpisodeMutations(
                    episodeId = episodeId,
                    countryCode = getCountryCode()
                )
            ),
            actionTransform = { actions ->
                actions.toMutationStream {
                    when (val action = type()) {
                        is PodcastEpisodeAction.Retry -> action.flow.flatMapLatest {
                            podcastsRepository.fetchEpisodeMutations(
                                episodeId = episodeId,
                                countryCode = getCountryCode()
                            )
                        }
                    }
                }
            }
        )

    val state = stateProducer.state
    val actions = stateProducer.accept
}

private fun GetCurrentlyPlayingEpisodePlaybackStateUseCase.playbackStateMutations(): Flow<Mutation<PodcastEpisodeDetailState>> =
    currentlyPlayingEpisodePlaybackStateStream
        .mapToMutation {
            when (it) {
                is UseCasePlaybackState.Paused,
                is UseCasePlaybackState.Ended -> copy(
                    currentlyPlayingEpisode = null
                )

                is UseCasePlaybackState.Loading -> copy(
                    loadingState = PodcastEpisodeDetailViewModel.LoadingState.PLAYBACK_LOADING
                )

                is UseCasePlaybackState.Playing -> copy(
                    loadingState =
                    if (loadingState != PodcastEpisodeDetailViewModel.LoadingState.IDLE) PodcastEpisodeDetailViewModel.LoadingState.IDLE
                    else loadingState,
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
): Flow<Mutation<PodcastEpisodeDetailState>> = flow {
    emit {
        copy(loadingState = PodcastEpisodeDetailViewModel.LoadingState.LOADING)
    }
    val fetchedResource = fetchPodcastEpisode(
        episodeId = episodeId,
        countryCode = countryCode
    )
    val episode = if (fetchedResource is FetchedResource.Success) fetchedResource.data else null
    emit {
        copy(
            podcastEpisode = episode,
            loadingState = if (episode == null) PodcastEpisodeDetailViewModel.LoadingState.ERROR else PodcastEpisodeDetailViewModel.LoadingState.IDLE
        )
    }
}
