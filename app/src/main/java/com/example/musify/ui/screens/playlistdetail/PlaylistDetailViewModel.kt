package com.example.musify.ui.screens.playlistdetail

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.musify.data.repositories.tracksrepository.TracksRepository
import com.example.musify.data.utils.NetworkMonitor
import com.example.musify.musicplayer.MusicPlaybackMonitor
import com.example.musify.ui.navigation.MusifyNavigationDestinations
import com.example.musify.utils.countryCode
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

@HiltViewModel
class PlaylistDetailViewModel @Inject constructor(
    @ApplicationContext context: Context,
    savedStateHandle: SavedStateHandle,
    tracksRepository: TracksRepository,
    networkMonitor: NetworkMonitor,
    musicPlaybackMonitor: MusicPlaybackMonitor,
) : ViewModel() {
    private val stateProducer =
        viewModelScope.playlistDetailStateProducer(
            playlistId = savedStateHandle[
                MusifyNavigationDestinations.PlaylistDetailScreen.NAV_ARG_PLAYLIST_ID
            ]!!,
            playlistName = savedStateHandle[
                MusifyNavigationDestinations.PlaylistDetailScreen.NAV_ARG_PLAYLIST_NAME
            ]!!,
            imageUrlString = savedStateHandle[
                MusifyNavigationDestinations.PlaylistDetailScreen.NAV_ARG_ENCODED_IMAGE_URL_STRING
            ]!!,
            ownerName = savedStateHandle[
                MusifyNavigationDestinations.PlaylistDetailScreen.NAV_ARG_OWNER_NAME]
            !!,
            totalNumberOfTracks = savedStateHandle[
                MusifyNavigationDestinations.PlaylistDetailScreen.NAV_ARG_NUMBER_OF_TRACKS
            ]!!,
            countryCode = context.countryCode,
            networkMonitor = networkMonitor,
            musicPlaybackMonitor = musicPlaybackMonitor,
            tracksRepository = tracksRepository,
        )

    val state = stateProducer.state
    val actions = stateProducer.accept
}

