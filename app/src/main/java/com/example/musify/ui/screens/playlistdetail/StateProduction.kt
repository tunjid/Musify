package com.example.musify.ui.screens.playlistdetail

import com.example.musify.data.repositories.tracksrepository.PlaylistQuery
import com.example.musify.data.repositories.tracksrepository.TracksRepository
import com.example.musify.data.tiling.Page
import com.example.musify.data.tiling.PagedItem
import com.example.musify.data.tiling.toNetworkBackedTiledList
import com.example.musify.data.tiling.withPlaceholders
import com.example.musify.data.utils.NetworkMonitor
import com.example.musify.domain.SearchResult
import com.example.musify.musicplayer.MusicPlaybackMonitor
import com.example.musify.musicplayer.currentlyPlayingTrackStream
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

sealed class PlaylistDetailAction {
    data class LoadAround(val query: PlaylistQuery?) : PlaylistDetailAction()
}

data class PlaylistDetailUiState(
    val playlistName: String,
    val imageUrlString: String,
    val ownerName: String,
    val totalNumberOfTracks: String,
    val currentQuery: PlaylistQuery,
    val isOnline: Boolean = true,
    val currentlyPlayingTrack: SearchResult.TrackSearchResult? = null,
    val items: TiledList<PlaylistQuery, PlayListItem> = emptyTiledList()
)

val PlaylistDetailUiState.showOffline: Boolean get() = !isOnline && items.isEmpty()

sealed interface PlayListItem : PagedItem {
    data class Loaded(
        override val pagedIndex: Int,
        val trackSearchResult: SearchResult.TrackSearchResult
    ) : PlayListItem

    data class Placeholder(
        override val pagedIndex: Int,
    ) : PlayListItem
}

private val PlayListItem.internalKey
    get() = when (this) {
        is PlayListItem.Loaded -> trackSearchResult.id
        is PlayListItem.Placeholder -> pagedIndex
    }

fun CoroutineScope.playlistDetailStateProducer(
    playlistId: String,
    playlistName: String,
    imageUrlString: String,
    ownerName: String,
    totalNumberOfTracks: String,
    countryCode: String,
    networkMonitor: NetworkMonitor,
    musicPlaybackMonitor: MusicPlaybackMonitor,
    tracksRepository: TracksRepository,
) = actionStateFlowProducer<PlaylistDetailAction, PlaylistDetailUiState>(
    initialState = PlaylistDetailUiState(
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
        networkMonitor.isOnlineMutations(),
        musicPlaybackMonitor.playingTrackMutations(),
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

private fun NetworkMonitor.isOnlineMutations(): Flow<Mutation<PlaylistDetailUiState>> =
    isOnline.mapToMutation {
        copy(isOnline = it)
    }

private fun MusicPlaybackMonitor.playingTrackMutations(): Flow<Mutation<PlaylistDetailUiState>> =
    currentlyPlayingTrackStream.mapToMutation {
        copy(currentlyPlayingTrack = it)
    }

context(SuspendingStateHolder<PlaylistDetailUiState>)
private suspend fun Flow<PlaylistDetailAction.LoadAround>.trackListMutations(
    tracksRepository: TracksRepository
): Flow<Mutation<PlaylistDetailUiState>> =
    map { it.query ?: state().currentQuery }
        .toNetworkBackedTiledList(
            startQuery = state().currentQuery,
            fetcher = tracksRepository::playListsFor.withPlaceholders(
                placeholderMapper = PlayListItem::Placeholder,
                loadedMapper = PlayListItem::Loaded
            )
        )
        .mapToMutation {
            copy(items = it.distinctBy(PlayListItem::internalKey))
        }
