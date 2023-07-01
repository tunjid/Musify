package com.example.musify.data.repositories.podcastsrepository

import androidx.paging.PagingData
import com.example.musify.data.tiling.Page
import com.example.musify.data.tiling.PagedQuery
import com.example.musify.data.utils.FetchedResource
import com.example.musify.domain.MusifyErrorType
import com.example.musify.domain.PodcastEpisode
import com.example.musify.domain.PodcastShow
import kotlinx.coroutines.flow.Flow

data class PodcastQuery(
    override val page: Page,
    val showId: String,
    val countryCode: String,
) : PagedQuery

/**
 * A repository that contains all methods related to podcasts.
 */
interface PodcastsRepository {
    suspend fun fetchPodcastEpisode(
        episodeId: String,
        countryCode: String
    ): FetchedResource<PodcastEpisode, MusifyErrorType>

    suspend fun fetchPodcastShow(
        showId: String,
        countryCode: String
    ): FetchedResource<PodcastShow, MusifyErrorType>

    fun podcastsFor(
        query: PodcastQuery
    ): Flow<List<PodcastEpisode>>
}