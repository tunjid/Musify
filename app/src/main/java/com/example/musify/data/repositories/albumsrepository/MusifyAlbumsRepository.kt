package com.example.musify.data.repositories.albumsrepository

import com.example.musify.data.remote.musicservice.SpotifyService
import com.example.musify.data.remote.response.AlbumMetadataResponse
import com.example.musify.data.remote.response.toAlbumSearchResult
import com.example.musify.data.remote.response.toAlbumSearchResultList
import com.example.musify.data.repositories.tokenrepository.TokenRepository
import com.example.musify.data.repositories.tokenrepository.runCatchingWithToken
import com.example.musify.data.utils.FetchedResource
import com.example.musify.data.utils.NetworkMonitor
import com.example.musify.domain.MusifyErrorType
import com.example.musify.domain.SearchResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class MusifyAlbumsRepository @Inject constructor(
    private val tokenRepository: TokenRepository,
    private val spotifyService: SpotifyService,
    private val networkMonitor: NetworkMonitor,
) : AlbumsRepository {

    override suspend fun fetchAlbumsOfArtistWithId(
        artistId: String,
        countryCode: String //ISO 3166-1 alpha-2 country code
    ): FetchedResource<List<SearchResult.AlbumSearchResult>, MusifyErrorType> =
        tokenRepository.runCatchingWithToken {
            spotifyService.getAlbumsOfArtistWithId(
                artistId,
                countryCode,
                it
            ).toAlbumSearchResultList()
        }

    override suspend fun fetchAlbumWithId(
        albumId: String,
        countryCode: String
    ): FetchedResource<SearchResult.AlbumSearchResult, MusifyErrorType> =
        tokenRepository.runCatchingWithToken {
            spotifyService.getAlbumWithId(albumId, countryCode, it).toAlbumSearchResult()
        }

    override fun albumsFor(
        query: ArtistAlbumsQuery
    ): Flow<List<SearchResult.AlbumSearchResult>> = networkMonitor.isOnline
        .filter { it }
        .map {
            spotifyService.getAlbumsOfArtistWithId(
                artistId = query.artistId,
                market = query.countryCode,
                token = tokenRepository.getValidBearerToken(),
                limit = query.page.limit,
                offset = query.page.offset,
            )
                .items
                .map(AlbumMetadataResponse::toAlbumSearchResult)
        }
}