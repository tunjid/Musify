package com.example.musify.ui.screens.playlistdetail

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.example.musify.data.repositories.tracksrepository.TracksRepository
import com.example.musify.ui.navigation.MusifyNavigationDestinations
import com.example.musify.usecases.getCurrentlyPlayingTrackUseCase.GetCurrentlyPlayingTrackUseCase
import com.example.musify.viewmodels.getCountryCode
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class PlaylistDetailViewModel @Inject constructor(
    application: Application,
    savedStateHandle: SavedStateHandle,
    tracksRepository: TracksRepository,
    getCurrentlyPlayingTrackUseCase: GetCurrentlyPlayingTrackUseCase,
) : AndroidViewModel(application) {
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
            countryCode = getCountryCode(),
            tracksRepository = tracksRepository,
            getCurrentlyPlayingTrackUseCase = getCurrentlyPlayingTrackUseCase,
        )

    val state = stateProducer.state
    val actions = stateProducer.accept
}

