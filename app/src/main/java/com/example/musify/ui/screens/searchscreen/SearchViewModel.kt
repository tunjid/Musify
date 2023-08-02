package com.example.musify.ui.screens.searchscreen

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.musify.data.repositories.genresrepository.GenresRepository
import com.example.musify.data.repositories.searchrepository.SearchRepository
import com.example.musify.data.utils.NetworkMonitor
import com.example.musify.musicplayer.MusicPlaybackMonitor
import com.example.musify.utils.countryCode
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

@HiltViewModel
class SearchViewModel @Inject constructor(
    @ApplicationContext context: Context,
    searchRepository: SearchRepository,
    networkMonitor: NetworkMonitor,
    musicPlaybackMonitor: MusicPlaybackMonitor,
    genresRepository: GenresRepository
) : ViewModel() {
    private val stateProducer =
        viewModelScope.searchStateProducer(
            countryCode = context.countryCode,
            networkMonitor = networkMonitor,
            genresRepository = genresRepository,
            searchRepository = searchRepository,
            musicPlaybackMonitor = musicPlaybackMonitor,
        )

    val state = stateProducer.state
    val actions = stateProducer.accept
}


