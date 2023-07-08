package com.example.musify.ui.screens.searchscreen

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.musify.data.repositories.genresrepository.GenresRepository
import com.example.musify.data.repositories.searchrepository.SearchRepository
import com.example.musify.data.utils.NetworkMonitor
import com.example.musify.usecases.getCurrentlyPlayingTrackUseCase.GetCurrentlyPlayingTrackUseCase
import com.example.musify.viewmodels.getCountryCode
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class SearchViewModel @Inject constructor(
    application: Application,
    getCurrentlyPlayingTrackUseCase: GetCurrentlyPlayingTrackUseCase,
    searchRepository: SearchRepository,
    networkMonitor: NetworkMonitor,
    genresRepository: GenresRepository
) : AndroidViewModel(application) {
    private val stateProducer =
        viewModelScope.searchStateProducer(
            countryCode = getCountryCode(),
            networkMonitor = networkMonitor,
            genresRepository = genresRepository,
            searchRepository = searchRepository,
            getCurrentlyPlayingTrackUseCase = getCurrentlyPlayingTrackUseCase,
        )

    val state = stateProducer.state
    val actions = stateProducer.accept
}


