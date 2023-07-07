package com.example.musify.ui.screens.albumdetail

import com.example.musify.data.repositories.tracksrepository.TracksRepository
import com.example.musify.data.utils.FetchedResource
import com.example.musify.domain.SearchResult
import com.example.musify.usecases.getCurrentlyPlayingTrackUseCase.GetCurrentlyPlayingTrackUseCase
import com.example.musify.usecases.getPlaybackLoadingStatusUseCase.GetPlaybackLoadingStatusUseCase
import com.tunjid.mutator.Mutation
import com.tunjid.mutator.coroutines.actionStateFlowProducer
import com.tunjid.mutator.coroutines.mapToMutation
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

data class AlbumDetailUiState(
    val albumArtUrl: String,
    val artists: String,
    val albumName: String,
    val yearOfRelease: String,
    val loadingState: AlbumDetailLoadingState = AlbumDetailLoadingState.Loading,
    val tracks: List<SearchResult.TrackSearchResult> = emptyList(),
    val currentlyPlayingTrack: SearchResult.TrackSearchResult? = null,
)

sealed class AlbumDetailLoadingState {
    object Idle : AlbumDetailLoadingState()
    object Loading : AlbumDetailLoadingState()
    data class Error(private val message: String) : AlbumDetailLoadingState()
}

fun CoroutineScope.albumDetailStateProducer(
    albumId: String,
    albumArtUrl: String,
    albumName: String,
    artists: String,
    yearOfRelease: String,
    countryCode: String,
    tracksRepository: TracksRepository,
    getCurrentlyPlayingTrackUseCase: GetCurrentlyPlayingTrackUseCase,
    getPlaybackLoadingStatusUseCase: GetPlaybackLoadingStatusUseCase,
) = actionStateFlowProducer<Unit, AlbumDetailUiState>(
    initialState = AlbumDetailUiState(
        albumArtUrl = albumArtUrl,
        artists = artists,
        albumName = albumName,
        yearOfRelease = yearOfRelease
    ),
    mutationFlows = listOf(
        getCurrentlyPlayingTrackUseCase.playingTrackMutations(),
        getPlaybackLoadingStatusUseCase.loadingStatusMutations(),
        tracksRepository.trackListMutations(
            albumId = albumId,
            countryCode = countryCode
        )
    ),
)

private fun GetCurrentlyPlayingTrackUseCase.playingTrackMutations(): Flow<Mutation<AlbumDetailUiState>> =
    currentlyPlayingTrackStream.mapToMutation {
        copy(currentlyPlayingTrack = it)
    }

private fun GetPlaybackLoadingStatusUseCase.loadingStatusMutations(): Flow<Mutation<AlbumDetailUiState>> =
    loadingStatusStream.mapToMutation { isPlaybackLoading ->
        copy(
            loadingState = when {
                isPlaybackLoading && loadingState !is AlbumDetailLoadingState.Loading ->
                    AlbumDetailLoadingState.Loading

                !isPlaybackLoading && loadingState is AlbumDetailLoadingState.Loading ->
                    AlbumDetailLoadingState.Idle

                else -> loadingState
            }
        )
    }

private fun TracksRepository.trackListMutations(
    albumId: String,
    countryCode: String,
): Flow<Mutation<AlbumDetailUiState>> = flow {
    val result = fetchTracksForAlbumWithId(
        albumId = albumId,
        countryCode = countryCode,
    )
    if (result is FetchedResource.Success) emit {
        copy(
            tracks = result.data,
            loadingState = AlbumDetailLoadingState.Idle
        )
    } else emit {
        copy(
            loadingState =
            AlbumDetailLoadingState.Error(
                "Unable to fetch tracks. Please check internet connection."
            )
        )
    }
}