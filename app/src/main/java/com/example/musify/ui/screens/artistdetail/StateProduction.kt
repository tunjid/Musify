package com.example.musify.ui.screens.artistdetail

import com.example.musify.data.repositories.albumsrepository.AlbumsRepository
import com.example.musify.data.repositories.albumsrepository.ArtistAlbumsQuery
import com.example.musify.data.repositories.tracksrepository.TracksRepository
import com.example.musify.data.tiling.Page
import com.example.musify.data.tiling.toTiledList
import com.example.musify.data.utils.FetchedResource
import com.example.musify.data.utils.NetworkMonitor
import com.example.musify.data.utils.onConnected
import com.example.musify.domain.SearchResult
import com.example.musify.usecases.getCurrentlyPlayingTrackUseCase.GetCurrentlyPlayingTrackUseCase
import com.example.musify.usecases.getPlaybackLoadingStatusUseCase.GetPlaybackLoadingStatusUseCase
import com.tunjid.mutator.Mutation
import com.tunjid.mutator.coroutines.SuspendingStateHolder
import com.tunjid.mutator.coroutines.actionStateFlowProducer
import com.tunjid.mutator.coroutines.mapToMutation
import com.tunjid.mutator.coroutines.toMutationStream
import com.tunjid.tiler.TiledList
import com.tunjid.tiler.distinctBy
import com.tunjid.tiler.emptyTiledList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

sealed class ArtistDetailAction {
    data class LoadAround(val query: ArtistAlbumsQuery?) : ArtistDetailAction()
}

data class ArtistDetailUiState(
    val artistName: String,
    val artistImageUrlString: String,
    val currentQuery: ArtistAlbumsQuery,
    val popularTracks: List<SearchResult.TrackSearchResult> = emptyList(),
    val loadingState: ArtistDetailScreenLoadingState = ArtistDetailScreenLoadingState.Loading,
    val currentlyPlayingTrack: SearchResult.TrackSearchResult? = null,
    val releases: TiledList<ArtistAlbumsQuery, SearchResult.AlbumSearchResult> = emptyTiledList()
)

/**
 * A sealed class hierarchy consisting of all UI states that are related to a screen
 * displaying the details of an artist.
 */
sealed class ArtistDetailScreenLoadingState {
    object Idle : ArtistDetailScreenLoadingState()
    object Loading : ArtistDetailScreenLoadingState()
    data class Error(private val message: String) : ArtistDetailScreenLoadingState()
}

fun CoroutineScope.aristDetailStateProducer(
    artistId: String,
    artistName: String,
    artistImageUrlString: String,
    countryCode: String,
    networkMonitor: NetworkMonitor,
    albumsRepository: AlbumsRepository,
    getCurrentlyPlayingTrackUseCase: GetCurrentlyPlayingTrackUseCase,
    getPlaybackLoadingStatusUseCase: GetPlaybackLoadingStatusUseCase,
    tracksRepository: TracksRepository,
) = actionStateFlowProducer<ArtistDetailAction, ArtistDetailUiState>(
    initialState = ArtistDetailUiState(
        artistName = artistName,
        artistImageUrlString = artistImageUrlString,
        currentQuery = ArtistAlbumsQuery(
            artistId = artistId,
            countryCode = countryCode,
            page = Page(offset = 0)
        )
    ),
    mutationFlows = listOf(
        getCurrentlyPlayingTrackUseCase.playingTrackMutations(),
        getPlaybackLoadingStatusUseCase.loadingStatusMutations(),
        tracksRepository.popularTrackMutations(
            artistId = artistId,
            countryCode = countryCode,
            networkMonitor = networkMonitor
        )
    ),
    actionTransform = { actions ->
        actions.toMutationStream {
            when (val action = type()) {
                is ArtistDetailAction.LoadAround -> action.flow.tracksMutations(
                    albumsRepository
                )
            }
        }
    }
)

private fun GetCurrentlyPlayingTrackUseCase.playingTrackMutations(): Flow<Mutation<ArtistDetailUiState>> =
    currentlyPlayingTrackStream.mapToMutation {
        copy(currentlyPlayingTrack = it)
    }

private fun GetPlaybackLoadingStatusUseCase.loadingStatusMutations(): Flow<Mutation<ArtistDetailUiState>> =
    loadingStatusStream.mapToMutation { isPlaybackLoading ->
        copy(
            loadingState = when {
                isPlaybackLoading && loadingState !is ArtistDetailScreenLoadingState.Loading ->
                    ArtistDetailScreenLoadingState.Loading

                !isPlaybackLoading && loadingState is ArtistDetailScreenLoadingState.Loading ->
                    ArtistDetailScreenLoadingState.Idle

                else -> loadingState
            }
        )
    }

private fun TracksRepository.popularTrackMutations(
    artistId: String,
    countryCode: String,
    networkMonitor: NetworkMonitor,
): Flow<Mutation<ArtistDetailUiState>> = networkMonitor
    .onConnected()
    .map {
        fetchTopTenTracksForArtistWithId(
            artistId = artistId,
            countryCode = countryCode
        )
    }
    .mapToMutation { fetchResult ->
        when (fetchResult) {
            is FetchedResource.Failure -> copy(
                loadingState = ArtistDetailScreenLoadingState.Error(
                    "Error loading tracks, please check internet connection"
                )
            )

            is FetchedResource.Success -> copy(
                popularTracks = fetchResult.data,
                loadingState = ArtistDetailScreenLoadingState.Idle
            )
        }
    }

context(SuspendingStateHolder<ArtistDetailUiState>)
private suspend fun Flow<ArtistDetailAction.LoadAround>.tracksMutations(
    albumsRepository: AlbumsRepository
): Flow<Mutation<ArtistDetailUiState>> =
    map { it.query ?: state().currentQuery }
        .toTiledList(
            startQuery = state().currentQuery,
            queryFor = { copy(page = it) },
            fetcher = albumsRepository::albumsFor
        )
        .mapToMutation {
            copy(releases = it.distinctBy(SearchResult.AlbumSearchResult::id))
        }
