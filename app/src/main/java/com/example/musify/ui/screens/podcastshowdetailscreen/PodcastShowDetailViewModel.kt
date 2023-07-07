package com.example.musify.ui.screens.podcastshowdetailscreen

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.example.musify.data.repositories.podcastsrepository.PodcastsRepository
import com.example.musify.ui.navigation.MusifyNavigationDestinations
import com.example.musify.usecases.getCurrentlyPlayingEpisodePlaybackStateUseCase.GetCurrentlyPlayingEpisodePlaybackStateUseCase
import com.example.musify.viewmodels.getCountryCode
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject


@HiltViewModel
class PodcastShowDetailViewModel @Inject constructor(
    application: Application,
    savedStateHandle: SavedStateHandle,
    podcastsRepository: PodcastsRepository,
    getCurrentlyPlayingEpisodePlaybackStateUseCase: GetCurrentlyPlayingEpisodePlaybackStateUseCase
) : AndroidViewModel(application) {
    private val stateProducer =
        viewModelScope.podcastShowDetailStateProducer(
            showId = savedStateHandle[
                MusifyNavigationDestinations.PodcastShowDetailScreen.NAV_ARG_PODCAST_SHOW_ID
            ]!!,
            countryCode = getCountryCode(),
            podcastsRepository = podcastsRepository,
            getCurrentlyPlayingEpisodePlaybackStateUseCase = getCurrentlyPlayingEpisodePlaybackStateUseCase
        )

    val state = stateProducer.state
    val actions = stateProducer.accept
}
