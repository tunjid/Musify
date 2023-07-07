package com.example.musify.ui.screens.artistdetail

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.example.musify.data.repositories.albumsrepository.AlbumsRepository
import com.example.musify.data.repositories.tracksrepository.TracksRepository
import com.example.musify.ui.navigation.MusifyNavigationDestinations
import com.example.musify.usecases.getCurrentlyPlayingTrackUseCase.GetCurrentlyPlayingTrackUseCase
import com.example.musify.usecases.getPlaybackLoadingStatusUseCase.GetPlaybackLoadingStatusUseCase
import com.example.musify.viewmodels.getCountryCode
import dagger.hilt.android.lifecycle.HiltViewModel
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import javax.inject.Inject


@HiltViewModel
class ArtistDetailViewModel @Inject constructor(
    application: Application,
    savedStateHandle: SavedStateHandle,
    albumsRepository: AlbumsRepository,
    getCurrentlyPlayingTrackUseCase: GetCurrentlyPlayingTrackUseCase,
    getPlaybackLoadingStatusUseCase: GetPlaybackLoadingStatusUseCase,
    tracksRepository: TracksRepository,
) : AndroidViewModel(application) {
    private val stateProducer =
        viewModelScope.aristDetailStateProducer(
            artistId = savedStateHandle[
                MusifyNavigationDestinations.ArtistDetailScreen.NAV_ARG_ARTIST_ID
            ]!!,
            artistName = savedStateHandle[
                MusifyNavigationDestinations.ArtistDetailScreen.NAV_ARG_ARTIST_NAME
            ]!!,
            artistImageUrlString = URLDecoder.decode(
                savedStateHandle[
                    MusifyNavigationDestinations.ArtistDetailScreen.NAV_ARG_ARTIST_NAME
                ]!!,
                StandardCharsets.UTF_8.toString()
            ),
            countryCode = getCountryCode(),
            albumsRepository = albumsRepository,
            getCurrentlyPlayingTrackUseCase = getCurrentlyPlayingTrackUseCase,
            getPlaybackLoadingStatusUseCase = getPlaybackLoadingStatusUseCase,
            tracksRepository = tracksRepository
        )

    val state = stateProducer.state
    val actions = stateProducer.accept
}
