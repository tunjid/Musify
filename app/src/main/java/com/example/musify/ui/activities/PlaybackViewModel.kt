package com.example.musify.ui.activities

import android.app.Application
import android.content.Context
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.musify.domain.PodcastEpisode
import com.example.musify.domain.SearchResult
import com.example.musify.domain.Streamable
import com.example.musify.musicplayer.MusicPlayerV2
import com.example.musify.usecases.downloadDrawableFromUrlUseCase.DownloadDrawableFromUrlUseCase
import com.tunjid.mutator.Mutation
import com.tunjid.mutator.coroutines.actionStateFlowProducer
import com.tunjid.mutator.coroutines.mapLatestToManyMutations
import com.tunjid.mutator.coroutines.mapLatestToMutation
import com.tunjid.mutator.coroutines.toMutationStream
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import java.util.concurrent.TimeUnit
import javax.inject.Inject

data class PlaybackScreenState(
    val playbackState: PlaybackViewModel.PlaybackState = PlaybackViewModel.PlaybackState.Idle,
    val currentTrackDuration: Long? = 0L,
    val currentTrackElapsed: Long? = 0L,
)

val PlaybackScreenState.totalDurationOfCurrentTrackTimeText: String
    get() = currentTrackDuration?.let(::convertTimestampMillisToString) ?: "00:00"
val PlaybackScreenState.currentTrackProgressText: String
    get() = currentTrackElapsed?.let(::convertTimestampMillisToString) ?: "00:00"

val PlaybackScreenState.currentTrackProgress: Float
    get() = when {
        currentTrackDuration != null && currentTrackElapsed != null ->
            (currentTrackElapsed.toFloat() / currentTrackDuration) * 100f

        else -> 0f
    }

sealed class PlaybackScreenAction {
    data class Play(val streamable: Streamable) : PlaybackScreenAction()

    data class Toggle(val streamable: Streamable) : PlaybackScreenAction()

    object Pause : PlaybackScreenAction()

}

@HiltViewModel
class PlaybackViewModel @Inject constructor(
    application: Application,
    private val musicPlayer: MusicPlayerV2,
    private val downloadDrawableFromUrlUseCase: DownloadDrawableFromUrlUseCase
) : AndroidViewModel(application) {

    private val stateProducer =
        viewModelScope.actionStateFlowProducer<PlaybackScreenAction, PlaybackScreenState>(
            initialState = PlaybackScreenState(),
            mutationFlows = listOf(
                musicPlayer.playbackMutations(),
                musicPlayer.playbackProgressMutations(),
            ),
            actionTransform = { actions ->
                actions.toMutationStream {
                    when (val action = type()) {
                        is PlaybackScreenAction.Pause -> action.flow.pauseMutations(
                            musicPlayer = musicPlayer,
                        )

                        is PlaybackScreenAction.Play -> action.flow.playMutations(
                            musicPlayer = musicPlayer,
                            downloadDrawableFromUrlUseCase = downloadDrawableFromUrlUseCase,
                            context = getApplication(),
                        )

                        is PlaybackScreenAction.Toggle -> action.flow.toggleMutations(
                            musicPlayer = musicPlayer,
                            downloadDrawableFromUrlUseCase = downloadDrawableFromUrlUseCase,
                            context = getApplication(),
                        )
                    }
                }
            }
        )

    val state = stateProducer.state
    val actions = stateProducer.accept

    companion object {
        val PLAYBACK_PROGRESS_RANGE = 0f..100f
    }

    sealed class PlaybackState(
        val currentlyPlayingStreamable: Streamable? = null,
        val previouslyPlayingStreamable: Streamable? = null
    ) {
        object Idle : PlaybackState()
        object Stopped : PlaybackState()
        data class Error(val errorMessage: String) : PlaybackState()
        data class Paused(val streamable: Streamable) : PlaybackState(streamable)
        data class Playing(val streamable: Streamable) : PlaybackState(streamable)
        data class PlaybackEnded(val streamable: Streamable) : PlaybackState(streamable)
        data class Loading(
            // Streamable instance that indicates the track that was playing before
            // the state was changed to loading
            val previousStreamable: Streamable?
        ) : PlaybackState(previouslyPlayingStreamable = previousStreamable)
    }
}

private fun MusicPlayerV2.playbackMutations(): Flow<Mutation<PlaybackScreenState>> =
    currentPlaybackStateStream.mapLatestToMutation {
        copy(
            playbackState = when (it) {
                is MusicPlayerV2.PlaybackState.Loading -> PlaybackViewModel.PlaybackState.Loading(
                    previousStreamable = it.previouslyPlayingStreamable
                )

                is MusicPlayerV2.PlaybackState.Idle -> PlaybackViewModel.PlaybackState.Idle
                is MusicPlayerV2.PlaybackState.Playing -> PlaybackViewModel.PlaybackState.Playing(
                    streamable = it.currentlyPlayingStreamable
                )

                is MusicPlayerV2.PlaybackState.Paused -> PlaybackViewModel.PlaybackState.Paused(
                    streamable = it.currentlyPlayingStreamable
                )

                is MusicPlayerV2.PlaybackState.Error -> PlaybackViewModel.PlaybackState.Error(
                    errorMessage = PLAYBACK_ERROR_MESSAGE
                )

                is MusicPlayerV2.PlaybackState.Ended -> PlaybackViewModel.PlaybackState.PlaybackEnded(
                    streamable = it.streamable
                )
            },
            currentTrackDuration = when (it) {
                is MusicPlayerV2.PlaybackState.Playing -> it.totalDuration
                else -> currentTrackDuration
            },
        )
    }

private fun MusicPlayerV2.playbackProgressMutations(): Flow<Mutation<PlaybackScreenState>> =
    currentPlaybackPositionInMillisFlow
        .filterNotNull()
        .mapLatestToMutation {
            copy(currentTrackElapsed = it)
        }


private fun Flow<PlaybackScreenAction.Toggle>.toggleMutations(
    musicPlayer: MusicPlayerV2,
    downloadDrawableFromUrlUseCase: DownloadDrawableFromUrlUseCase,
    context: Context
): Flow<Mutation<PlaybackScreenState>> = flatMapLatest {
    if (musicPlayer.tryResume()) emptyFlow()
    else map { PlaybackScreenAction.Play(it.streamable) }.playMutations(
        musicPlayer = musicPlayer,
        downloadDrawableFromUrlUseCase = downloadDrawableFromUrlUseCase,
        context = context
    )
}

private fun Flow<PlaybackScreenAction.Play>.playMutations(
    musicPlayer: MusicPlayerV2,
    downloadDrawableFromUrlUseCase: DownloadDrawableFromUrlUseCase,
    context: Context
): Flow<Mutation<PlaybackScreenState>> = mapLatestToManyMutations { (streamable) ->
    if (streamable.streamInfo.streamUrl == null) {
        val streamableType = when (streamable) {
            is PodcastEpisode -> "podcast episode"
            is SearchResult.TrackSearchResult -> "track"
        }
        return@mapLatestToManyMutations emit {
            copy(
                playbackState = PlaybackViewModel.PlaybackState.Error(
                    errorMessage = "This $streamableType is currently unavailable for playback."
                )
            )
        }
    }

    val downloadAlbumArtResult = downloadDrawableFromUrlUseCase.invoke(
        urlString = streamable.streamInfo.imageUrl,
        context = context
    )
    if (downloadAlbumArtResult.isSuccess) {
        // getOrNull() can't be null because this line is executed
        // if, and only if the image was downloaded successfully.
        val bitmap = downloadAlbumArtResult.getOrNull()!!.toBitmap()
        musicPlayer.playStreamable(
            streamable = streamable,
            associatedAlbumArt = bitmap
        )
    } else emit {
        copy(
            playbackState = PlaybackViewModel.PlaybackState.Error(PLAYBACK_ERROR_MESSAGE),
        )
    }
}

private fun Flow<PlaybackScreenAction.Pause>.pauseMutations(
    musicPlayer: MusicPlayerV2,
): Flow<Mutation<PlaybackScreenState>> = flatMapLatest {
    musicPlayer.pauseCurrentlyPlayingTrack()
    emptyFlow()
}


private fun convertTimestampMillisToString(millis: Long): String = with(TimeUnit.MILLISECONDS) {
    // don't display the hour information if the track's duration is
    // less than an hour
    if (toHours(millis) == 0L) "%02d:%02d".format(
        toMinutes(millis), toSeconds(millis)
    )
    else "%02d%02d:%02d".format(
        toHours(millis), toMinutes(millis), toSeconds(millis)
    )
}

private const val PLAYBACK_ERROR_MESSAGE = "An error occurred. Please check internet connection."