package com.example.musify.ui.screens.albumdetail

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
import com.example.musify.domain.SearchResult
import com.example.musify.ui.components.DefaultMusifyLoadingAnimation
import com.example.musify.ui.components.DetailScreenTopAppBar
import com.example.musify.ui.components.HeaderImageSource
import com.example.musify.ui.components.ImageHeaderWithMetadata
import com.example.musify.ui.components.MusifyBottomNavigationConstants
import com.example.musify.ui.components.MusifyCompactTrackCard
import com.example.musify.ui.components.MusifyMiniPlayerConstants
import com.example.musify.ui.components.collapsingheader.CollapsingHeader
import com.example.musify.ui.components.detailCollapsingHeaderState
import com.example.musify.ui.components.detailTopAppBarGradient
import com.example.musify.ui.components.scrollbar.DraggableScrollbar
import com.example.musify.ui.components.scrollbar.rememberDraggableScroller
import com.example.musify.ui.components.scrollbar.scrollbarState
import com.example.musify.ui.components.toNormalizedHeaderProgress
import com.example.musify.ui.dynamicTheme.dynamicbackgroundmodifier.DynamicBackgroundResource
import com.example.musify.ui.dynamicTheme.dynamicbackgroundmodifier.backgroundColor
import com.example.musify.ui.dynamicTheme.dynamicbackgroundmodifier.gradientBackground
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@ExperimentalMaterialApi
@Composable
fun AlbumDetailScreen(
    albumName: String,
    artistsString: String,
    yearOfRelease: String,
    albumArtUrlString: String,
    trackList: List<SearchResult.TrackSearchResult>,
    onTrackItemClick: (SearchResult.TrackSearchResult) -> Unit,
    onBackButtonClicked: () -> Unit,
    isLoading: Boolean,
    isErrorMessageVisible: Boolean,
    currentlyPlayingTrack: SearchResult.TrackSearchResult?
) {
    var isLoadingPlaceholderForAlbumArtVisible by remember { mutableStateOf(false) }
    val lazyListState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    val dynamicBackgroundResource = remember {
        DynamicBackgroundResource.FromImageUrl(albumArtUrlString)
    }
    val dynamicBackgroundColor by dynamicBackgroundResource.backgroundColor()
    val collapsingHeaderState = detailCollapsingHeaderState()

    Box(modifier = Modifier.fillMaxSize()) {
        CollapsingHeader(
            state = collapsingHeaderState,
            headerContent = {
                HeaderWithImageItem(
                    dynamicBackgroundColor = dynamicBackgroundColor,
                    albumName = albumName,
                    albumArtUrlString = albumArtUrlString,
                    artistsString = artistsString,
                    yearOfRelease = yearOfRelease,
                    headerTranslation = collapsingHeaderState.translation,
                    translationProgress = collapsingHeaderState.progress,
                    isLoadingPlaceholderForAlbumArtVisible = isLoadingPlaceholderForAlbumArtVisible,
                    onImageLoading = { isLoadingPlaceholderForAlbumArtVisible = true },
                    onImageLoaded = { isLoadingPlaceholderForAlbumArtVisible = false },
                )
            },
            body = {
                TrackList(
                    lazyListState = lazyListState,
                    isErrorMessageVisible = isErrorMessageVisible,
                    trackList = trackList,
                    onTrackItemClick = onTrackItemClick,
                    currentlyPlayingTrack = currentlyPlayingTrack
                )
            }
        )
        DefaultMusifyLoadingAnimation(
            modifier = Modifier.align(Alignment.Center),
            isVisible = isLoading
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
            title = albumName,
            contentAlpha = collapsingHeaderState.progress.toNormalizedHeaderProgress(),
            onBackButtonClicked = onBackButtonClicked,
            onClick = {
                coroutineScope.launch { lazyListState.animateScrollToItem(0) }
            }
        )
    }
}

@ExperimentalMaterialApi
@Composable
private fun TrackList(
    lazyListState: LazyListState,
    isErrorMessageVisible: Boolean,
    trackList: List<SearchResult.TrackSearchResult>,
    onTrackItemClick: (SearchResult.TrackSearchResult) -> Unit,
    currentlyPlayingTrack: SearchResult.TrackSearchResult?
) {
    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                bottom = MusifyBottomNavigationConstants.navigationHeight + MusifyMiniPlayerConstants.miniPlayerHeight
            ),
            state = lazyListState
        ) {
            when {
                isErrorMessageVisible -> item {
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
                    items = trackList,
                    key = SearchResult.TrackSearchResult::id
                ) {
                    MusifyCompactTrackCard(
                        track = it,
                        onClick = onTrackItemClick,
                        isCurrentlyPlaying = it == currentlyPlayingTrack,
                        isAlbumArtVisible = false,
                        subtitleTextStyle = LocalTextStyle.current.copy(
                            fontWeight = FontWeight.Thin,
                            color = MaterialTheme.colors.onBackground.copy(alpha = ContentAlpha.disabled),
                        ),
                        contentPadding = PaddingValues(16.dp)
                    )
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
        val itemsAvailable = trackList.size + 1 // Include header
        val scrollbarState = lazyListState.scrollbarState(
            itemsAvailable = itemsAvailable,
        )
        lazyListState.DraggableScrollbar(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(bottom = 56.dp),
            state = scrollbarState,
            orientation = Orientation.Vertical,
            onThumbMoved = lazyListState.rememberDraggableScroller(
                itemsAvailable = itemsAvailable,
            )
        )
    }
}

@Composable
private fun AlbumArtHeaderMetadata(yearOfRelease: String) {
    Text(
        text = "Album • $yearOfRelease",
        fontWeight = FontWeight.Normal,
        style = MaterialTheme.typography
            .subtitle2
            .copy(
                color = MaterialTheme.colors
                    .onBackground
                    .copy(alpha = ContentAlpha.medium)
            )
    )
}

@Composable
private fun HeaderWithImageItem(
    dynamicBackgroundColor: Color,
    albumName: String,
    albumArtUrlString: String,
    artistsString: String,
    yearOfRelease: String,
    headerTranslation: Float,
    translationProgress: Float,
    isLoadingPlaceholderForAlbumArtVisible: Boolean,
    onImageLoading: () -> Unit,
    onImageLoaded: (Throwable?) -> Unit
) {
    Column(
        modifier = Modifier
            .offset { IntOffset(x = 0, y = -headerTranslation.roundToInt()) }
            .gradientBackground(
                startColor = dynamicBackgroundColor,
                endColor = MaterialTheme.colors.surface
            )
            .statusBarsPadding()
    ) {
        ImageHeaderWithMetadata(
            title = albumName,
            headerImageSource = HeaderImageSource.ImageFromUrlString(albumArtUrlString),
            subtitle = artistsString,
            headerTranslation = headerTranslation,
            translationProgress = translationProgress,
            isLoadingPlaceholderVisible = isLoadingPlaceholderForAlbumArtVisible,
            onImageLoading = onImageLoading,
            onImageLoaded = onImageLoaded,
            additionalMetadataContent = { AlbumArtHeaderMetadata(yearOfRelease) }
        )
        Spacer(modifier = Modifier.size(16.dp))
    }
}