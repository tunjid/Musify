package com.example.musify.viewmodels.artistviewmodel

import android.app.Application
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.example.musify.data.repositories.albumsrepository.ArtistAlbumsQuery
import com.example.musify.data.repositories.albumsrepository.AlbumsRepository
import com.example.musify.data.repositories.tracksrepository.TracksRepository
import com.example.musify.data.tiling.Page
import com.example.musify.data.tiling.toTiledList
import com.example.musify.data.utils.FetchedResource
import com.example.musify.domain.SearchResult
import com.example.musify.ui.navigation.MusifyNavigationDestinations
import com.example.musify.usecases.getCurrentlyPlayingTrackUseCase.GetCurrentlyPlayingTrackUseCase
import com.example.musify.usecases.getPlaybackLoadingStatusUseCase.GetPlaybackLoadingStatusUseCase
import com.example.musify.viewmodels.getCountryCode
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

/**
 * A sealed class hierarchy consisting of all UI states that are related to a screen
 * displaying the details of an artist.
 */
sealed class ArtistDetailScreenUiState {
    object Idle : ArtistDetailScreenUiState()
    object Loading : ArtistDetailScreenUiState()
    data class Error(private val message: String) : ArtistDetailScreenUiState()
}

@HiltViewModel
class ArtistDetailViewModel @Inject constructor(
    application: Application,
    savedStateHandle: SavedStateHandle,
    albumsRepository: AlbumsRepository,
    getCurrentlyPlayingTrackUseCase: GetCurrentlyPlayingTrackUseCase,
    getPlaybackLoadingStatusUseCase: GetPlaybackLoadingStatusUseCase,
    private val tracksRepository: TracksRepository,
) : AndroidViewModel(application) {

    private val _popularTracks = mutableStateOf<List<SearchResult.TrackSearchResult>>(emptyList())
    val popularTracks = _popularTracks as State<List<SearchResult.TrackSearchResult>>

    private val _uiState = mutableStateOf<ArtistDetailScreenUiState>(ArtistDetailScreenUiState.Idle)
    val uiState = _uiState as State<ArtistDetailScreenUiState>

    private val artistId =
        savedStateHandle.get<String>(MusifyNavigationDestinations.ArtistDetailScreen.NAV_ARG_ARTIST_ID)!!

    val currentlyPlayingTrackStream = getCurrentlyPlayingTrackUseCase.currentlyPlayingTrackStream

    private val albumsQuery = MutableStateFlow(
        ArtistAlbumsQuery(
            artistId = artistId,
            countryCode = getCountryCode(),
            page = Page(offset = 0)
        )
    )
    val artistAlbums = albumsQuery.toTiledList(
        startQuery = albumsQuery.value,
        queryFor = { copy(page = it) },
        fetcher = albumsRepository::albumsFor
    )
        .map { it.distinctBy(SearchResult.AlbumSearchResult::id) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyTiledList()
        )

    val onQueryChanged: (ArtistAlbumsQuery) -> Unit = albumsQuery::value::set

    init {
        viewModelScope.launch { fetchAndAssignPopularTracks() }
        getPlaybackLoadingStatusUseCase.loadingStatusStream.onEach { isPlaybackLoading ->
            if (isPlaybackLoading && _uiState.value !is ArtistDetailScreenUiState.Loading) {
                _uiState.value = ArtistDetailScreenUiState.Loading
                return@onEach
            }
            if (!isPlaybackLoading && _uiState.value is ArtistDetailScreenUiState.Loading) {
                _uiState.value = ArtistDetailScreenUiState.Idle
                return@onEach
            }
        }.launchIn(viewModelScope)
    }

    private suspend fun fetchAndAssignPopularTracks() {
        _uiState.value = ArtistDetailScreenUiState.Loading
        val fetchResult = tracksRepository.fetchTopTenTracksForArtistWithId(
            artistId = artistId,
            countryCode = getCountryCode()
        )
        when (fetchResult) {
            is FetchedResource.Failure -> {
                _uiState.value =
                    ArtistDetailScreenUiState.Error("Error loading tracks, please check internet connection")
            }
            is FetchedResource.Success -> {
                _popularTracks.value = fetchResult.data
                _uiState.value = ArtistDetailScreenUiState.Idle
            }
        }
    }
}