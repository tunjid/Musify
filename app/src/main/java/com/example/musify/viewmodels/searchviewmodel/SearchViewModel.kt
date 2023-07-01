package com.example.musify.viewmodels.searchviewmodel

import android.app.Application
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.example.musify.data.remote.musicservice.SearchQueryType
import com.example.musify.data.repositories.genresrepository.GenresRepository
import com.example.musify.data.repositories.searchrepository.ContentQuery
import com.example.musify.data.repositories.searchrepository.SearchRepository
import com.example.musify.data.repositories.searchrepository.itemsFor
import com.example.musify.data.tiling.Page
import com.example.musify.di.IODispatcher
import com.example.musify.domain.SearchResult
import com.example.musify.domain.SearchResults
import com.example.musify.usecases.getCurrentlyPlayingTrackUseCase.GetCurrentlyPlayingTrackUseCase
import com.example.musify.usecases.getPlaybackLoadingStatusUseCase.GetPlaybackLoadingStatusUseCase
import com.example.musify.viewmodels.getCountryCode
import com.tunjid.tiler.PivotRequest
import com.tunjid.tiler.Tile
import com.tunjid.tiler.TiledList
import com.tunjid.tiler.emptyTiledList
import com.tunjid.tiler.listTiler
import com.tunjid.tiler.toPivotedTileInputs
import com.tunjid.tiler.toTiledList
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.retry
import kotlinx.coroutines.flow.stateIn
import java.io.IOException
import javax.inject.Inject

/**
 * An enum class that contains the different ui states associated with
 * the [SearchViewModel].
 */
enum class SearchScreenUiState { LOADING, IDLE }

@HiltViewModel
class SearchViewModel @Inject constructor(
    application: Application,
    getCurrentlyPlayingTrackUseCase: GetCurrentlyPlayingTrackUseCase,
    getPlaybackLoadingStatusUseCase: GetPlaybackLoadingStatusUseCase,
    @IODispatcher private val ioDispatcher: CoroutineDispatcher,
    private val genresRepository: GenresRepository,
    private val searchRepository: SearchRepository
) : AndroidViewModel(application) {

    private val _uiState = mutableStateOf(SearchScreenUiState.IDLE)
    val uiState = _uiState as State<SearchScreenUiState>

    private val albumsQuery = SearchQueryType.ALBUM.contentFlow(
        countryCode = getCountryCode(),
    )
    val albumsTiledList = albumsQuery.toTiledList<SearchResult.AlbumSearchResult>(
        searchRepository = searchRepository,
        scope = viewModelScope,
    )

    private val artistsQuery = SearchQueryType.ARTIST.contentFlow(
        countryCode = getCountryCode(),
    )
    val artistsTiledList = artistsQuery.toTiledList<SearchResult.ArtistSearchResult>(
        searchRepository = searchRepository,
        scope = viewModelScope,
    )

    private val episodesQuery = SearchQueryType.EPISODE.contentFlow(
        countryCode = getCountryCode(),
    )
    val episodesTiledList = episodesQuery.toTiledList<SearchResult.EpisodeSearchResult>(
        searchRepository = searchRepository,
        scope = viewModelScope,
    )

    private val playlistsQuery = SearchQueryType.PLAYLIST.contentFlow(
        countryCode = getCountryCode(),
    )
    val playlistsTiledList = playlistsQuery.toTiledList<SearchResult.PlaylistSearchResult>(
        searchRepository = searchRepository,
        scope = viewModelScope,
    )

    private val showsQuery = SearchQueryType.SHOW.contentFlow(
        countryCode = getCountryCode(),
    )
    val showsTiledList = showsQuery.toTiledList<SearchResult.PodcastSearchResult>(
        searchRepository = searchRepository,
        scope = viewModelScope,
    )

    private val tracksQuery = SearchQueryType.TRACK.contentFlow(
        countryCode = getCountryCode(),
    )
    val tracksTiledList = tracksQuery.toTiledList<SearchResult.TrackSearchResult>(
        searchRepository = searchRepository,
        scope = viewModelScope,
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

    init {
        getPlaybackLoadingStatusUseCase
            .loadingStatusStream
            .onEach { isPlaybackLoading ->
                if (isPlaybackLoading && _uiState.value != SearchScreenUiState.LOADING) {
                    _uiState.value = SearchScreenUiState.LOADING
                    return@onEach
                }
                if (!isPlaybackLoading && _uiState.value == SearchScreenUiState.LOADING) {
                    _uiState.value = SearchScreenUiState.IDLE
                    return@onEach
                }
            }
            .launchIn(viewModelScope)
    }

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

    private inline fun SearchQueryType.contentFlow(
        countryCode: String
    ) = MutableStateFlow(
        contentQueryFor(
            searchQuery = "",
            countryCode = countryCode
        )
    )

    private inline fun <reified T : SearchResult> MutableStateFlow<ContentQuery>.toTiledList(
        searchRepository: SearchRepository,
        scope: CoroutineScope,
    ): StateFlow<TiledList<ContentQuery, T>> = debounce {
        if (it.searchQuery.length < 2) 0
        else 500
    }
        .toPivotedTileInputs(searchPivot())
        .toTiledList(
            listTiler(
                order = Tile.Order.PivotSorted(
                    query = value,
                    comparator = compareBy { it.page.offset }
                ),
                limiter = Tile.Limiter(
                    maxQueries = 5
                ),
                fetcher = { query ->
                    if (query.searchQuery.isEmpty()) emptyFlow()
                    else searchRepository.searchFor(query).map<SearchResults, List<T>>(SearchResults::itemsFor)
                        .retry(retries = 10) { e ->
                            e.printStackTrace()
                            // retry on any IOException but also introduce delay if retrying
                            (e is IOException).also { if (it) delay(1000) }
                        }

                }
            )
        )
        .onEach {
            if (value.type == SearchQueryType.ALBUM) println("OUT ${it.size} items")
        }
        .stateIn(
            scope = scope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyTiledList()
        ) as StateFlow<TiledList<ContentQuery, T>>

    private fun searchPivot() = PivotRequest<ContentQuery, SearchResult>(
        onCount = 3,
        offCount = 4,
        comparator = compareBy { it.page.offset },
        nextQuery = {
            copy(
                page = Page(offset = page.offset + page.limit)
            )
        },
        previousQuery = {
            if (page.offset == 0) null
            else copy(
                page = Page(offset = page.offset - page.limit)
            )
        },
    )
}