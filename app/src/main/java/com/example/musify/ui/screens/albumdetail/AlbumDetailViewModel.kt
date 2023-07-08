package com.example.musify.ui.screens.albumdetail

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.example.musify.data.repositories.tracksrepository.TracksRepository
import com.example.musify.ui.navigation.MusifyNavigationDestinations
import com.example.musify.usecases.getCurrentlyPlayingTrackUseCase.GetCurrentlyPlayingTrackUseCase
import com.example.musify.usecases.getPlaybackLoadingStatusUseCase.GetPlaybackLoadingStatusUseCase
import com.example.musify.viewmodels.getCountryCode
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class AlbumDetailViewModel @Inject constructor(
    application: Application,
    savedStateHandle: SavedStateHandle,
    getCurrentlyPlayingTrackUseCase: GetCurrentlyPlayingTrackUseCase,
    getPlaybackLoadingStatusUseCase: GetPlaybackLoadingStatusUseCase,
    tracksRepository: TracksRepository,
) : AndroidViewModel(application) {
    private val stateProducer =
        viewModelScope.albumDetailStateProducer(
            albumId = savedStateHandle[
                MusifyNavigationDestinations.AlbumDetailScreen.NAV_ARG_ALBUM_ID
            ]!!,
            albumArtUrl = savedStateHandle[
                MusifyNavigationDestinations.AlbumDetailScreen.NAV_ARG_ENCODED_IMAGE_URL_STRING
            ]!!,
            albumName = savedStateHandle[
                MusifyNavigationDestinations.AlbumDetailScreen.NAV_ARG_ALBUM_NAME
            ]!!,
            artists = savedStateHandle[
                MusifyNavigationDestinations.AlbumDetailScreen.NAV_ARG_ARTISTS_STRING
            ]!!,
            yearOfRelease = savedStateHandle[
                MusifyNavigationDestinations.AlbumDetailScreen.NAV_ARG_YEAR_OF_RELEASE_STRING
            ]!!,
            countryCode = getCountryCode(),
            tracksRepository = tracksRepository,
            getCurrentlyPlayingTrackUseCase = getCurrentlyPlayingTrackUseCase,
            getPlaybackLoadingStatusUseCase = getPlaybackLoadingStatusUseCase,
        )

    val state = stateProducer.state
}
