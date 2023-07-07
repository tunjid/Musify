package com.example.musify.ui.screens.playlistdetail

import com.example.musify.data.repositories.tracksrepository.PlaylistQuery
import com.example.musify.data.repositories.tracksrepository.TracksRepository
import com.example.musify.data.tiling.Page
import com.example.musify.data.tiling.toTiledList
import com.example.musify.domain.SearchResult
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

sealed class PlaylistDetailAction {
    data class LoadAround(val query: PlaylistQuery?) : PlaylistDetailAction()
}

data class PlaylistDetailState(
    val playlistName: String,
    val imageUrlString: String,
    val ownerName: String,
    val totalNumberOfTracks: String,
    val currentQuery: PlaylistQuery,
    val currentlyPlayingTrack: SearchResult.TrackSearchResult? = null,
    val tracks: TiledList<PlaylistQuery, SearchResult.TrackSearchResult> = emptyTiledList()
)

fun CoroutineScope.playlistDetailStateProducer(
    playlistId: String,
    playlistName: String,
    imageUrlString: String,
    ownerName: String,
    totalNumberOfTracks: String,
    countryCode: String,
    tracksRepository: TracksRepository,
    getCurrentlyPlayingTrackUseCase: GetCurrentlyPlayingTrackUseCase,
) = actionStateFlowProducer<PlaylistDetailAction, PlaylistDetailState>(
    initialState = PlaylistDetailState(
        playlistName = playlistName,
        imageUrlString = imageUrlString,
        ownerName = ownerName,
        totalNumberOfTracks = totalNumberOfTracks,
        currentQuery = PlaylistQuery(
            id = playlistId,
            countryCode = countryCode,
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

private fun GetCurrentlyPlayingTrackUseCase.playingTrackMutations(): Flow<Mutation<PlaylistDetailState>> =
    currentlyPlayingTrackStream.map {
        mutation { copy(currentlyPlayingTrack = it) }
    }

context(SuspendingStateHolder<PlaylistDetailState>)
private suspend fun Flow<PlaylistDetailAction.LoadAround>.trackListMutations(
    tracksRepository: TracksRepository
): Flow<Mutation<PlaylistDetailState>> =
    map { it.query ?: state().currentQuery }
        .toTiledList(
            startQuery = state().currentQuery,
            queryFor = { copy(page = it) },
            fetcher = tracksRepository::playListsFor
        )
        .mapToMutation {
            copy(tracks = it.distinctBy(com.example.musify.domain.SearchResult.TrackSearchResult::id))
        }
