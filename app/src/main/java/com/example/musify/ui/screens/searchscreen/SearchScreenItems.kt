package com.example.musify.ui.screens.searchscreen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.musify.data.repositories.searchrepository.ContentQuery
import com.example.musify.domain.SearchResult
import com.example.musify.ui.components.DefaultMusifyErrorMessage
import com.example.musify.ui.components.EpisodeListCard
import com.example.musify.ui.components.ListItemCardType
import com.example.musify.ui.components.MusifyBottomNavigationConstants
import com.example.musify.ui.components.MusifyCompactListItemCard
import com.example.musify.ui.components.MusifyCompactTrackCard
import com.example.musify.ui.components.MusifyCompactTrackCardDefaults
import com.example.musify.ui.components.MusifyMiniPlayerConstants
import com.example.musify.ui.components.PodcastCard
import com.tunjid.tiler.TiledList
import com.tunjid.tiler.compose.PivotedTilingEffect
import kotlinx.coroutines.flow.StateFlow

/**
 * A color that is meant to be applied to all types of search items.
 * [Color.Transparent] is specified as the background color for the
 * cards. Since the search screen's background uses a dynamic color based
 * on the currently playing track, the background color of the cards
 * needs to be transparent in order for the dynamic color to be
 * visible.
 */
private val CardBackgroundColor @Composable get() = Color.Transparent
private val CardShape = RectangleShape

@ExperimentalMaterialApi
@Composable
fun SearchTrackListItems(
    isOnline: Boolean,
    currentlyPlayingTrack: SearchResult.TrackSearchResult?,
    tracksListForSearchQuery: StateFlow<TiledList<ContentQuery, SearchResult.TrackSearchResult>>,
    onQueryChanged: (ContentQuery) -> Unit,
    onItemClick: (SearchResult) -> Unit,
) = TiledLazyColumn(
    isOnline = isOnline,
    itemsFlow = tracksListForSearchQuery,
    onQueryChanged = onQueryChanged
) { items ->
    itemsIndexedWithEmptyListContent(
        items = items,
        cardType = ListItemCardType.TRACK,
        key = { index, track -> "$index${track.id}" }
    ) { _, track ->
        MusifyCompactTrackCard(
            backgroundColor = CardBackgroundColor,
            shape = CardShape,
            track = track,
            onClick = onItemClick,
            isCurrentlyPlaying = track == currentlyPlayingTrack
        )
    }
}

@ExperimentalMaterialApi
@Composable
fun SearchAlbumListItems(
    isOnline: Boolean,
    albumListForSearchQuery: StateFlow<TiledList<ContentQuery, SearchResult.AlbumSearchResult>>,
    onQueryChanged: (ContentQuery) -> Unit,
    onItemClick: (SearchResult) -> Unit,
) = TiledLazyColumn(
    isOnline = isOnline,
    itemsFlow = albumListForSearchQuery,
    onQueryChanged = onQueryChanged
) { items ->
    itemsIndexedWithEmptyListContent(
        items = items,
        cardType = ListItemCardType.ALBUM,
        key = { index, album -> "$index${album.id}" }
    ) { _, album ->
        MusifyCompactListItemCard(
            backgroundColor = CardBackgroundColor,
            shape = CardShape,
            cardType = ListItemCardType.ALBUM,
            thumbnailImageUrlString = album.albumArtUrlString,
            title = album.name,
            subtitle = album.artistsString,
            onClick = { onItemClick(album) },
            onTrailingButtonIconClick = { onItemClick(album) },
            contentPadding = MusifyCompactTrackCardDefaults.defaultContentPadding
        )
    }
}

@ExperimentalMaterialApi
@Composable
fun SearchArtistListItems(
    isOnline: Boolean,
    artistListForSearchQuery: StateFlow<TiledList<ContentQuery, SearchResult.ArtistSearchResult>>,
    onItemClick: (SearchResult) -> Unit,
    onQueryChanged: (ContentQuery) -> Unit,
    artistImageErrorPainter: Painter
) = TiledLazyColumn(
    isOnline = isOnline,
    itemsFlow = artistListForSearchQuery,
    onQueryChanged = onQueryChanged
) { items ->
    itemsIndexedWithEmptyListContent(
        items = items,
        cardType = ListItemCardType.PLAYLIST,
        key = { index, artist -> "$index${artist.id}" }
    ) { _, artist ->
        MusifyCompactListItemCard(
            backgroundColor = CardBackgroundColor,
            shape = CardShape,
            cardType = ListItemCardType.ARTIST,
            thumbnailImageUrlString = artist.imageUrlString ?: "",
            title = artist.name,
            subtitle = "Artist",
            onClick = { onItemClick(artist) },
            onTrailingButtonIconClick = { onItemClick(artist) },
            errorPainter = artistImageErrorPainter,
            contentPadding = MusifyCompactTrackCardDefaults.defaultContentPadding
        )
    }
}

@ExperimentalMaterialApi
@Composable
fun SearchPlaylistListItems(
    isOnline: Boolean,
    playlistListForSearchQuery: StateFlow<TiledList<ContentQuery, SearchResult.PlaylistSearchResult>>,
    onQueryChanged: (ContentQuery) -> Unit,
    onItemClick: (SearchResult) -> Unit,
    playlistImageErrorPainter: Painter
) = TiledLazyColumn(
    isOnline = isOnline,
    itemsFlow = playlistListForSearchQuery,
    onQueryChanged = onQueryChanged
) { items ->
    itemsIndexedWithEmptyListContent(
        items = items,
        cardType = ListItemCardType.PLAYLIST,
        key = { index, playlist -> "$index${playlist.id}" }
    ) { _, playlist ->
        MusifyCompactListItemCard(
            backgroundColor = CardBackgroundColor,
            shape = CardShape,
            cardType = ListItemCardType.PLAYLIST,
            thumbnailImageUrlString = playlist.imageUrlString ?: "",
            title = playlist.name,
            subtitle = "Playlist",
            onClick = { onItemClick(playlist) },
            onTrailingButtonIconClick = { onItemClick(playlist) },
            errorPainter = playlistImageErrorPainter,
            contentPadding = MusifyCompactTrackCardDefaults.defaultContentPadding
        )
    }
}

@ExperimentalMaterialApi
@Composable
fun SearchPodcastListItems(
    isOnline: Boolean,
    podcastsForSearchQuery: StateFlow<TiledList<ContentQuery, SearchResult.PodcastSearchResult>>,
    episodesForSearchQuery: StateFlow<TiledList<ContentQuery, SearchResult.EpisodeSearchResult>>,
    onQueryChanged: (ContentQuery) -> Unit,
    onPodcastItemClicked: (SearchResult.PodcastSearchResult) -> Unit,
    onEpisodeItemClicked: (SearchResult.EpisodeSearchResult) -> Unit
) {
    val podcasts by podcastsForSearchQuery.collectAsState()
    val rowLazyState = rememberLazyListState()
    rowLazyState.PivotedTilingEffect(
        items = podcasts,
        onQueryChanged = { if (it != null) onQueryChanged(it) }
    )
    TiledLazyColumn(
        isOnline = isOnline,
        itemsFlow = episodesForSearchQuery,
        onQueryChanged = onQueryChanged
    ) { items ->
        item {
            Text(
                modifier = Modifier.padding(
                    horizontal = 16.dp,
                    vertical = 8.dp
                ),
                text = "Podcasts & Shows",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.h6
            )
            LazyRow(
                modifier = Modifier
                    .fillParentMaxWidth()
                    .height(238.dp),
                state = rowLazyState,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(horizontal = 16.dp)
            ) {
                items(
                    items = podcasts
                ) { podcast ->
                    PodcastCard(
                        podcastArtUrlString = podcast.imageUrlString,
                        name = podcast.name,
                        nameOfPublisher = podcast.nameOfPublisher,
                        onClick = { onPodcastItemClicked(podcast) }
                    )
                }
            }
        }
        itemsIndexedWithEmptyListContent(items) { _, episode ->
            EpisodeListCard(
                episodeSearchResult = episode,
                onClick = { onEpisodeItemClicked(episode) }
            )
        }
    }
}

private fun <T : Any> LazyListScope.itemsIndexedWithEmptyListContent(
    items: TiledList<ContentQuery, T>,
    cardType: ListItemCardType? = null,
    key: ((index: Int, item: T) -> Any)? = null,
    emptyListContent: @Composable LazyItemScope.() -> Unit = {
        val title = remember(cardType) {
            "Couldn't find " +
                    "${
                        when (cardType) {
                            ListItemCardType.ALBUM -> "any albums"
                            ListItemCardType.ARTIST -> "any artists"
                            ListItemCardType.TRACK -> "any tracks"
                            ListItemCardType.PLAYLIST -> "any playlists"
                            null -> "anything"
                        }
                    } matching the search query."
        }
        DefaultMusifyErrorMessage(
            title = title,
            subtitle = "Try searching again using a different spelling or keyword.",
            modifier = Modifier
                .fillParentMaxSize()
                .padding(horizontal = 16.dp)
                .windowInsetsPadding(WindowInsets.ime)
        )
    },
    itemContent: @Composable LazyItemScope.(index: Int, value: T) -> Unit
) {
    itemsIndexed(
        items = items,
        key = key,
        itemContent = itemContent
    )
//    if (items.isEmpty()) {
//        item { emptyListContent.invoke(this) }
//    } else {
//        itemsIndexed(
//            items = items,
//            key = key,
//            itemContent = itemContent
//        )
//    }
}

@Composable
private fun <Item> TiledLazyColumn(
    isOnline: Boolean,
    itemsFlow: StateFlow<TiledList<ContentQuery, Item>>,
    onQueryChanged: (ContentQuery) -> Unit,
    content: LazyListScope.(TiledList<ContentQuery, Item>) -> Unit
) {
    val lazyListState = rememberLazyListState()
    val items by itemsFlow.collectAsState()
    Box(modifier = Modifier.fillMaxSize()) {
        if (items.isEmpty() && !isOnline) DefaultMusifyErrorMessage(
            title = "Oops! Something doesn't look right",
            subtitle = "Please check the internet connection",
            modifier = Modifier
                .align(Alignment.Center)
                .imePadding(),
            onRetryButtonClicked = { }
        )
        else LazyColumn(
            modifier = Modifier
                .background(MaterialTheme.colors.background.copy(alpha = 0.7f))
                .fillMaxSize(),
            state = lazyListState,
            contentPadding = PaddingValues(
                bottom = MusifyBottomNavigationConstants.navigationHeight + MusifyMiniPlayerConstants.miniPlayerHeight
            ),
        ) {
            content(items)
            item {
                Spacer(modifier = Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
            }
        }
    }
    lazyListState.PivotedTilingEffect(
        items = items,
        onQueryChanged = { query ->
            if (query != null) onQueryChanged(query)
        }
    )
}