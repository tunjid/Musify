package com.example.musify.viewmodels.searchviewmodel

import android.app.Application
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.musify.data.remote.musicservice.SearchQueryType
import com.example.musify.data.repositories.genresrepository.GenresRepository
import com.example.musify.data.repositories.searchrepository.ContentQuery
import com.example.musify.data.repositories.searchrepository.SearchRepository
import com.example.musify.data.repositories.searchrepository.itemsFor
import com.example.musify.data.tiling.Page
import com.example.musify.data.tiling.toTiledList
import com.example.musify.data.utils.NetworkMonitor
import com.example.musify.domain.SearchResult
import com.example.musify.domain.SearchResults
import com.example.musify.usecases.getCurrentlyPlayingTrackUseCase.GetCurrentlyPlayingTrackUseCase
import com.example.musify.viewmodels.getCountryCode
import com.tunjid.tiler.TiledList
import com.tunjid.tiler.distinctBy
import com.tunjid.tiler.emptyTiledList
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class SearchViewModel @Inject constructor(
    application: Application,
    getCurrentlyPlayingTrackUseCase: GetCurrentlyPlayingTrackUseCase,
    searchRepository: SearchRepository,
    networkMonitor: NetworkMonitor,
    private val genresRepository: GenresRepository
) : AndroidViewModel(application) {

    val isOnline = networkMonitor.isOnline
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = true,
        )

    private val albumsQuery = SearchQueryType.ALBUM.contentFlow(
        countryCode = getCountryCode(),
    )
    val albumsTiledList = albumsQuery.toTiledList(
        scope = viewModelScope,
        searchRepository = searchRepository,
        idFunction = SearchResult.AlbumSearchResult::id
    )

    private val artistsQuery = SearchQueryType.ARTIST.contentFlow(
        countryCode = getCountryCode(),
    )
    val artistsTiledList = artistsQuery.toTiledList(
        scope = viewModelScope,
        searchRepository = searchRepository,
        idFunction = SearchResult.ArtistSearchResult::id
    )

    private val episodesQuery = SearchQueryType.EPISODE.contentFlow(
        countryCode = getCountryCode(),
    )
    val episodesTiledList = episodesQuery.toTiledList(
        scope = viewModelScope,
        searchRepository = searchRepository,
        idFunction = SearchResult.EpisodeSearchResult::id
    )

    private val playlistsQuery = SearchQueryType.PLAYLIST.contentFlow(
        countryCode = getCountryCode(),
    )
    val playlistsTiledList = playlistsQuery.toTiledList(
        scope = viewModelScope,
        searchRepository = searchRepository,
        idFunction = SearchResult.PlaylistSearchResult::id
    )

    private val showsQuery = SearchQueryType.SHOW.contentFlow(
        countryCode = getCountryCode(),
    )
    val showsTiledList = showsQuery.toTiledList(
        scope = viewModelScope,
        searchRepository = searchRepository,
        idFunction = SearchResult.PodcastSearchResult::id
    )

    private val tracksQuery = SearchQueryType.TRACK.contentFlow(
        countryCode = getCountryCode(),
    )
    val trackTiledList = tracksQuery.toTiledList(
        scope = viewModelScope,
        searchRepository = searchRepository,
        idFunction = SearchResult.TrackSearchResult::id
    )

    val onContentQueryChanged = { query: ContentQuery ->
        when (query.type) {
            SearchQueryType.ALBUM -> albumsQuery.value = query
            SearchQueryType.ARTIST -> artistsQuery.value = query
            SearchQueryType.PLAYLIST -> playlistsQuery.value = query
            SearchQueryType.TRACK -> tracksQuery.value = query
            SearchQueryType.SHOW -> showsQuery.value = query
            SearchQueryType.EPISODE -> episodesQuery.value = query
        }
    }

    private val _currentlySelectedFilter = mutableStateOf(SearchFilter.TRACKS)
    val currentlySelectedFilter = _currentlySelectedFilter as State<SearchFilter>

    val currentlyPlayingTrackStream = getCurrentlyPlayingTrackUseCase.currentlyPlayingTrackStream

    fun search(searchQuery: String) {
        albumsQuery.value = SearchQueryType.ALBUM.contentQueryFor(
            searchQuery = searchQuery,
            countryCode = getCountryCode()
        )
        artistsQuery.value = SearchQueryType.ARTIST.contentQueryFor(
            searchQuery = searchQuery,
            countryCode = getCountryCode()
        )
        episodesQuery.value = SearchQueryType.EPISODE.contentQueryFor(
            searchQuery = searchQuery,
            countryCode = getCountryCode()
        )
        playlistsQuery.value = SearchQueryType.PLAYLIST.contentQueryFor(
            searchQuery = searchQuery,
            countryCode = getCountryCode()
        )
        showsQuery.value = SearchQueryType.SHOW.contentQueryFor(
            searchQuery = searchQuery,
            countryCode = getCountryCode()
        )
        tracksQuery.value = SearchQueryType.TRACK.contentQueryFor(
            searchQuery = searchQuery,
            countryCode = getCountryCode()
        )
    }

    fun getAvailableGenres() = genresRepository.fetchAvailableGenres()

    fun updateSearchFilter(newSearchFilter: SearchFilter) {
        _currentlySelectedFilter.value = newSearchFilter
    }

    private fun SearchQueryType.contentQueryFor(
        searchQuery: String,
        countryCode: String,
    ) = ContentQuery(
        type = this,
        searchQuery = searchQuery,
        page = Page(offset = 0),
        countryCode = countryCode
    )

    private fun SearchQueryType.contentFlow(
        countryCode: String
    ) = MutableStateFlow(
        contentQueryFor(
            searchQuery = "",
            countryCode = countryCode
        )
    )

    private inline fun <reified T : SearchResult> MutableStateFlow<ContentQuery>.toTiledList(
        scope: CoroutineScope,
        searchRepository: SearchRepository,
        crossinline idFunction: (T) -> Any
    ): StateFlow<TiledList<ContentQuery, T>> =
        debounce {
            if (it.searchQuery.length < 2) 0
            else 500
        }.toTiledList(
            startQuery = value,
            queryFor = { copy(page = it) },
            fetcher = { query ->
                if (query.searchQuery.isEmpty()) emptyFlow()
                else searchRepository.searchFor(query)
                    .map<SearchResults, List<T>>(SearchResults::itemsFor)
            }
        )
            .map { it.distinctBy(idFunction) }
            .stateIn(
                scope = scope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = emptyTiledList()
            )
}