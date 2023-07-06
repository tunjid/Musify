package com.example.musify.viewmodels.artistviewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.example.musify.data.repositories.albumsrepository.AlbumsRepository
import com.example.musify.data.repositories.albumsrepository.ArtistAlbumsQuery
import com.example.musify.data.repositories.tracksrepository.TracksRepository
import com.example.musify.data.tiling.Page
import com.example.musify.data.tiling.toTiledList
import com.example.musify.data.utils.FetchedResource
import com.example.musify.domain.SearchResult
import com.example.musify.ui.navigation.MusifyNavigationDestinations
import com.example.musify.usecases.getCurrentlyPlayingTrackUseCase.GetCurrentlyPlayingTrackUseCase
import com.example.musify.usecases.getPlaybackLoadingStatusUseCase.GetPlaybackLoadingStatusUseCase
import com.example.musify.viewmodels.getCountryCode
import com.tunjid.mutator.Mutation
import com.tunjid.mutator.coroutines.SuspendingStateHolder
import com.tunjid.mutator.coroutines.actionStateFlowProducer
import com.tunjid.mutator.coroutines.mapToMutation
import com.tunjid.mutator.coroutines.toMutationStream
import com.tunjid.tiler.TiledList
import com.tunjid.tiler.distinctBy
import com.tunjid.tiler.emptyTiledList
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import javax.inject.Inject

data class ArtistDetailState(
    val artistName: String,
    val artistImageUrlString: String,
    val currentQuery: ArtistAlbumsQuery,
    val popularTracks: List<SearchResult.TrackSearchResult> = emptyList(),
    val loadingState: ArtistDetailScreenLoadingState = ArtistDetailScreenLoadingState.Loading,
    val currentlyPlayingTrack: SearchResult.TrackSearchResult? = null,
    val releases: TiledList<ArtistAlbumsQuery, SearchResult.AlbumSearchResult> = emptyTiledList()
)

sealed class ArtistDetailAction {
    data class LoadAround(val query: ArtistAlbumsQuery?) : ArtistDetailAction()
}

/**
 * A sealed class hierarchy consisting of all UI states that are related to a screen
 * displaying the details of an artist.
 */
sealed class ArtistDetailScreenLoadingState {
    object Idle : ArtistDetailScreenLoadingState()
    object Loading : ArtistDetailScreenLoadingState()
    data class Error(private val message: String) : ArtistDetailScreenLoadingState()
}

@HiltViewModel
class ArtistDetailViewModel @Inject constructor(
    application: Application,
    savedStateHandle: SavedStateHandle,
    albumsRepository: AlbumsRepository,
    getCurrentlyPlayingTrackUseCase: GetCurrentlyPlayingTrackUseCase,
    getPlaybackLoadingStatusUseCase: GetPlaybackLoadingStatusUseCase,
    tracksRepository: TracksRepository,
) : AndroidViewModel(application) {

    private val artistId =
        savedStateHandle.get<String>(MusifyNavigationDestinations.ArtistDetailScreen.NAV_ARG_ARTIST_ID)!!

    private val stateProducer =
        viewModelScope.actionStateFlowProducer<ArtistDetailAction, ArtistDetailState>(
            initialState = ArtistDetailState(
                artistName = savedStateHandle[MusifyNavigationDestinations.ArtistDetailScreen.NAV_ARG_ARTIST_NAME]!!,
                artistImageUrlString = URLDecoder.decode(
                    savedStateHandle[MusifyNavigationDestinations.ArtistDetailScreen.NAV_ARG_ARTIST_NAME]!!,
                    StandardCharsets.UTF_8.toString()
                ),
                currentQuery = ArtistAlbumsQuery(
                    artistId = artistId,
                    countryCode = getCountryCode(),
                    page = Page(offset = 0)
                )
            ),
            mutationFlows = listOf(
                getCurrentlyPlayingTrackUseCase.playingTrackMutations(),
                getPlaybackLoadingStatusUseCase.loadingStatusMutations(),
                tracksRepository.popularTrackMutations(
                    artistId = artistId,
                    countryCode = getCountryCode()
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

    val state = stateProducer.state
    val actions = stateProducer.accept
}

private fun GetCurrentlyPlayingTrackUseCase.playingTrackMutations(): Flow<Mutation<ArtistDetailState>> =
    currentlyPlayingTrackStream.mapToMutation {
        copy(currentlyPlayingTrack = it)
    }

private fun GetPlaybackLoadingStatusUseCase.loadingStatusMutations(): Flow<Mutation<ArtistDetailState>> =
    loadingStatusStream.mapToMutation { isPlaybackLoading ->
        copy(
            loadingState = when {
                isPlaybackLoading && loadingState !is ArtistDetailScreenLoadingState.Loading -> ArtistDetailScreenLoadingState.Loading
                !isPlaybackLoading && loadingState is ArtistDetailScreenLoadingState.Loading -> ArtistDetailScreenLoadingState.Idle
                else -> loadingState
            }
        )
    }

private fun TracksRepository.popularTrackMutations(
    artistId: String,
    countryCode: String
): Flow<Mutation<ArtistDetailState>> = flow {
    val fetchResult = fetchTopTenTracksForArtistWithId(
        artistId = artistId,
        countryCode = countryCode
    )
    when (fetchResult) {
        is FetchedResource.Failure -> emit {
            copy(loadingState = ArtistDetailScreenLoadingState.Error("Error loading tracks, please check internet connection"))
        }

        is FetchedResource.Success -> emit {
            copy(
                popularTracks = fetchResult.data,
                loadingState = ArtistDetailScreenLoadingState.Idle
            )
        }
    }
}

context(SuspendingStateHolder<ArtistDetailState>)
private suspend fun Flow<ArtistDetailAction.LoadAround>.tracksMutations(
    albumsRepository: AlbumsRepository
): Flow<Mutation<ArtistDetailState>> =
    map { it.query ?: state().currentQuery }
        .toTiledList(
            startQuery = state().currentQuery,
            queryFor = { copy(page = it) },
            fetcher = albumsRepository::albumsFor
        )
        .mapToMutation {
            copy(releases = it.distinctBy(SearchResult.AlbumSearchResult::id))
        }