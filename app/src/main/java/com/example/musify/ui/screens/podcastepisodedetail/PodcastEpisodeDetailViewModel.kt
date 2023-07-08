package com.example.musify.ui.screens.podcastepisodedetail

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.musify.data.repositories.podcastsrepository.PodcastsRepository
import com.example.musify.ui.navigation.MusifyNavigationDestinations
import com.example.musify.usecases.getCurrentlyPlayingEpisodePlaybackStateUseCase.GetCurrentlyPlayingEpisodePlaybackStateUseCase
import com.example.musify.utils.countryCode
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

@HiltViewModel
class PodcastEpisodeDetailViewModel @Inject constructor(
    @ApplicationContext context: Context,
    podcastsRepository: PodcastsRepository,
    savedStateHandle: SavedStateHandle,
    getCurrentlyPlayingEpisodePlaybackStateUseCase: GetCurrentlyPlayingEpisodePlaybackStateUseCase,
) : ViewModel() {
    private val stateProducer =
        viewModelScope.podcastEpisodeDetailStateProducer(
            episodeId = savedStateHandle[
                MusifyNavigationDestinations.PodcastEpisodeDetailScreen.NAV_ARG_PODCAST_EPISODE_ID
            ]!!,
            countryCode = context.countryCode,
            podcastsRepository = podcastsRepository,
            getCurrentlyPlayingEpisodePlaybackStateUseCase = getCurrentlyPlayingEpisodePlaybackStateUseCase
        )

    val state = stateProducer.state
    val actions = stateProducer.accept
}
