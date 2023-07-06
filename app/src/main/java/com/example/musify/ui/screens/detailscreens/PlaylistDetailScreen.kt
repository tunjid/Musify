package com.example.musify.ui.screens.detailscreens

import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.ContentAlpha
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.LocalTextStyle
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.musify.data.repositories.tracksrepository.PlaylistQuery
import com.example.musify.domain.SearchResult
import com.example.musify.ui.components.DetailScreenTopAppBar
import com.example.musify.ui.components.HeaderImageSource
import com.example.musify.ui.components.ImageHeaderWithMetadata
import com.example.musify.ui.components.MusifyBottomNavigationConstants
import com.example.musify.ui.components.MusifyCompactTrackCard
import com.example.musify.ui.components.MusifyMiniPlayerConstants
import com.example.musify.ui.dynamicTheme.dynamicbackgroundmodifier.DynamicBackgroundResource
import com.example.musify.ui.dynamicTheme.dynamicbackgroundmodifier.dynamicBackground
import com.tunjid.tiler.TiledList
import com.tunjid.tiler.compose.PivotedTilingEffect
import kotlinx.coroutines.launch


@ExperimentalMaterialApi
@Composable
fun PlaylistDetailScreen(
    playlistName: String,
    playlistImageUrlString: String?,
    nameOfPlaylistOwner: String,
    totalNumberOfTracks: String,
    @DrawableRes imageResToUseWhenImageUrlStringIsNull: Int,
    tracks: TiledList<PlaylistQuery, SearchResult.TrackSearchResult>,
    currentlyPlayingTrack: SearchResult.TrackSearchResult?,
    onQueryChanged: (PlaylistQuery?) -> Unit,
    onBackButtonClicked: () -> Unit,
    onTrackClicked: (SearchResult.TrackSearchResult) -> Unit,
) {
    var isLoadingPlaceholderForAlbumArtVisible by remember { mutableStateOf(false) }
    val lazyListState = rememberLazyListState()
    val isAppBarVisible by remember {
        derivedStateOf { lazyListState.firstVisibleItemIndex > 0 }
    }
    val dynamicBackgroundResource = remember {
        if (playlistImageUrlString == null) DynamicBackgroundResource.Empty
        else DynamicBackgroundResource.FromImageUrl(playlistImageUrlString)

    }
    val coroutineScope = rememberCoroutineScope()

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                bottom = MusifyBottomNavigationConstants.navigationHeight + MusifyMiniPlayerConstants.miniPlayerHeight
            ),
            state = lazyListState
        ) {
            headerWithImageItem(
                dynamicBackgroundResource = dynamicBackgroundResource,
                playlistName = playlistName,
                playlistImageUrlString = playlistImageUrlString,
                imageResToUseWhenImageUrlStringIsNull = imageResToUseWhenImageUrlStringIsNull,
                nameOfPlaylistOwner = nameOfPlaylistOwner,
                totalNumberOfTracks = totalNumberOfTracks,
                isLoadingPlaceholderForAlbumArtVisible = isLoadingPlaceholderForAlbumArtVisible,
                onImageLoading = { isLoadingPlaceholderForAlbumArtVisible = true },
                onImageLoaded = { isLoadingPlaceholderForAlbumArtVisible = false },
                onBackButtonClicked = onBackButtonClicked
            )
            if (false) {
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
            } else {
                items(
                    items = tracks,
                    key = SearchResult.TrackSearchResult::id
                ) {
                    MusifyCompactTrackCard(
                        track = it,
                        onClick = onTrackClicked,
                        isCurrentlyPlaying = it == currentlyPlayingTrack,
                        isAlbumArtVisible = true,
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
//        DefaultMusifyLoadingAnimation(
//            modifier = Modifier.align(Alignment.Center),
//            isVisible = isLoading
//        )
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
                title = playlistName,
                onBackButtonClicked = onBackButtonClicked,
                dynamicBackgroundResource = dynamicBackgroundResource,
                onClick = {
                    coroutineScope.launch { lazyListState.animateScrollToItem(0) }
                }
            )
        }
        lazyListState.PivotedTilingEffect(
            items = tracks,
            onQueryChanged = onQueryChanged
        )
    }
}

private fun LazyListScope.headerWithImageItem(
    dynamicBackgroundResource: DynamicBackgroundResource,
    playlistName: String,
    playlistImageUrlString: String?,
    @DrawableRes imageResToUseWhenImageUrlStringIsNull: Int,
    nameOfPlaylistOwner: String,
    totalNumberOfTracks: String,
    isLoadingPlaceholderForAlbumArtVisible: Boolean,
    onImageLoading: () -> Unit,
    onImageLoaded: (Throwable?) -> Unit,
    onBackButtonClicked: () -> Unit
) {
    item {
        Column(
            modifier = Modifier
                .dynamicBackground(dynamicBackgroundResource)
                .statusBarsPadding()
        ) {
            ImageHeaderWithMetadata(
                title = playlistName,
                headerImageSource = if (playlistImageUrlString == null)
                    HeaderImageSource.ImageFromDrawableResource(
                        resourceId = imageResToUseWhenImageUrlStringIsNull
                    )
                else HeaderImageSource.ImageFromUrlString(playlistImageUrlString),
                subtitle = "by $nameOfPlaylistOwner • $totalNumberOfTracks tracks",
                onBackButtonClicked = onBackButtonClicked,
                isLoadingPlaceholderVisible = isLoadingPlaceholderForAlbumArtVisible,
                onImageLoading = onImageLoading,
                onImageLoaded = onImageLoaded,
                additionalMetadataContent = { }
            )
            Spacer(modifier = Modifier.size(16.dp))
        }
    }
}