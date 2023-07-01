package com.example.musify.data.repositories.searchrepository

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import com.example.musify.data.paging.SpotifyAlbumSearchPagingSource
import com.example.musify.data.paging.SpotifyArtistSearchPagingSource
import com.example.musify.data.paging.SpotifyEpisodeSearchPagingSource
import com.example.musify.data.paging.SpotifyPlaylistSearchPagingSource
import com.example.musify.data.paging.SpotifyPodcastSearchPagingSource
import com.example.musify.data.paging.SpotifyTrackSearchPagingSource
import com.example.musify.data.remote.musicservice.SearchQueryType
import com.example.musify.data.remote.musicservice.SpotifyService
import com.example.musify.data.remote.response.toSearchResults
import com.example.musify.data.repositories.tokenrepository.TokenRepository
import com.example.musify.data.repositories.tokenrepository.runCatchingWithToken
import com.example.musify.data.utils.FetchedResource
import com.example.musify.domain.MusifyErrorType
import com.example.musify.domain.SearchResult
import com.example.musify.domain.SearchResults
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class MusifySearchRepository @Inject constructor(
    private val tokenRepository: TokenRepository,
    private val spotifyService: SpotifyService,
    private val pagingConfig: PagingConfig
) : SearchRepository {
    override suspend fun fetchSearchResultsForQuery(
        searchQuery: String,
        countryCode: String
    ): FetchedResource<SearchResults, MusifyErrorType> = tokenRepository.runCatchingWithToken {
        spotifyService.search(searchQuery, countryCode, it).toSearchResults()
    }

    override fun getPaginatedSearchStreamForAlbums(
        searchQuery: String,
        countryCode: String
    ): Flow<PagingData<SearchResult.AlbumSearchResult>> = Pager(pagingConfig) {
        SpotifyAlbumSearchPagingSource(
            searchQuery = searchQuery,
            countryCode = countryCode,
            tokenRepository = tokenRepository,
            spotifyService = spotifyService
        )
    }.flow

    override fun getPaginatedSearchStreamForArtists(
        searchQuery: String,
        countryCode: String
    ): Flow<PagingData<SearchResult.ArtistSearchResult>> = Pager(pagingConfig) {
        SpotifyArtistSearchPagingSource(
            searchQuery = searchQuery,
            countryCode = countryCode,
            tokenRepository = tokenRepository,
            spotifyService = spotifyService
        )
    }.flow

    override fun getPaginatedSearchStreamForTracks(
        searchQuery: String,
        countryCode: String
    ): Flow<PagingData<SearchResult.TrackSearchResult>> = Pager(pagingConfig) {
        SpotifyTrackSearchPagingSource(
            searchQuery = searchQuery,
            countryCode = countryCode,
            tokenRepository = tokenRepository,
            spotifyService = spotifyService
        )
    }.flow

    override fun getPaginatedSearchStreamForPlaylists(
        searchQuery: String,
        countryCode: String
    ): Flow<PagingData<SearchResult.PlaylistSearchResult>> = Pager(pagingConfig) {
        SpotifyPlaylistSearchPagingSource(
            searchQuery = searchQuery,
            countryCode = countryCode,
            tokenRepository = tokenRepository,
            spotifyService = spotifyService
        )
    }.flow

    override fun getPaginatedSearchStreamForPodcasts(
        searchQuery: String,
        countryCode: String
    ): Flow<PagingData<SearchResult.PodcastSearchResult>> = Pager(pagingConfig) {
        SpotifyPodcastSearchPagingSource(
            searchQuery = searchQuery,
            countryCode = countryCode,
            tokenRepository = tokenRepository,
            spotifyService = spotifyService
        )
    }.flow

    override fun getPaginatedSearchStreamForEpisodes(
        searchQuery: String,
        countryCode: String
    ): Flow<PagingData<SearchResult.EpisodeSearchResult>> = Pager(pagingConfig) {
        SpotifyEpisodeSearchPagingSource(
            searchQuery = searchQuery,
            countryCode = countryCode,
            tokenRepository = tokenRepository,
            spotifyService = spotifyService
        )
    }.flow

    override fun searchFor(
        contentQuery: ContentQuery
    ): Flow<SearchResults> = flow {
        emit(
            spotifyService.search(
                searchQuery = contentQuery.searchQuery,
                market = contentQuery.countryCode,
                token = tokenRepository.getValidBearerToken(),
                limit = contentQuery.page.limit,
                offset = contentQuery.page.offset,
                type = contentQuery.type.value
            ).toSearchResults()
        )
    }
}

