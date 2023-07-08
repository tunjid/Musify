package com.example.musify.data.repositories.albumsrepository

import com.example.musify.data.tiling.Page
import com.example.musify.data.tiling.PagedQuery
import com.example.musify.data.utils.FetchedResource
import com.example.musify.domain.MusifyErrorType
import com.example.musify.domain.SearchResult
import kotlinx.coroutines.flow.Flow

data class ArtistAlbumsQuery(
    override val page: Page,
    val artistId: String,
    val countryCode: String,
) : PagedQuery

/**
 * A repository that contains methods related to albums. **All methods
 * of this interface will always return an instance of [SearchResult.AlbumSearchResult]**
 * encapsulated inside [FetchedResource.Success] if the resource was
 * fetched successfully. This ensures that the return value of all the
 * methods of [AlbumsRepository] will always return [SearchResult.AlbumSearchResult]
 * in the case of a successful fetch operation.
 */
interface AlbumsRepository {
    suspend fun fetchAlbumWithId(
        albumId: String,
        countryCode: String
    ): FetchedResource<SearchResult.AlbumSearchResult, MusifyErrorType>

    suspend fun fetchAlbumsOfArtistWithId(
        artistId: String,
        countryCode: String
    ): FetchedResource<List<SearchResult.AlbumSearchResult>, MusifyErrorType>

    fun albumsFor(
        query: ArtistAlbumsQuery
    ): Flow<List<SearchResult.AlbumSearchResult>>
}
