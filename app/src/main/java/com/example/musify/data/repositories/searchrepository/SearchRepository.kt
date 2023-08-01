package com.example.musify.data.repositories.searchrepository

import com.example.musify.data.remote.musicservice.SearchQueryType
import com.example.musify.data.tiling.Page
import com.example.musify.data.tiling.PagedQuery
import com.example.musify.data.utils.FetchedResource
import com.example.musify.domain.MusifyErrorType
import com.example.musify.domain.SearchResult
import com.example.musify.domain.SearchResults
import kotlinx.coroutines.flow.Flow

data class ContentQuery(
    override val page: Page,
    val searchQuery: String,
    val countryCode: String,
    val type: SearchQueryType,
) : PagedQuery<ContentQuery> {
    override fun updatePage(page: Page): ContentQuery = copy(page = page)
}

/**
 * A repository that contains all methods related to searching.
 */
interface SearchRepository {
    suspend fun fetchSearchResultsForQuery(
        searchQuery: String,
        countryCode: String
    ): FetchedResource<SearchResults, MusifyErrorType>

    fun searchFor(
        contentQuery: ContentQuery
    ): Flow<SearchResults>
}

@Suppress("UNCHECKED_CAST")
inline fun <reified T : SearchResult> SearchResults.itemsFor(): List<T> =
    when (T::class) {
        SearchResult.AlbumSearchResult::class -> albums
        SearchResult.ArtistSearchResult::class -> artists
        SearchResult.TrackSearchResult::class -> tracks
        SearchResult.PlaylistSearchResult::class -> playlists
        SearchResult.PodcastSearchResult::class -> shows
        SearchResult.EpisodeSearchResult::class -> episodes
        else -> emptyList()
    } as List<T>
