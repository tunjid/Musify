package com.example.musify.musicplayer

import com.example.musify.domain.PodcastEpisode
import com.example.musify.domain.SearchResult
import com.example.musify.domain.Streamable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull

val MusicPlaybackMonitor.currentlyPlayingStreamableStream: Flow<Streamable>
    get() = currentPlaybackStateStream
        .filterIsInstance<MusicPlayerV2.PlaybackState.Playing>()
        .map { it.currentlyPlayingStreamable }

val MusicPlaybackMonitor.currentlyPlayingTrackStream: Flow<SearchResult.TrackSearchResult>
    get() = currentlyPlayingStreamableStream
        .filterIsInstance()

val MusicPlaybackMonitor.currentlyPlayingEpisodePlaybackStateStream
    get() = currentPlaybackStateStream
        .mapNotNull {
            // check if the currently playing streamable is an instance of PodcastEpisode if, and
            // only if, the currently playing streamable is not null. If it is null, it might
            // mean that the playback state is ended,idle or loading.
            if (it.currentlyPlayingStreamable != null && it.currentlyPlayingStreamable !is PodcastEpisode) {
                return@mapNotNull null
            }
            when (it) {
                is MusicPlayerV2.PlaybackState.Ended,
                is MusicPlayerV2.PlaybackState.Error -> MusicPlaybackMonitor.PlaybackState.Ended

                is MusicPlayerV2.PlaybackState.Loading -> MusicPlaybackMonitor.PlaybackState.Loading
                is MusicPlayerV2.PlaybackState.Paused -> {
                    MusicPlaybackMonitor.PlaybackState.Paused(it.currentlyPlayingStreamable as PodcastEpisode)
                }

                is MusicPlayerV2.PlaybackState.Playing -> {
                    MusicPlaybackMonitor.PlaybackState.Playing(it.currentlyPlayingStreamable as PodcastEpisode)
                }

                is MusicPlayerV2.PlaybackState.Idle -> null
            }
        }

val MusicPlaybackMonitor.loadingStatusStream: Flow<Boolean>
    get() = currentPlaybackStateStream
        .map { it is MusicPlayerV2.PlaybackState.Loading }
        .distinctUntilChanged()