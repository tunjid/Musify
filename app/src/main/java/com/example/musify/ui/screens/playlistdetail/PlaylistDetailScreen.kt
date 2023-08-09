package com.example.musify.ui.screens.playlistdetail

import androidx.annotation.DrawableRes
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.ContentAlpha
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.LocalTextStyle
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.example.musify.data.repositories.tracksrepository.PlaylistQuery
import com.example.musify.domain.SearchResult
import com.example.musify.ui.components.DetailScreenTopAppBar
import com.example.musify.ui.components.HeaderImageSource
import com.example.musify.ui.components.ImageHeaderWithMetadata
import com.example.musify.ui.components.MusifyBottomNavigationConstants
import com.example.musify.ui.components.MusifyCompactLoadingTrackCard
import com.example.musify.ui.components.MusifyCompactTrackCard
import com.example.musify.ui.components.MusifyMiniPlayerConstants
import com.example.musify.ui.components.collapsingheader.CollapsingHeader
import com.example.musify.ui.components.detailCollapsingHeaderState
import com.example.musify.ui.components.detailTopAppBarGradient
import com.example.musify.ui.components.scrollbar.DraggableScrollbar
import com.example.musify.ui.components.scrollbar.rememberTiledDraggableScroller
import com.example.musify.ui.components.scrollbar.tiledListScrollbarState
import com.example.musify.ui.components.toNormalizedHeaderProgress
import com.example.musify.ui.dynamicTheme.dynamicbackgroundmodifier.DynamicBackgroundResource
import com.example.musify.ui.dynamicTheme.dynamicbackgroundmodifier.backgroundColor
import com.example.musify.ui.dynamicTheme.dynamicbackgroundmodifier.gradientBackground
import com.tunjid.tiler.TiledList
import com.tunjid.tiler.compose.PivotedTilingEffect
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@ExperimentalMaterialApi
@Composable
fun PlaylistDetailScreen(
    playlistName: String,
    playlistImageUrlString: String?,
    nameOfPlaylistOwner: String,
    totalNumberOfTracks: String,
    showOffline: Boolean,
    @DrawableRes imageResToUseWhenImageUrlStringIsNull: Int,
    items: TiledList<PlaylistQuery, PlayListItem>,
    currentlyPlayingTrack: SearchResult.TrackSearchResult?,
    onQueryChanged: (PlaylistQuery?) -> Unit,
    onBackButtonClicked: () -> Unit,
    onTrackClicked: (SearchResult.TrackSearchResult) -> Unit,
) {
    var isLoadingPlaceholderForAlbumArtVisible by remember { mutableStateOf(false) }
    val lazyListState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    val dynamicBackgroundResource = remember {
        if (playlistImageUrlString == null) DynamicBackgroundResource.Empty
        else DynamicBackgroundResource.FromImageUrl(playlistImageUrlString)
    }
    val dynamicBackgroundColor by dynamicBackgroundResource.backgroundColor()
    val collapsingHeaderState = detailCollapsingHeaderState()

    Box(modifier = Modifier.fillMaxSize()) {
        CollapsingHeader(
            state = collapsingHeaderState,
            headerContent = {
                HeaderWithImageItem(
                    dynamicBackgroundColor = dynamicBackgroundColor,
                    playlistName = playlistName,
                    playlistImageUrlString = playlistImageUrlString,
                    headerTranslation = collapsingHeaderState.translation,
                    translationProgress = collapsingHeaderState.progress,
                    imageResToUseWhenImageUrlStringIsNull = imageResToUseWhenImageUrlStringIsNull,
                    nameOfPlaylistOwner = nameOfPlaylistOwner,
                    totalNumberOfTracks = totalNumberOfTracks,
                    isLoadingPlaceholderForAlbumArtVisible = isLoadingPlaceholderForAlbumArtVisible,
                    onImageLoading = { isLoadingPlaceholderForAlbumArtVisible = true },
                    onImageLoaded = { isLoadingPlaceholderForAlbumArtVisible = false },
                )
            },
            body = {
                TrackList(
                    lazyListState = lazyListState,
                    showOffline = showOffline,
                    items = items,
                    onTrackClicked = onTrackClicked,
                    currentlyPlayingTrack = currentlyPlayingTrack,
                    totalNumberOfTracks = totalNumberOfTracks,
                    onQueryChanged = onQueryChanged
                )
            }
        )
        DetailScreenTopAppBar(
            modifier = Modifier
                .detailTopAppBarGradient(
                    startColor = dynamicBackgroundColor,
                    endColor = MaterialTheme.colors.surface,
                    progress = collapsingHeaderState.progress.toNormalizedHeaderProgress()
                )
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .statusBarsPadding(),
            title = playlistName,
            contentAlpha = collapsingHeaderState.progress.toNormalizedHeaderProgress(),
            onBackButtonClicked = onBackButtonClicked,
            onClick = {
                coroutineScope.launch { lazyListState.animateScrollToItem(0) }
            }
        )
        lazyListState.PivotedTilingEffect(
            items = items,
            onQueryChanged = onQueryChanged
        )
    }
}

@ExperimentalMaterialApi
@Composable
private fun TrackList(
    lazyListState: LazyListState,
    showOffline: Boolean,
    items: TiledList<PlaylistQuery, PlayListItem>,
    onTrackClicked: (SearchResult.TrackSearchResult) -> Unit,
    currentlyPlayingTrack: SearchResult.TrackSearchResult?,
    totalNumberOfTracks: String,
    onQueryChanged: (PlaylistQuery?) -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                bottom = MusifyBottomNavigationConstants.navigationHeight + MusifyMiniPlayerConstants.miniPlayerHeight
            ),
            state = lazyListState
        ) {
            // if error message visible
            when {
                showOffline -> item {
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

                else -> items(
                    items = items,
                    key = PlayListItem::pagedIndex
                ) { playListItem ->
                    when (playListItem) {
                        is PlayListItem.Loaded -> MusifyCompactTrackCard(
                            track = playListItem.trackSearchResult,
                            onClick = onTrackClicked,
                            isCurrentlyPlaying = playListItem.trackSearchResult == currentlyPlayingTrack,
                            isAlbumArtVisible = true,
                            subtitleTextStyle = LocalTextStyle.current.copy(
                                fontWeight = FontWeight.Thin,
                                color = MaterialTheme.colors.onBackground.copy(alpha = ContentAlpha.disabled),
                            ),
                            contentPadding = PaddingValues(16.dp)
                        )

                        is PlayListItem.Placeholder -> MusifyCompactLoadingTrackCard()
                    }
                }
            }
            item {
                Spacer(
                    modifier = Modifier
                        .windowInsetsBottomHeight(WindowInsets.navigationBars)
                        .padding(bottom = 16.dp)
                )
            }
        }
        val tracksCount = totalNumberOfTracks.toIntOrNull() ?: 0
        val scrollbarState = lazyListState.tiledListScrollbarState(
            itemsAvailable = tracksCount,
            tiledItems = items
        )
        lazyListState.DraggableScrollbar(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(bottom = 56.dp),
            state = scrollbarState,
            orientation = Orientation.Vertical,
            onThumbMoved = lazyListState.rememberTiledDraggableScroller(
                itemsAvailable = tracksCount,
                tiledItems = items,
                onQueryChanged = onQueryChanged,
            )
        )
    }
}

@Composable
private fun HeaderWithImageItem(
    dynamicBackgroundColor: Color,
    playlistName: String,
    playlistImageUrlString: String?,
    headerTranslation: Float,
    translationProgress: Float,
    @DrawableRes imageResToUseWhenImageUrlStringIsNull: Int,
    nameOfPlaylistOwner: String,
    totalNumberOfTracks: String,
    isLoadingPlaceholderForAlbumArtVisible: Boolean,
    onImageLoading: () -> Unit,
    onImageLoaded: (Throwable?) -> Unit
) {
    ImageHeaderWithMetadata(
        modifier = Modifier
            .offset { IntOffset(x = 0, y = -headerTranslation.roundToInt()) }
            .gradientBackground(
                startColor = dynamicBackgroundColor,
                endColor = MaterialTheme.colors.surface
            )
            .statusBarsPadding(),
        title = playlistName,
        headerImageSource = if (playlistImageUrlString == null)
            HeaderImageSource.ImageFromDrawableResource(
                resourceId = imageResToUseWhenImageUrlStringIsNull
            )
        else HeaderImageSource.ImageFromUrlString(playlistImageUrlString),
        headerTranslation = headerTranslation,
        translationProgress = translationProgress,
        subtitle = "by $nameOfPlaylistOwner • $totalNumberOfTracks tracks",
        isLoadingPlaceholderVisible = isLoadingPlaceholderForAlbumArtVisible,
        onImageLoading = onImageLoading,
        onImageLoaded = onImageLoaded,
        additionalMetadataContent = {
            Spacer(modifier = Modifier.size(16.dp))
        }
    )
}
