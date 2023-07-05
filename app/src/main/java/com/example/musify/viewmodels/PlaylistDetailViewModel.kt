package com.example.musify.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.example.musify.data.repositories.tracksrepository.PlaylistQuery
import com.example.musify.data.repositories.tracksrepository.TracksRepository
import com.example.musify.data.tiling.Page
import com.example.musify.data.tiling.toTiledList
import com.example.musify.domain.SearchResult
import com.example.musify.ui.navigation.MusifyNavigationDestinations
import com.example.musify.usecases.getCurrentlyPlayingTrackUseCase.GetCurrentlyPlayingTrackUseCase
import com.tunjid.mutator.Mutation
import com.tunjid.mutator.coroutines.SuspendingStateHolder
import com.tunjid.mutator.coroutines.actionStateFlowProducer
import com.tunjid.mutator.coroutines.mapToMutation
import com.tunjid.mutator.coroutines.toMutationStream
import com.tunjid.mutator.mutation
import com.tunjid.tiler.TiledList
import com.tunjid.tiler.distinctBy
import com.tunjid.tiler.emptyTiledList
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

data class PlaylistDetailState(
    val playlistName: String,
    val imageUrlString: String,
    val ownerName: String,
    val totalNumberOfTracks: String,
    val currentQuery: PlaylistQuery,
    val currentlyPlayingTrack: SearchResult.TrackSearchResult? = null,
    val tracks: TiledList<PlaylistQuery, SearchResult.TrackSearchResult> = emptyTiledList()
)

sealed class PlaylistDetailAction {
    data class LoadAround(val query: PlaylistQuery) : PlaylistDetailAction()
}

@HiltViewModel
class PlaylistDetailViewModel @Inject constructor(
    application: Application,
    savedStateHandle: SavedStateHandle,
    tracksRepository: TracksRepository,
    getCurrentlyPlayingTrackUseCase: GetCurrentlyPlayingTrackUseCase,
) : AndroidViewModel(application) {
    private val playlistId =
        savedStateHandle.get<String>(MusifyNavigationDestinations.PlaylistDetailScreen.NAV_ARG_PLAYLIST_ID)!!

    private val stateProducer =
        viewModelScope.actionStateFlowProducer<PlaylistDetailAction, PlaylistDetailState>(
            initialState = PlaylistDetailState(
                playlistName =
                savedStateHandle[MusifyNavigationDestinations.PlaylistDetailScreen.NAV_ARG_PLAYLIST_NAME]!!,
                imageUrlString =
                savedStateHandle[MusifyNavigationDestinations.PlaylistDetailScreen.NAV_ARG_ENCODED_IMAGE_URL_STRING]!!,
                ownerName =
                savedStateHandle[MusifyNavigationDestinations.PlaylistDetailScreen.NAV_ARG_OWNER_NAME]!!,
                totalNumberOfTracks =
                savedStateHandle[MusifyNavigationDestinations.PlaylistDetailScreen.NAV_ARG_NUMBER_OF_TRACKS]!!,
                currentQuery = PlaylistQuery(
                    id = playlistId,
                    countryCode = getCountryCode(),
                    page = Page(offset = 0)
                )
            ),
            mutationFlows = listOf(
                getCurrentlyPlayingTrackUseCase.playingTrackMutations(),
            ),
            actionTransform = { actions ->
                actions.toMutationStream {
                    when (val action = type()) {
                        is PlaylistDetailAction.LoadAround -> action.flow.trackListMutations(
                            tracksRepository = tracksRepository
                        )
                    }
                }
            }
        )

    val state = stateProducer.state
    val actions = stateProducer.accept
}

private fun GetCurrentlyPlayingTrackUseCase.playingTrackMutations(): Flow<Mutation<PlaylistDetailState>> =
    currentlyPlayingTrackStream.map {
        mutation { copy(currentlyPlayingTrack = it) }
    }

context(SuspendingStateHolder<PlaylistDetailState>)
private suspend fun Flow<PlaylistDetailAction.LoadAround>.trackListMutations(
    tracksRepository: TracksRepository
): Flow<Mutation<PlaylistDetailState>> =
    map { it.query }
        .toTiledList(
            startQuery = state().currentQuery,
            queryFor = { copy(page = it) },
            fetcher = tracksRepository::playListsFor
        )
        .mapToMutation {
            copy(tracks = it.distinctBy(SearchResult.TrackSearchResult::id))
        }
