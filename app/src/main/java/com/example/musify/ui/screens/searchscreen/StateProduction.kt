package com.example.musify.ui.screens.searchscreen

import com.example.musify.data.remote.musicservice.SearchQueryType
import com.example.musify.data.repositories.genresrepository.GenresRepository
import com.example.musify.data.repositories.searchrepository.ContentQuery
import com.example.musify.data.repositories.searchrepository.SearchRepository
import com.example.musify.data.repositories.searchrepository.itemsFor
import com.example.musify.data.tiling.Page
import com.example.musify.data.tiling.toNetworkBackedTiledList
import com.example.musify.data.utils.NetworkMonitor
import com.example.musify.domain.Genre
import com.example.musify.domain.SearchResult
import com.example.musify.domain.SearchResults
import com.example.musify.usecases.getCurrentlyPlayingTrackUseCase.GetCurrentlyPlayingTrackUseCase
import com.tunjid.mutator.Mutation
import com.tunjid.mutator.coroutines.actionStateFlowProducer
import com.tunjid.mutator.coroutines.mapToMutation
import com.tunjid.mutator.coroutines.toMutationStream
import com.tunjid.tiler.TiledList
import com.tunjid.tiler.distinctBy
import com.tunjid.tiler.emptyTiledList
import com.tunjid.tiler.map
import com.tunjid.tiler.tiledListOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn


sealed class SearchAction(val key: String) {

    sealed class Searches : SearchAction(key = "Searches") {
        data class LoadAround(val contentQuery: ContentQuery?) : Searches()

        data class Search(val searchQuery: String) : Searches()
    }

    data class SearchFilterChange(
        val searchFilter: SearchFilter
    ) : SearchAction(key = "FilterChange")
}

data class SearchUiState(
    val isOnline: Boolean = true,
    val genres: List<Genre> = emptyList(),
    val selectedSearchFilter: SearchFilter = SearchFilter.TRACKS,
    val currentlyPlayingTrack: SearchResult.TrackSearchResult? = null,
    val tiledItems: SearchTiledListFlows = SearchTiledListFlows(
        albumTiledListFlow = MutableStateFlow(emptyTiledList()),
        artistTiledListFLow = MutableStateFlow(emptyTiledList()),
        trackTiledListFlow = MutableStateFlow(emptyTiledList()),
        playlistTiledListFlow = MutableStateFlow(emptyTiledList()),
        podcastTiledListFlow = MutableStateFlow(emptyTiledList()),
        episodeTiledListFlow = MutableStateFlow(emptyTiledList()),
    ),
)

sealed class SearchItem<out T : SearchResult> {
    object Empty : SearchItem<Nothing>()

    data class Loaded<T : SearchResult>(val result: T) : SearchItem<T>()
}

val SearchItem<*>.id
    get() = when (this) {
        SearchItem.Empty -> "Empty"
        is SearchItem.Loaded -> result.id
    }

val <T : SearchResult> List<SearchItem<T>>.loadedItem: T?
    get() = when (val first = firstOrNull()) {
        null, SearchItem.Empty -> null
        is SearchItem.Loaded -> first.result
    }

data class SearchTiledListFlows(
    val albumTiledListFlow: StateFlow<TiledList<ContentQuery, SearchItem<SearchResult.AlbumSearchResult>>>,
    val artistTiledListFLow: StateFlow<TiledList<ContentQuery, SearchItem<SearchResult.ArtistSearchResult>>>,
    val trackTiledListFlow: StateFlow<TiledList<ContentQuery, SearchItem<SearchResult.TrackSearchResult>>>,
    val playlistTiledListFlow: StateFlow<TiledList<ContentQuery, SearchItem<SearchResult.PlaylistSearchResult>>>,
    val podcastTiledListFlow: StateFlow<TiledList<ContentQuery, SearchItem<SearchResult.PodcastSearchResult>>>,
    val episodeTiledListFlow: StateFlow<TiledList<ContentQuery, SearchItem<SearchResult.EpisodeSearchResult>>>,
)

fun CoroutineScope.searchStateProducer(
    countryCode: String,
    networkMonitor: NetworkMonitor,
    genresRepository: GenresRepository,
    searchRepository: SearchRepository,
    getCurrentlyPlayingTrackUseCase: GetCurrentlyPlayingTrackUseCase,
) = actionStateFlowProducer<SearchAction, SearchUiState>(
    initialState = SearchUiState(
        genres = genresRepository.fetchAvailableGenres()
    ),
    mutationFlows = listOf(
        networkMonitor.isOnlineMutations(),
        getCurrentlyPlayingTrackUseCase.playingTrackMutations(),
    ),
    actionTransform = { actions ->
        actions.toMutationStream(keySelector = SearchAction::key) {
            when (val action = type()) {
                is SearchAction.Searches -> action.flow.searchMutations(
                    countryCode = countryCode,
                    scope = this@searchStateProducer,
                    searchRepository = searchRepository,
                )

                is SearchAction.SearchFilterChange -> action.flow.searchFilterMutations()
            }
        }
    }
)

private fun NetworkMonitor.isOnlineMutations(): Flow<Mutation<SearchUiState>> =
    isOnline.mapToMutation {
        copy(isOnline = it)
    }

private fun GetCurrentlyPlayingTrackUseCase.playingTrackMutations(): Flow<Mutation<SearchUiState>> =
    currentlyPlayingTrackStream.mapToMutation {
        copy(currentlyPlayingTrack = it)
    }

private fun Flow<SearchAction.SearchFilterChange>.searchFilterMutations(): Flow<Mutation<SearchUiState>> =
    mapToMutation {
        copy(selectedSearchFilter = it.searchFilter)
    }

private fun Flow<SearchAction.Searches>.searchMutations(
    countryCode: String,
    scope: CoroutineScope,
    searchRepository: SearchRepository
): Flow<Mutation<SearchUiState>> = flow {
    val albumsQuery = SearchQueryType.ALBUM.contentFlow(
        countryCode = countryCode,
    )
    val albumsTiledList = albumsQuery.toTiledList<SearchResult.AlbumSearchResult>(
        scope = scope,
        searchRepository = searchRepository
    )

    val artistsQuery = SearchQueryType.ARTIST.contentFlow(
        countryCode = countryCode,
    )
    val artistsTiledList = artistsQuery.toTiledList<SearchResult.ArtistSearchResult>(
        scope = scope,
        searchRepository = searchRepository
    )

    val episodesQuery = SearchQueryType.EPISODE.contentFlow(
        countryCode = countryCode,
    )
    val episodesTiledList = episodesQuery.toTiledList<SearchResult.EpisodeSearchResult>(
        scope = scope,
        searchRepository = searchRepository
    )

    val playlistsQuery = SearchQueryType.PLAYLIST.contentFlow(
        countryCode = countryCode,
    )
    val playlistsTiledList = playlistsQuery.toTiledList<SearchResult.PlaylistSearchResult>(
        scope = scope,
        searchRepository = searchRepository
    )

    val showsQuery = SearchQueryType.SHOW.contentFlow(
        countryCode = countryCode,
    )
    val showsTiledList = showsQuery.toTiledList<SearchResult.PodcastSearchResult>(
        scope = scope,
        searchRepository = searchRepository
    )

    val tracksQuery = SearchQueryType.TRACK.contentFlow(
        countryCode = countryCode,
    )
    val trackTiledList = tracksQuery.toTiledList<SearchResult.TrackSearchResult>(
        scope = scope,
        searchRepository = searchRepository
    )

    // Emit the tiled item state flows first
    emit {
        copy(
            tiledItems = SearchTiledListFlows(
                albumTiledListFlow = albumsTiledList,
                artistTiledListFLow = artistsTiledList,
                trackTiledListFlow = trackTiledList,
                playlistTiledListFlow = playlistsTiledList,
                podcastTiledListFlow = showsTiledList,
                episodeTiledListFlow = episodesTiledList
            )
        )
    }

    // Collect from the backing flow and update searches as appropriate
    collect { action ->
        when (action) {
            is SearchAction.Searches.LoadAround -> when (action.contentQuery?.type) {
                SearchQueryType.ALBUM -> albumsQuery.value = action.contentQuery
                SearchQueryType.ARTIST -> artistsQuery.value = action.contentQuery
                SearchQueryType.PLAYLIST -> playlistsQuery.value = action.contentQuery
                SearchQueryType.TRACK -> tracksQuery.value = action.contentQuery
                SearchQueryType.SHOW -> showsQuery.value = action.contentQuery
                SearchQueryType.EPISODE -> episodesQuery.value = action.contentQuery
                null -> Unit
            }

            // Only pages in the pager will have their search queries executed, but the queries
            // for offscreen pages will be saved to begin execution when they're visible
            is SearchAction.Searches.Search -> {
                albumsQuery.value = SearchQueryType.ALBUM.contentQueryFor(
                    searchQuery = action.searchQuery,
                    countryCode = countryCode,
                )
                artistsQuery.value = SearchQueryType.ARTIST.contentQueryFor(
                    searchQuery = action.searchQuery,
                    countryCode = countryCode,
                )
                episodesQuery.value = SearchQueryType.EPISODE.contentQueryFor(
                    searchQuery = action.searchQuery,
                    countryCode = countryCode,
                )
                playlistsQuery.value = SearchQueryType.PLAYLIST.contentQueryFor(
                    searchQuery = action.searchQuery,
                    countryCode = countryCode,
                )
                showsQuery.value = SearchQueryType.SHOW.contentQueryFor(
                    searchQuery = action.searchQuery,
                    countryCode = countryCode,
                )
                tracksQuery.value = SearchQueryType.TRACK.contentQueryFor(
                    searchQuery = action.searchQuery,
                    countryCode = countryCode,
                )
            }
        }
    }
}

private inline fun <reified T : SearchResult> MutableStateFlow<ContentQuery>.toTiledList(
    scope: CoroutineScope,
    searchRepository: SearchRepository
): StateFlow<TiledList<ContentQuery, SearchItem<T>>> {
    val startPage = value.page
    return debounce {
        // Don't debounce the If its the first character or more is being loaded
        if (it.searchQuery.length < 2 || it.page.offset != startPage.offset) 0
        // Debounce for key input
        else 300
    }.toNetworkBackedTiledList(
        startQuery = value
    ) { query ->
        if (query.searchQuery.isEmpty()) emptyFlow()
        else searchRepository.searchFor(query)
            .map<SearchResults, List<T>>(SearchResults::itemsFor)
    }
        .map { tiledItems ->
            if (tiledItems.isEmpty()) tiledListOf(value to SearchItem.Empty)
            else tiledItems
                .distinctBy(SearchResult::id)
                .map { SearchItem.Loaded(it) }
        }
        .debounce { tiledItems ->
            // If empty, the search query might have just changed.
            // Allow items to be fetched for item position animations
            if (tiledItems.first() is SearchItem.Empty) 350L
            else 0L
        }
        .stateIn(
            scope = scope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyTiledList()
        )
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