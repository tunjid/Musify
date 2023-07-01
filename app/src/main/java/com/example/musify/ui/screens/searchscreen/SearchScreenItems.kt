package com.example.musify.ui.screens.searchscreen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
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
import com.example.musify.ui.components.MusifyCompactListItemCard
import com.example.musify.ui.components.MusifyCompactTrackCard
import com.example.musify.ui.components.MusifyCompactTrackCardDefaults
import com.example.musify.ui.components.PodcastCard
import com.tunjid.tiler.TiledList

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
fun LazyListScope.searchTrackListItems(
    tracksListForSearchQuery: TiledList<ContentQuery, SearchResult.TrackSearchResult>,
    currentlyPlayingTrack: SearchResult.TrackSearchResult?,
    onItemClick: (SearchResult) -> Unit,
) {
    itemsIndexedWithEmptyListContent(
        items = tracksListForSearchQuery,
        cardType = ListItemCardType.TRACK,
        key = { index, track -> "$index${track.id}" }
    ) { _, track ->
        track?.let {
            MusifyCompactTrackCard(
                backgroundColor = CardBackgroundColor,
                shape = CardShape,
                track = it,
                onClick = onItemClick,
                isCurrentlyPlaying = it == currentlyPlayingTrack
            )
        }
    }
}

@ExperimentalMaterialApi
fun LazyListScope.searchAlbumListItems(
    albumListForSearchQuery: TiledList<ContentQuery, SearchResult.AlbumSearchResult>,
    onItemClick: (SearchResult) -> Unit,
) {

    println("There are ${albumListForSearchQuery.size} albums")
    itemsIndexedWithEmptyListContent(
        items = albumListForSearchQuery,
        cardType = ListItemCardType.ALBUM,
        key = { index, album -> "$index${album.id}" }
    ) { _, album ->
        album?.let {
            MusifyCompactListItemCard(
                backgroundColor = CardBackgroundColor,
                shape = CardShape,
                cardType = ListItemCardType.ALBUM,
                thumbnailImageUrlString = it.albumArtUrlString,
                title = it.name,
                subtitle = it.artistsString,
                onClick = { onItemClick(it) },
                onTrailingButtonIconClick = { onItemClick(it) },
                contentPadding = MusifyCompactTrackCardDefaults.defaultContentPadding
            )
        }
    }
}

@ExperimentalMaterialApi
fun LazyListScope.searchArtistListItems(
    artistListForSearchQuery: TiledList<ContentQuery, SearchResult.ArtistSearchResult>,
    onItemClick: (SearchResult) -> Unit,
    artistImageErrorPainter: Painter
) {
    itemsIndexedWithEmptyListContent(
        items = artistListForSearchQuery,
        cardType = ListItemCardType.PLAYLIST,
        key = { index, artist -> "$index${artist.id}" }
    ) { _, artist ->
        artist?.let {
            MusifyCompactListItemCard(
                backgroundColor = CardBackgroundColor,
                shape = CardShape,
                cardType = ListItemCardType.ARTIST,
                thumbnailImageUrlString = it.imageUrlString ?: "",
                title = it.name,
                subtitle = "Artist",
                onClick = { onItemClick(it) },
                onTrailingButtonIconClick = { onItemClick(it) },
                errorPainter = artistImageErrorPainter,
                contentPadding = MusifyCompactTrackCardDefaults.defaultContentPadding
            )
        }
    }
}

@ExperimentalMaterialApi
fun LazyListScope.searchPlaylistListItems(
    playlistListForSearchQuery: TiledList<ContentQuery, SearchResult.PlaylistSearchResult>,
    onItemClick: (SearchResult) -> Unit,
    playlistImageErrorPainter: Painter
) {
    itemsIndexedWithEmptyListContent(
        items = playlistListForSearchQuery,
        cardType = ListItemCardType.PLAYLIST,
        key = { index, playlist -> "$index${playlist.id}" }
    ) { _, playlist ->
        playlist?.let {
            MusifyCompactListItemCard(
                backgroundColor = CardBackgroundColor,
                shape = CardShape,
                cardType = ListItemCardType.PLAYLIST,
                thumbnailImageUrlString = it.imageUrlString ?: "",
                title = it.name,
                subtitle = "Playlist",
                onClick = { onItemClick(it) },
                onTrailingButtonIconClick = { onItemClick(it) },
                errorPainter = playlistImageErrorPainter,
                contentPadding = MusifyCompactTrackCardDefaults.defaultContentPadding
            )
        }
    }
}

@ExperimentalMaterialApi
fun LazyListScope.searchPodcastListItems(
    podcastsForSearchQuery: TiledList<ContentQuery, SearchResult.PodcastSearchResult>,
    episodesForSearchQuery: TiledList<ContentQuery, SearchResult.EpisodeSearchResult>,
    onPodcastItemClicked: (SearchResult.PodcastSearchResult) -> Unit,
    onEpisodeItemClicked: (SearchResult.EpisodeSearchResult) -> Unit
) {
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
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(horizontal = 16.dp)
        ) {
            items(
                items = podcastsForSearchQuery
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

    itemsIndexedWithEmptyListContent(episodesForSearchQuery) { _, episode ->
        episode?.let {
            EpisodeListCard(
                episodeSearchResult = it,
                onClick = { onEpisodeItemClicked(it) }
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
    itemContent: @Composable LazyItemScope.(index: Int, value: T?) -> Unit
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
