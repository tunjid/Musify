package com.example.musify.ui.screens.podcastshowdetailscreen

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.musify.data.repositories.podcastsrepository.PodcastsRepository
import com.example.musify.musicplayer.MusicPlaybackMonitor
import com.example.musify.ui.navigation.MusifyNavigationDestinations
import com.example.musify.utils.countryCode
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

@HiltViewModel
class PodcastShowDetailViewModel @Inject constructor(
    @ApplicationContext context: Context,
    savedStateHandle: SavedStateHandle,
    podcastsRepository: PodcastsRepository,
    musicPlaybackMonitor: MusicPlaybackMonitor
) : ViewModel() {
    private val stateProducer =
        viewModelScope.podcastShowDetailStateProducer(
            showId = savedStateHandle[
                MusifyNavigationDestinations.PodcastShowDetailScreen.NAV_ARG_PODCAST_SHOW_ID
            ]!!,
            countryCode = context.countryCode,
            podcastsRepository = podcastsRepository,
            musicPlaybackMonitor = musicPlaybackMonitor
        )

    val state = stateProducer.state
    val actions = stateProducer.accept
}
