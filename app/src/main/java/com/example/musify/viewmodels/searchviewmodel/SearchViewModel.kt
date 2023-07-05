package com.example.musify.viewmodels.searchviewmodel

import android.app.Application
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
import com.example.musify.domain.Genre
import com.example.musify.domain.SearchResult
import com.example.musify.domain.SearchResults
import com.example.musify.usecases.getCurrentlyPlayingTrackUseCase.GetCurrentlyPlayingTrackUseCase
import com.example.musify.viewmodels.getCountryCode
import com.tunjid.mutator.Mutation
import com.tunjid.mutator.coroutines.actionStateFlowProducer
import com.tunjid.mutator.coroutines.mapToMutation
import com.tunjid.mutator.coroutines.toMutationStream
import com.tunjid.tiler.TiledList
import com.tunjid.tiler.distinctBy
import com.tunjid.tiler.emptyTiledList
import dagger.hilt.android.lifecycle.HiltViewModel
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
import javax.inject.Inject

data class SearchState(
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

data class SearchTiledListFlows(
    val albumTiledListFlow: StateFlow<TiledList<ContentQuery, SearchResult.AlbumSearchResult>>,
    val artistTiledListFLow: StateFlow<TiledList<ContentQuery, SearchResult.ArtistSearchResult>>,
    val trackTiledListFlow: StateFlow<TiledList<ContentQuery, SearchResult.TrackSearchResult>>,
    val playlistTiledListFlow: StateFlow<TiledList<ContentQuery, SearchResult.PlaylistSearchResult>>,
    val podcastTiledListFlow: StateFlow<TiledList<ContentQuery, SearchResult.PodcastSearchResult>>,
    val episodeTiledListFlow: StateFlow<TiledList<ContentQuery, SearchResult.EpisodeSearchResult>>,
)

sealed class SearchAction(val key: String) {

    sealed class Searches : SearchAction(key = "Searches") {
        data class LoadAround(val contentQuery: ContentQuery) : Searches()

        data class Search(val searchQuery: String) : Searches()
    }

    data class SearchFilterChange(val searchFilter: SearchFilter) : SearchAction(key = "FilterChange")
}

@HiltViewModel
class SearchViewModel @Inject constructor(
    application: Application,
    getCurrentlyPlayingTrackUseCase: GetCurrentlyPlayingTrackUseCase,
    searchRepository: SearchRepository,
    networkMonitor: NetworkMonitor,
    genresRepository: GenresRepository
) : AndroidViewModel(application) {

    private val stateProducer =
        viewModelScope.actionStateFlowProducer<SearchAction, SearchState>(
            initialState = SearchState(
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
                            countryCode = getCountryCode(),
                            scope = viewModelScope,
                            searchRepository = searchRepository,
                        )

                        is SearchAction.SearchFilterChange -> action.flow.searchFilterMutations()
                    }
                }
            }
        )

    val state = stateProducer.state
    val actions = stateProducer.accept
}

private fun NetworkMonitor.isOnlineMutations(): Flow<Mutation<SearchState>> =
    isOnline.mapToMutation {
        copy(isOnline = it)
    }

private fun GetCurrentlyPlayingTrackUseCase.playingTrackMutations(): Flow<Mutation<SearchState>> =
    currentlyPlayingTrackStream.mapToMutation {
        copy(currentlyPlayingTrack = it)
    }

private fun Flow<SearchAction.SearchFilterChange>.searchFilterMutations(): Flow<Mutation<SearchState>> =
    mapToMutation {
        copy(selectedSearchFilter = it.searchFilter)
    }

private fun Flow<SearchAction.Searches>.searchMutations(
    countryCode: String,
    scope: CoroutineScope,
    searchRepository: SearchRepository
): Flow<Mutation<SearchState>> = flow {
    val albumsQuery = SearchQueryType.ALBUM.contentFlow(
        countryCode = countryCode,
    )
    val albumsTiledList = albumsQuery.toTiledList(
        scope = scope,
        searchRepository = searchRepository,
        idFunction = SearchResult.AlbumSearchResult::id
    )

    val artistsQuery = SearchQueryType.ARTIST.contentFlow(
        countryCode = countryCode,
    )
    val artistsTiledList = artistsQuery.toTiledList(
        scope = scope,
        searchRepository = searchRepository,
        idFunction = SearchResult.ArtistSearchResult::id
    )

    val episodesQuery = SearchQueryType.EPISODE.contentFlow(
        countryCode = countryCode,
    )
    val episodesTiledList = episodesQuery.toTiledList(
        scope = scope,
        searchRepository = searchRepository,
        idFunction = SearchResult.EpisodeSearchResult::id
    )

    val playlistsQuery = SearchQueryType.PLAYLIST.contentFlow(
        countryCode = countryCode,
    )
    val playlistsTiledList = playlistsQuery.toTiledList(
        scope = scope,
        searchRepository = searchRepository,
        idFunction = SearchResult.PlaylistSearchResult::id
    )

    val showsQuery = SearchQueryType.SHOW.contentFlow(
        countryCode = countryCode,
    )
    val showsTiledList = showsQuery.toTiledList(
        scope = scope,
        searchRepository = searchRepository,
        idFunction = SearchResult.PodcastSearchResult::id
    )

    val tracksQuery = SearchQueryType.TRACK.contentFlow(
        countryCode = countryCode,
    )
    val trackTiledList = tracksQuery.toTiledList(
        scope = scope,
        searchRepository = searchRepository,
        idFunction = SearchResult.TrackSearchResult::id
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
    collect { search ->
        when (search) {
            is SearchAction.Searches.LoadAround -> when (search.contentQuery.type) {
                SearchQueryType.ALBUM -> albumsQuery.value = search.contentQuery
                SearchQueryType.ARTIST -> artistsQuery.value = search.contentQuery
                SearchQueryType.PLAYLIST -> playlistsQuery.value = search.contentQuery
                SearchQueryType.TRACK -> tracksQuery.value = search.contentQuery
                SearchQueryType.SHOW -> showsQuery.value = search.contentQuery
                SearchQueryType.EPISODE -> episodesQuery.value = search.contentQuery
            }

            is SearchAction.Searches.Search -> {
                albumsQuery.value = SearchQueryType.ALBUM.contentQueryFor(
                    searchQuery = search.searchQuery,
                    countryCode = countryCode,
                )
                artistsQuery.value = SearchQueryType.ARTIST.contentQueryFor(
                    searchQuery = search.searchQuery,
                    countryCode = countryCode,
                )
                episodesQuery.value = SearchQueryType.EPISODE.contentQueryFor(
                    searchQuery = search.searchQuery,
                    countryCode = countryCode,
                )
                playlistsQuery.value = SearchQueryType.PLAYLIST.contentQueryFor(
                    searchQuery = search.searchQuery,
                    countryCode = countryCode,
                )
                showsQuery.value = SearchQueryType.SHOW.contentQueryFor(
                    searchQuery = search.searchQuery,
                    countryCode = countryCode,
                )
                tracksQuery.value = SearchQueryType.TRACK.contentQueryFor(
                    searchQuery = search.searchQuery,
                    countryCode = countryCode,
                )
            }
        }
    }
}

private inline fun <reified T : SearchResult> MutableStateFlow<ContentQuery>.toTiledList(
    scope: CoroutineScope,
    searchRepository: SearchRepository,
    crossinline idFunction: (T) -> Any
): StateFlow<TiledList<ContentQuery, T>> =
    debounce {
        // Don't debounce the If its the first character or more is being loaded
        if (it.searchQuery.length < 2 || it.page.offset != 0) 0
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
