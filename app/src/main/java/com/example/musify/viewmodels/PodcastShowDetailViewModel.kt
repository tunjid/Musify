package com.example.musify.viewmodels

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.example.musify.data.repositories.podcastsrepository.PodcastQuery
import com.example.musify.data.repositories.podcastsrepository.PodcastsRepository
import com.example.musify.data.tiling.Page
import com.example.musify.data.tiling.toTiledList
import com.example.musify.data.utils.FetchedResource
import com.example.musify.domain.PodcastEpisode
import com.example.musify.domain.PodcastShow
import com.example.musify.ui.navigation.MusifyNavigationDestinations
import com.example.musify.usecases.getCurrentlyPlayingEpisodePlaybackStateUseCase.GetCurrentlyPlayingEpisodePlaybackStateUseCase
import com.tunjid.tiler.distinctBy
import com.tunjid.tiler.emptyTiledList
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.example.musify.usecases.getCurrentlyPlayingEpisodePlaybackStateUseCase.GetCurrentlyPlayingEpisodePlaybackStateUseCase.PlaybackState as UseCasePlaybackState


@HiltViewModel
class PodcastShowDetailViewModel @Inject constructor(
    application: Application,
    savedStateHandle: SavedStateHandle,
    getCurrentlyPlayingEpisodePlaybackStateUseCase: GetCurrentlyPlayingEpisodePlaybackStateUseCase,
    private val podcastsRepository: PodcastsRepository
) : AndroidViewModel(application) {

    enum class UiState { IDLE, LOADING, PLAYBACK_LOADING, ERROR }

    private val showId =
        savedStateHandle.get<String>(MusifyNavigationDestinations.PodcastShowDetailScreen.NAV_ARG_PODCAST_SHOW_ID)!!

    private val episodesQuery = MutableStateFlow(
        PodcastQuery(
            showId = showId,
            countryCode = getCountryCode(),
            page = Page(offset = 0)
        )
    )
    val episodesForShow = episodesQuery.toTiledList(
        startQuery = episodesQuery.value,
        queryFor = { copy(page = it) },
        fetcher = podcastsRepository::podcastsFor
    )
        .map { it.distinctBy(PodcastEpisode::id) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyTiledList()
        )

    val onQueryChanged: (PodcastQuery) -> Unit = episodesQuery::value::set

    var currentlyPlayingEpisode by mutableStateOf<PodcastEpisode?>(null)
        private set

    var uiState by mutableStateOf(UiState.IDLE)
        private set

    var podcastShow by mutableStateOf<PodcastShow?>(null)
        private set

    var isCurrentlyPlayingEpisodePaused by mutableStateOf<Boolean?>(null)
        private set

    init {
        fetchShowUpdatingUiState()
        getCurrentlyPlayingEpisodePlaybackStateUseCase
            .currentlyPlayingEpisodePlaybackStateStream
            .onEach {
                when (it) {
                    is UseCasePlaybackState.Ended -> {
                        isCurrentlyPlayingEpisodePaused = null
                        currentlyPlayingEpisode = null
                    }
                    is UseCasePlaybackState.Loading -> uiState = UiState.PLAYBACK_LOADING
                    is UseCasePlaybackState.Paused ->{
                        currentlyPlayingEpisode = it.pausedEpisode
                        isCurrentlyPlayingEpisodePaused = true
                    }
                    is UseCasePlaybackState.Playing -> {
                        if (uiState != UiState.IDLE) uiState = UiState.IDLE
                        if (isCurrentlyPlayingEpisodePaused == null || isCurrentlyPlayingEpisodePaused == true) {
                            isCurrentlyPlayingEpisodePaused = false
                        }
                        currentlyPlayingEpisode = it.playingEpisode
                    }
                }
            }.launchIn(viewModelScope)
    }

    fun retryFetchingShow() {
        fetchShowUpdatingUiState()
    }

    private fun fetchShowUpdatingUiState() {
        viewModelScope.launch {
            uiState = UiState.LOADING
            val result = podcastsRepository.fetchPodcastShow(
                showId = showId,
                countryCode = getCountryCode()
            )
            if (result is FetchedResource.Success) {
                uiState = UiState.IDLE
                podcastShow = result.data
            } else {
                uiState = UiState.ERROR
            }
        }
    }

}