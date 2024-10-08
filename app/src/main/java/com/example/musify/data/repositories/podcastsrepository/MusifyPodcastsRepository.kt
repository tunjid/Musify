package com.example.musify.data.repositories.podcastsrepository

import com.example.musify.data.remote.musicservice.SpotifyService
import com.example.musify.data.remote.response.toPodcastEpisode
import com.example.musify.data.remote.response.toPodcastShow
import com.example.musify.data.repositories.tokenrepository.TokenRepository
import com.example.musify.data.repositories.tokenrepository.runCatchingWithToken
import com.example.musify.data.utils.FetchedResource
import com.example.musify.data.utils.NetworkMonitor
import com.example.musify.domain.MusifyErrorType
import com.example.musify.domain.PodcastEpisode
import com.example.musify.domain.PodcastShow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class MusifyPodcastsRepository @Inject constructor(
    private val tokenRepository: TokenRepository,
    private val spotifyService: SpotifyService,
    private val networkMonitor: NetworkMonitor,
) : PodcastsRepository {

    override suspend fun fetchPodcastEpisode(
        episodeId: String,
        countryCode: String
    ): FetchedResource<PodcastEpisode, MusifyErrorType> = tokenRepository.runCatchingWithToken {
        spotifyService.getEpisodeWithId(
            token = it, id = episodeId, market = countryCode
        ).toPodcastEpisode()
    }

    override suspend fun fetchPodcastShow(
        showId: String,
        countryCode: String
    ): FetchedResource<PodcastShow, MusifyErrorType> = tokenRepository.runCatchingWithToken {
        spotifyService.getShowWithId(
            token = it, id = showId, market = countryCode
        ).toPodcastShow()
    }

    override fun podcastsFor(
        query: PodcastQuery
    ): Flow<List<PodcastEpisode>> = networkMonitor.isOnline
        .filter { it }
        .map {
            val showResponse = spotifyService.getShowWithId(
                token = tokenRepository.getValidBearerToken(),
                id = query.showId,
                market = query.countryCode,
            )
            spotifyService.getEpisodesForShowWithId(
                token = tokenRepository.getValidBearerToken(),
                id = query.showId,
                market = query.countryCode,
                limit = query.page.limit,
                offset = query.page.offset
            )
                .items
                .map {
                    it.toPodcastEpisode(showResponse)
                }
        }
}
