package com.example.musify.ui.screens.podcastepisodedetail

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
class PodcastEpisodeDetailViewModel @Inject constructor(
    application: Application,
    podcastsRepository: PodcastsRepository,
    savedStateHandle: SavedStateHandle,
    getCurrentlyPlayingEpisodePlaybackStateUseCase: GetCurrentlyPlayingEpisodePlaybackStateUseCase,
) : AndroidViewModel(application) {
    private val stateProducer =
        viewModelScope.podcastEpisodeDetailStateProducer(
            episodeId = savedStateHandle[
                MusifyNavigationDestinations.PodcastEpisodeDetailScreen.NAV_ARG_PODCAST_EPISODE_ID
            ]!!,
            countryCode = getCountryCode(),
            podcastsRepository = podcastsRepository,
            getCurrentlyPlayingEpisodePlaybackStateUseCase = getCurrentlyPlayingEpisodePlaybackStateUseCase
        )

    val state = stateProducer.state
    val actions = stateProducer.accept
}
