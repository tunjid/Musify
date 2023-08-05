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
import com.example.musify.musicplayer.MusicPlaybackMonitor
import com.example.musify.musicplayer.currentlyPlayingTrackStream
import com.tunjid.mutator.Mutation
import com.tunjid.mutator.coroutines.SuspendingStateHolder
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
    val contentQueryMap: Map<SearchQueryType, ContentQuery>,
    val tiledItems: SearchTiledListFlows = SearchTiledListFlows(
        albumTiledListFlow = MutableStateFlow(emptyTiledList()),
        artistTiledListFlow = MutableStateFlow(emptyTiledList()),
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
    val artistTiledListFlow: StateFlow<TiledList<ContentQuery, SearchItem<SearchResult.ArtistSearchResult>>>,
    val trackTiledListFlow: StateFlow<TiledList<ContentQuery, SearchItem<SearchResult.TrackSearchResult>>>,
    val playlistTiledListFlow: StateFlow<TiledList<ContentQuery, SearchItem<SearchResult.PlaylistSearchResult>>>,
    val podcastTiledListFlow: StateFlow<TiledList<ContentQuery, SearchItem<SearchResult.PodcastSearchResult>>>,
    val episodeTiledListFlow: StateFlow<TiledList<ContentQuery, SearchItem<SearchResult.EpisodeSearchResult>>>,
)

fun CoroutineScope.searchStateProducer(
    countryCode: String,
    networkMonitor: NetworkMonitor,
    musicPlaybackMonitor: MusicPlaybackMonitor,
    genresRepository: GenresRepository,
    searchRepository: SearchRepository,
) = actionStateFlowProducer<SearchAction, SearchUiState>(
    initialState = SearchUiState(
        genres = genresRepository.fetchAvailableGenres(),
        contentQueryMap = mapSearchQueryTypesTo { searchQueryType ->
            searchQueryType.contentQueryFor(
                searchQuery = "",
                countryCode = countryCode
            )
        }
    ),
    mutationFlows = listOf(
        networkMonitor.isOnlineMutations(),
        musicPlaybackMonitor.playingTrackMutations(),
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

private fun MusicPlaybackMonitor.playingTrackMutations(): Flow<Mutation<SearchUiState>> =
    currentlyPlayingTrackStream.mapToMutation {
        copy(currentlyPlayingTrack = it)
    }

private fun Flow<SearchAction.SearchFilterChange>.searchFilterMutations(): Flow<Mutation<SearchUiState>> =
    mapToMutation {
        copy(selectedSearchFilter = it.searchFilter)
    }

context(SuspendingStateHolder<SearchUiState>)
private fun Flow<SearchAction.Searches>.searchMutations(
    countryCode: String,
    scope: CoroutineScope,
    searchRepository: SearchRepository
): Flow<Mutation<SearchUiState>> = flow {
    val initialState = state()
    // Restore queries from last session
    val queryTypeToQueryStateFlow = mapSearchQueryTypesTo {
        MutableStateFlow(initialState.contentQueryMap.getValue(it))
    }

    // Emit the tiled item state flows first
    emit {
        copy(
            tiledItems = SearchTiledListFlows(
                albumTiledListFlow = queryTypeToQueryStateFlow
                    .getValue(SearchQueryType.ALBUM)
                    .toTiledList(
                        scope = scope,
                        initialValue = initialState.tiledItems.albumTiledListFlow.value,
                        searchRepository = searchRepository
                    ),
                artistTiledListFlow = queryTypeToQueryStateFlow
                    .getValue(SearchQueryType.ARTIST)
                    .toTiledList(
                        scope = scope,
                        initialValue = initialState.tiledItems.artistTiledListFlow.value,
                        searchRepository = searchRepository
                    ),
                trackTiledListFlow = queryTypeToQueryStateFlow
                    .getValue(SearchQueryType.TRACK)
                    .toTiledList(
                        scope = scope,
                        initialValue = initialState.tiledItems.trackTiledListFlow.value,
                        searchRepository = searchRepository
                    ),
                playlistTiledListFlow = queryTypeToQueryStateFlow
                    .getValue(SearchQueryType.PLAYLIST)
                    .toTiledList(
                        scope = scope,
                        initialValue = initialState.tiledItems.playlistTiledListFlow.value,
                        searchRepository = searchRepository
                    ),
                podcastTiledListFlow = queryTypeToQueryStateFlow
                    .getValue(SearchQueryType.SHOW)
                    .toTiledList(
                        scope = scope,
                        initialValue = initialState.tiledItems.podcastTiledListFlow.value,
                        searchRepository = searchRepository
                    ),
                episodeTiledListFlow = queryTypeToQueryStateFlow
                    .getValue(SearchQueryType.EPISODE)
                    .toTiledList(
                        scope = scope,
                        initialValue = initialState.tiledItems.episodeTiledListFlow.value,
                        searchRepository = searchRepository
                    )
            )
        )
    }

    // Collect from the backing flow and update searches as appropriate
    collect { action ->
        when (action) {
            is SearchAction.Searches.LoadAround -> when (val type = action.contentQuery?.type) {
                null -> Unit
                else -> {
                    queryTypeToQueryStateFlow.getValue(type).value = action.contentQuery
                    // Update UI state with the latest query
                    emit { copy(contentQueryMap = contentQueryMap + (type to action.contentQuery)) }
                }
            }

            // Only pages in the pager will have their search queries executed, but the queries
            // for offscreen pages will be saved to begin execution when they're visible
            is SearchAction.Searches.Search -> {
                val newContentQueryMap = mapSearchQueryTypesTo { searchQueryType ->
                    searchQueryType.contentQueryFor(
                        searchQuery = action.searchQuery,
                        countryCode = countryCode
                    )
                }
                queryTypeToQueryStateFlow.forEach { (type, stateFlow) ->
                    stateFlow.value = newContentQueryMap.getValue(type)
                }
                // Update UI state with the latest queries
                emit { copy(contentQueryMap = newContentQueryMap) }
            }
        }
    }
}

private inline fun <reified T : SearchResult> MutableStateFlow<ContentQuery>.toTiledList(
    scope: CoroutineScope,
    initialValue: TiledList<ContentQuery, SearchItem<T>>,
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
            initialValue = initialValue
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

fun <T> mapSearchQueryTypesTo(mapper: (SearchQueryType) -> T) =
    SearchQueryType.values().associateBy(
        keySelector = { it },
        valueTransform = mapper
    )
