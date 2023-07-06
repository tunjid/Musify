package com.example.musify.ui.screens.detailscreens

import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.ContentAlpha
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.FloatingActionButton
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.musify.data.repositories.albumsrepository.ArtistAlbumsQuery
import com.example.musify.domain.SearchResult
import com.example.musify.ui.components.AsyncImageWithPlaceholder
import com.example.musify.ui.components.DefaultMusifyLoadingAnimation
import com.example.musify.ui.components.DetailScreenTopAppBar
import com.example.musify.ui.components.ListItemCardType
import com.example.musify.ui.components.MusifyBottomNavigationConstants
import com.example.musify.ui.components.MusifyCompactListItemCard
import com.example.musify.ui.components.MusifyCompactTrackCard
import com.example.musify.ui.components.MusifyMiniPlayerConstants
import com.example.musify.ui.dynamicTheme.dynamicbackgroundmodifier.DynamicBackgroundResource
import com.tunjid.tiler.TiledList
import com.tunjid.tiler.compose.PivotedTilingEffect
import kotlinx.coroutines.launch

@ExperimentalMaterialApi
@Composable
fun ArtistDetailScreen(
    artistName: String,
    artistImageUrlString: String?,
    popularTracks: List<SearchResult.TrackSearchResult>,
    releases: TiledList<ArtistAlbumsQuery, SearchResult.AlbumSearchResult>,
    currentlyPlayingTrack: SearchResult.TrackSearchResult?,
    onQueryChanged: (ArtistAlbumsQuery?) -> Unit,
    onBackButtonClicked: () -> Unit,
    onPlayButtonClicked: () -> Unit,
    onTrackClicked: (SearchResult.TrackSearchResult) -> Unit,
    onAlbumClicked: (SearchResult.AlbumSearchResult) -> Unit,
    isLoading: Boolean,
    @DrawableRes fallbackImageRes: Int,
    isErrorMessageVisible: Boolean
) {
    val subtitleTextColorWithAlpha = MaterialTheme.colors.onBackground.copy(
        alpha = ContentAlpha.disabled
    )
    var isCoverArtPlaceholderVisible by remember { mutableStateOf(false) }
    val lazyListState = rememberLazyListState()
    val fallbackImagePainter =
        rememberVectorPainter(ImageVector.vectorResource(id = fallbackImageRes))
    val isAppBarVisible by remember {
        derivedStateOf { lazyListState.firstVisibleItemIndex > 0 }
    }
    val dynamicBackgroundResource = remember {
        if (artistImageUrlString == null) DynamicBackgroundResource.Empty
        else DynamicBackgroundResource.FromImageUrl(artistImageUrlString)
    }
    val coroutineScope = rememberCoroutineScope()
    Box {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            state = lazyListState,
            contentPadding = PaddingValues(
                bottom = MusifyBottomNavigationConstants.navigationHeight + MusifyMiniPlayerConstants.miniPlayerHeight
            )
        ) {
            artistCoverArtHeaderItem(
                artistName = artistName,
                artistCoverArtUrlString = artistImageUrlString,
                onBackButtonClicked = onBackButtonClicked,
                onPLayButtonClick = onPlayButtonClicked,
                isLoadingPlaceholderVisible = isCoverArtPlaceholderVisible,
                onCoverArtLoading = { isCoverArtPlaceholderVisible = true },
                onCoverArtLoaded = { isCoverArtPlaceholderVisible = false },
                fallbackImagePainter = fallbackImagePainter
            )
            item {
                SubtitleText(
                    modifier = Modifier.padding(all = 16.dp),
                    text = "Popular tracks"
                )
            }
            itemsIndexed(popularTracks) { index, track ->
                Row(
                    modifier = Modifier.height(64.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        modifier = if (index + 1 < 10) Modifier.padding(16.dp)
                        else Modifier.padding(
                            top = 16.dp,
                            bottom = 16.dp,
                            start = 16.dp,
                            end = 8.dp
                        ),
                        text = "${index + 1}"
                    )
                    MusifyCompactTrackCard(
                        track = track,
                        subtitleTextStyle = MaterialTheme.typography
                            .caption
                            .copy(color = subtitleTextColorWithAlpha),
                        onClick = onTrackClicked,
                        isCurrentlyPlaying = track == currentlyPlayingTrack
                    )
                }
            }
            item {
                SubtitleText(
                    modifier = Modifier.padding(start = 16.dp),
                    text = "Releases"
                )
            }
            items(
                items = releases,
                key = SearchResult.AlbumSearchResult::id
            ) { album ->
                MusifyCompactListItemCard(
                    modifier = Modifier
                        .height(80.dp)
                        .padding(horizontal = 16.dp),
                    cardType = ListItemCardType.ALBUM,
                    thumbnailImageUrlString = album.albumArtUrlString,
                    title = album.name,
                    titleTextStyle = MaterialTheme.typography.h6,
                    subtitle = album.yearOfReleaseString,
                    subtitleTextStyle = MaterialTheme.typography
                        .subtitle2
                        .copy(color = subtitleTextColorWithAlpha),
                    onClick = { onAlbumClicked(album) },
                    onTrailingButtonIconClick = { onAlbumClicked(album) }
                )
            }
            item {
                Spacer(modifier = Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
            }
            if (isErrorMessageVisible) {
                item {
                    Column(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Oops! Something doesn't look right",
                            style = MaterialTheme.typography.h6,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Please check the internet connection",
                            style = MaterialTheme.typography.subtitle2
                        )
                    }
                }
            }
        }
        DefaultMusifyLoadingAnimation(
            modifier = Modifier.align(Alignment.Center),
            isVisible = isLoading
        )
        AnimatedVisibility(
            visible = isAppBarVisible,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            DetailScreenTopAppBar(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .statusBarsPadding(),
                title = artistName,
                onBackButtonClicked = onBackButtonClicked,
                dynamicBackgroundResource = dynamicBackgroundResource,
                onClick = {
                    coroutineScope.launch { lazyListState.animateScrollToItem(0) }
                }
            )
        }
        lazyListState.PivotedTilingEffect(
            items = releases,
            onQueryChanged = onQueryChanged
        )
    }
}

@Composable
private fun SubtitleText(modifier: Modifier = Modifier, text: String) {
    Text(
        modifier = modifier,
        text = text,
        style = MaterialTheme.typography.h5,
        fontWeight = FontWeight.Bold
    )
}

private fun LazyListScope.artistCoverArtHeaderItem(
    artistName: String,
    artistCoverArtUrlString: String?,
    fallbackImagePainter: Painter,
    onBackButtonClicked: () -> Unit,
    onPLayButtonClick: () -> Unit,
    isLoadingPlaceholderVisible: Boolean = false,
    onCoverArtLoading: (() -> Unit)? = null,
    onCoverArtLoaded: ((Throwable?) -> Unit)? = null,
) {
    item {
        Box(
            modifier = Modifier
                .fillParentMaxHeight(0.6f)
                .fillParentMaxWidth()
        ) {
            if (artistCoverArtUrlString != null) {
                AsyncImageWithPlaceholder(
                    modifier = Modifier.fillMaxSize(),
                    model = artistCoverArtUrlString,
                    contentScale = ContentScale.Crop,
                    contentDescription = null,
                    isLoadingPlaceholderVisible = isLoadingPlaceholderVisible,
                    onImageLoading = { onCoverArtLoading?.invoke() },
                    onImageLoadingFinished = { onCoverArtLoaded?.invoke(it) }
                )
            } else {
                Image(
                    modifier = Modifier.fillMaxSize(),
                    painter = fallbackImagePainter,
                    contentDescription = null
                )
            }

            // scrim
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.2f))
            )
            IconButton(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .statusBarsPadding()
                    .padding(16.dp)
                    .size(50.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colors.background.copy(alpha = 0.7f)),
                onClick = onBackButtonClicked
            ) {
                Icon(
                    imageVector = Icons.Filled.ArrowBack,
                    contentDescription = null
                )
            }
            Text(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(16.dp),
                text = artistName,
                style = MaterialTheme.typography.h3,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            FloatingActionButton(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 16.dp)
                    .offset(y = 24.dp),
                backgroundColor = MaterialTheme.colors.primary,
                onClick = onPLayButtonClick
            ) {
                Icon(
                    modifier = Modifier.size(50.dp),
                    imageVector = Icons.Filled.PlayArrow,
                    contentDescription = null
                )
            }
        }
    }
}