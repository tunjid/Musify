package com.example.musify.ui.screens.albumdetail

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.example.musify.data.repositories.tracksrepository.TracksRepository
import com.example.musify.data.utils.FetchedResource
import com.example.musify.domain.SearchResult
import com.example.musify.ui.navigation.MusifyNavigationDestinations
import com.example.musify.usecases.getCurrentlyPlayingTrackUseCase.GetCurrentlyPlayingTrackUseCase
import com.example.musify.usecases.getPlaybackLoadingStatusUseCase.GetPlaybackLoadingStatusUseCase
import com.example.musify.viewmodels.getCountryCode
import com.tunjid.mutator.Mutation
import com.tunjid.mutator.coroutines.actionStateFlowProducer
import com.tunjid.mutator.coroutines.mapToMutation
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

data class AlbumDetailState(
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

@HiltViewModel
class AlbumDetailViewModel @Inject constructor(
    application: Application,
    savedStateHandle: SavedStateHandle,
    getCurrentlyPlayingTrackUseCase: GetCurrentlyPlayingTrackUseCase,
    getPlaybackLoadingStatusUseCase: GetPlaybackLoadingStatusUseCase,
    tracksRepository: TracksRepository,
) : AndroidViewModel(application) {

    private val albumId =
        savedStateHandle.get<String>(MusifyNavigationDestinations.AlbumDetailScreen.NAV_ARG_ALBUM_ID)!!

    private val stateProducer =
        viewModelScope.actionStateFlowProducer<Unit, AlbumDetailState>(
            initialState = AlbumDetailState(
                albumArtUrl = savedStateHandle[MusifyNavigationDestinations.AlbumDetailScreen.NAV_ARG_ENCODED_IMAGE_URL_STRING]!!,
                albumName = savedStateHandle[MusifyNavigationDestinations.AlbumDetailScreen.NAV_ARG_ALBUM_NAME]!!,
                artists = savedStateHandle[MusifyNavigationDestinations.AlbumDetailScreen.NAV_ARG_ARTISTS_STRING]!!,
                yearOfRelease = savedStateHandle[MusifyNavigationDestinations.AlbumDetailScreen.NAV_ARG_YEAR_OF_RELEASE_STRING]!!,
            ),
            mutationFlows = listOf(
                getCurrentlyPlayingTrackUseCase.playingTrackMutations(),
                getPlaybackLoadingStatusUseCase.loadingStatusMutations(),
                tracksRepository.trackListMutations(
                    albumId = albumId,
                    countryCode = getCountryCode()
                )
            ),
        )

    val state = stateProducer.state
}

private fun GetCurrentlyPlayingTrackUseCase.playingTrackMutations(): Flow<Mutation<AlbumDetailState>> =
    currentlyPlayingTrackStream.mapToMutation {
        copy(currentlyPlayingTrack = it)
    }

private fun GetPlaybackLoadingStatusUseCase.loadingStatusMutations(): Flow<Mutation<AlbumDetailState>> =
    loadingStatusStream.mapToMutation { isPlaybackLoading ->
        copy(
            loadingState = when {
                isPlaybackLoading && loadingState !is AlbumDetailLoadingState.Loading -> AlbumDetailLoadingState.Loading
                !isPlaybackLoading && loadingState is AlbumDetailLoadingState.Loading -> AlbumDetailLoadingState.Idle
                else -> loadingState
            }
        )
    }

private fun TracksRepository.trackListMutations(
    albumId: String,
    countryCode: String,
): Flow<Mutation<AlbumDetailState>> = flow {
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
            AlbumDetailLoadingState.Error("Unable to fetch tracks. Please check internet connection.")
        )
    }
}