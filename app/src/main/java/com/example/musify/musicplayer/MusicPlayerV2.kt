package com.example.musify.musicplayer

import android.graphics.Bitmap
import com.example.musify.domain.PodcastEpisode
import com.example.musify.domain.Streamable
import kotlinx.coroutines.flow.Flow

interface MusicPlaybackMonitor {
    val currentPlaybackStateStream: Flow<MusicPlayerV2.PlaybackState>

    sealed interface PlaybackState {
        data class Playing(val playingEpisode: PodcastEpisode) : PlaybackState
        data class Paused(val pausedEpisode: PodcastEpisode) : PlaybackState
        object Loading : PlaybackState
        object Ended : PlaybackState
    }
}

interface MusicPlayerV2 : MusicPlaybackMonitor {
    sealed class PlaybackState(open val currentlyPlayingStreamable: Streamable? = null) {
        data class Loading(val previouslyPlayingStreamable: Streamable?) : PlaybackState()
        data class Playing(
            override val currentlyPlayingStreamable: Streamable,
            val totalDuration: Long,
        ) : PlaybackState()

        data class Paused(override val currentlyPlayingStreamable: Streamable) : PlaybackState()
        data class Ended(val streamable: Streamable) : PlaybackState()
        object Error : PlaybackState()
        object Idle : PlaybackState()
    }

    val currentPlaybackPositionInMillisFlow: Flow<Long?>

    fun playStreamable(streamable: Streamable, associatedAlbumArt: Bitmap)
    fun pauseCurrentlyPlayingTrack()
    fun stopPlayingTrack()
    fun tryResume(): Boolean
}