package com.example.musify.ui.activities

import androidx.core.graphics.drawable.toBitmap
import com.example.musify.domain.PodcastEpisode
import com.example.musify.domain.SearchResult
import com.example.musify.domain.Streamable
import com.example.musify.musicplayer.MusicPlayerV2
import com.example.musify.usecases.downloadDrawableFromUrlUseCase.DownloadDrawableFromUrlUseCase
import com.tunjid.mutator.Mutation
import com.tunjid.mutator.coroutines.actionStateFlowMutator
import com.tunjid.mutator.coroutines.mapLatestToManyMutations
import com.tunjid.mutator.coroutines.mapLatestToMutation
import com.tunjid.mutator.coroutines.toMutationStream
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import java.util.concurrent.TimeUnit

sealed class PlaybackScreenAction {
    data class Play(val streamable: Streamable) : PlaybackScreenAction()

    data class Toggle(val streamable: Streamable) : PlaybackScreenAction()

    object Pause : PlaybackScreenAction()

}

data class PlaybackUiState(
    val playbackState: PlaybackState = PlaybackState.Idle,
    val currentTrackDuration: Long? = 0L,
    val currentTrackElapsed: Long? = 0L,
) {
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

fun CoroutineScope.playbackStateProducer(
    musicPlayer: MusicPlayerV2,
    downloadDrawableFromUrlUseCase: DownloadDrawableFromUrlUseCase,
) = actionStateFlowMutator<PlaybackScreenAction, PlaybackUiState>(
    initialState = PlaybackUiState(),
    inputs = listOf(
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
                )

                is PlaybackScreenAction.Toggle -> action.flow.toggleMutations(
                    musicPlayer = musicPlayer,
                    downloadDrawableFromUrlUseCase = downloadDrawableFromUrlUseCase,
                )
            }
        }
    }
)

val PlaybackUiState.totalDurationOfCurrentTrackTimeText: String
    get() = currentTrackDuration?.let(::convertTimestampMillisToString) ?: "00:00"

val PlaybackUiState.currentTrackProgressText: String
    get() = currentTrackElapsed?.let(::convertTimestampMillisToString) ?: "00:00"

val PlaybackUiState.currentTrackProgress: Float
    get() = when {
        currentTrackDuration != null && currentTrackElapsed != null ->
            currentTrackElapsed.toFloat() / currentTrackDuration

        else -> 0f
    }


private fun MusicPlayerV2.playbackMutations(): Flow<Mutation<PlaybackUiState>> =
    currentPlaybackStateStream.mapLatestToMutation {
        copy(
            playbackState = when (it) {
                is MusicPlayerV2.PlaybackState.Loading -> PlaybackUiState.PlaybackState.Loading(
                    previousStreamable = it.previouslyPlayingStreamable
                )

                is MusicPlayerV2.PlaybackState.Idle -> PlaybackUiState.PlaybackState.Idle
                is MusicPlayerV2.PlaybackState.Playing -> PlaybackUiState.PlaybackState.Playing(
                    streamable = it.currentlyPlayingStreamable
                )

                is MusicPlayerV2.PlaybackState.Paused -> PlaybackUiState.PlaybackState.Paused(
                    streamable = it.currentlyPlayingStreamable
                )

                is MusicPlayerV2.PlaybackState.Error -> PlaybackUiState.PlaybackState.Error(
                    errorMessage = PLAYBACK_ERROR_MESSAGE
                )

                is MusicPlayerV2.PlaybackState.Ended -> PlaybackUiState.PlaybackState.PlaybackEnded(
                    streamable = it.streamable
                )
            },
            currentTrackDuration = when (it) {
                is MusicPlayerV2.PlaybackState.Playing -> it.totalDuration
                else -> currentTrackDuration
            },
        )
    }

private fun MusicPlayerV2.playbackProgressMutations(): Flow<Mutation<PlaybackUiState>> =
    currentPlaybackPositionInMillisFlow
        .filterNotNull()
        .mapLatestToMutation {
            copy(currentTrackElapsed = it)
        }


private fun Flow<PlaybackScreenAction.Toggle>.toggleMutations(
    musicPlayer: MusicPlayerV2,
    downloadDrawableFromUrlUseCase: DownloadDrawableFromUrlUseCase
): Flow<Mutation<PlaybackUiState>> = flatMapLatest {
    if (musicPlayer.tryResume()) emptyFlow()
    else map { PlaybackScreenAction.Play(it.streamable) }.playMutations(
        musicPlayer = musicPlayer,
        downloadDrawableFromUrlUseCase = downloadDrawableFromUrlUseCase
    )
}

private fun Flow<PlaybackScreenAction.Play>.playMutations(
    musicPlayer: MusicPlayerV2,
    downloadDrawableFromUrlUseCase: DownloadDrawableFromUrlUseCase
): Flow<Mutation<PlaybackUiState>> = mapLatestToManyMutations { (streamable) ->
    if (streamable.streamInfo.streamUrl == null) {
        val streamableType = when (streamable) {
            is PodcastEpisode -> "podcast episode"
            is SearchResult.TrackSearchResult -> "track"
        }
        return@mapLatestToManyMutations emit {
            copy(
                playbackState = PlaybackUiState.PlaybackState.Error(
                    errorMessage = "This $streamableType is currently unavailable for playback."
                )
            )
        }
    }

    val downloadAlbumArtResult = downloadDrawableFromUrlUseCase.invoke(
        urlString = streamable.streamInfo.imageUrl,
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
            playbackState = PlaybackUiState.PlaybackState.Error(PLAYBACK_ERROR_MESSAGE),
        )
    }
}

private fun Flow<PlaybackScreenAction.Pause>.pauseMutations(
    musicPlayer: MusicPlayerV2,
): Flow<Mutation<PlaybackUiState>> = flatMapLatest {
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
