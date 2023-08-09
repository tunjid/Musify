package com.example.musify.data.repositories.searchrepository

import com.example.musify.data.remote.musicservice.SpotifyService
import com.example.musify.data.remote.response.toSearchResults
import com.example.musify.data.repositories.tokenrepository.TokenRepository
import com.example.musify.data.repositories.tokenrepository.runCatchingWithToken
import com.example.musify.data.utils.FetchedResource
import com.example.musify.data.utils.NetworkMonitor
import com.example.musify.domain.MusifyErrorType
import com.example.musify.domain.SearchResults
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class MusifySearchRepository @Inject constructor(
    private val tokenRepository: TokenRepository,
    private val spotifyService: SpotifyService,
    private val networkMonitor: NetworkMonitor,
) : SearchRepository {
    override suspend fun fetchSearchResultsForQuery(
        searchQuery: String,
        countryCode: String
    ): FetchedResource<SearchResults, MusifyErrorType> = tokenRepository.runCatchingWithToken {
        spotifyService.search(searchQuery, countryCode, it).toSearchResults()
    }

    override fun searchFor(
        contentQuery: ContentQuery
    ): Flow<SearchResults> = networkMonitor.isOnline
        .filter { it }
        .map {
            spotifyService.search(
                searchQuery = contentQuery.searchQuery,
                market = contentQuery.countryCode,
                token = tokenRepository.getValidBearerToken(),
                limit = contentQuery.page.limit,
                offset = contentQuery.page.offset,
                type = contentQuery.type.value
            ).toSearchResults()
        }
}

