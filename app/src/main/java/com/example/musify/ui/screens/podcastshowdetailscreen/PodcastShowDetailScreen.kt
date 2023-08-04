package com.example.musify.ui.screens.podcastshowdetailscreen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.ContentAlpha
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedButton
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.text.HtmlCompat
import com.example.musify.R
import com.example.musify.data.repositories.podcastsrepository.PodcastQuery
import com.example.musify.domain.PodcastEpisode
import com.example.musify.domain.PodcastShow
import com.example.musify.domain.equalsIgnoringImageSize
import com.example.musify.ui.components.AsyncImageWithPlaceholder
import com.example.musify.ui.components.DefaultMusifyLoadingAnimation
import com.example.musify.ui.components.DetailScreenTopAppBar
import com.example.musify.ui.components.HtmlTextView
import com.example.musify.ui.components.scrollbar.DraggableScrollbar
import com.example.musify.ui.components.scrollbar.rememberTiledDraggableScroller
import com.example.musify.ui.components.scrollbar.tiledListScrollbarState
import com.example.musify.ui.dynamicTheme.dynamicbackgroundmodifier.DynamicBackgroundResource
import com.example.musify.ui.dynamicTheme.dynamicbackgroundmodifier.dynamicBackground
import com.tunjid.tiler.TiledList
import com.tunjid.tiler.compose.PivotedTilingEffect
import kotlinx.coroutines.launch

@ExperimentalMaterialApi
@Composable
fun PodcastShowDetailScreen(
    podcastShow: PodcastShow,
    onBackButtonClicked: () -> Unit,
    onEpisodePlayButtonClicked: (PodcastEpisode) -> Unit,
    onEpisodePauseButtonClicked: (PodcastEpisode) -> Unit,
    currentlyPlayingEpisode: PodcastEpisode?,
    isCurrentlyPlayingEpisodePaused: Boolean?,
    isPlaybackLoading: Boolean,
    onEpisodeClicked: (PodcastEpisode) -> Unit,
    onQueryChanged: (PodcastQuery?) -> Unit,
    items: TiledList<PodcastQuery, ShowItem>
) {
    val lazyListState = rememberLazyListState()
    val isAppBarVisible by remember {
        derivedStateOf {
            if (lazyListState.firstVisibleItemIndex != 0) return@derivedStateOf true
            // The first item in the list is the header with the image of the show.
            // If the first item (item at index 0) is offset by more than 200dp
            // display the app bar.
            lazyListState.firstVisibleItemScrollOffset > 200
        }
    }
    val spannedHtmlDescription = remember {
        HtmlCompat.fromHtml(podcastShow.htmlDescription, 0)
    }
    val dynamicBackgroundResource = remember {
        DynamicBackgroundResource.FromImageUrl(podcastShow.imageUrlString)
    }
    val scope = rememberCoroutineScope()
    Box {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            state = lazyListState
        ) {
            item {
                Header(
                    imageUrlString = podcastShow.imageUrlString,
                    onBackButtonClicked = onBackButtonClicked,
                    title = podcastShow.name,
                    nameOfPublisher = podcastShow.nameOfPublisher
                )
            }
            item {
                // make text expandable once support for spanned
                // text is made available for compose.
                HtmlTextView(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    text = spannedHtmlDescription,
                    textAppearanceResId = com.google.android.material.R.style.TextAppearance_MaterialComponents_Subtitle2,
                    color = Color.White.copy(alpha = ContentAlpha.medium),
                )
            }
            item {
                Spacer(modifier = Modifier.size(8.dp))
                Text(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    color = Color.White,
                    text = "Episodes",
                    fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.subtitle1
                )
                Spacer(modifier = Modifier.size(8.dp))
            }
            this.items(
                items = items,
                key = ShowItem::pagedIndex
            ) { item ->
                when (item) {
                    is ShowItem.Loaded -> StreamableEpisodeCard(
                        episode = item.episode,
                        isEpisodePlaying = currentlyPlayingEpisode.equalsIgnoringImageSize(item.episode) && isCurrentlyPlayingEpisodePaused == false,
                        isCardHighlighted = currentlyPlayingEpisode.equalsIgnoringImageSize(item.episode),
                        onPlayButtonClicked = { onEpisodePlayButtonClicked(item.episode) },
                        onPauseButtonClicked = { onEpisodePauseButtonClicked(item.episode) },
                        onClicked = { onEpisodeClicked(item.episode) },
                    )

                    is ShowItem.Placeholder -> StreamableEpisodeLoadingCard()
                }
            }
        }
        AnimatedVisibility(
            visible = isAppBarVisible,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            DetailScreenTopAppBar(
                modifier = Modifier
                    .statusBarsPadding()
                    .fillMaxWidth(),
                title = podcastShow.name,
                onBackButtonClicked = onBackButtonClicked,
                dynamicBackgroundResource = dynamicBackgroundResource,
                onClick = {
                    scope.launch { lazyListState.animateScrollToItem(0) }
                }
            )
        }
        DefaultMusifyLoadingAnimation(
            modifier = Modifier.align(Alignment.Center),
            isVisible = isPlaybackLoading
        )
        val scrollbarState = lazyListState.tiledListScrollbarState(
            itemsAvailable = podcastShow.totalEpisodes,
            tiledItems = items
        )
        lazyListState.DraggableScrollbar(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .systemBarsPadding()
                .padding(
                    top = 56.dp,
                    bottom = 56.dp,
                ),
            state = scrollbarState,
            orientation = Orientation.Vertical,
            onThumbMoved = lazyListState.rememberTiledDraggableScroller(
                itemsAvailable = podcastShow.totalEpisodes,
                tiledItems = items,
                onQueryChanged = onQueryChanged,
            )
        )
        lazyListState.PivotedTilingEffect(
            items = items,
            onQueryChanged = onQueryChanged
        )
    }
}

@Composable
private fun Header(
    imageUrlString: String,
    onBackButtonClicked: () -> Unit,
    title: String,
    nameOfPublisher: String
) {
    val dynamicBackgroundResource =
        remember { DynamicBackgroundResource.FromImageUrl(imageUrlString) }
    val columnVerticalArrangementSpacing = 16.dp

    Column(
        modifier = Modifier
            .dynamicBackground(dynamicBackgroundResource)
            .fillMaxWidth()
            .systemBarsPadding(),
        verticalArrangement = Arrangement.spacedBy(columnVerticalArrangementSpacing)
    ) {
        IconButton(onClick = onBackButtonClicked) {
            Icon(
                imageVector = ImageVector.vectorResource(R.drawable.ic_baseline_chevron_left_24),
                contentDescription = null
            )
        }
        PodcastHeaderImageWithMetadata(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                // The back button above has a slightly bigger size because
                // of the default touch target sizing applied by compose. This
                // together with the vertical arrangement specified by the
                // parent column, cause this composable have a lot of padding on
                // top of it. Therefore, apply an offset to reduce the spacing
                // above the composable.
                .offset(y = -columnVerticalArrangementSpacing),
            imageUrl = imageUrlString,
            title = title,
            nameOfPublisher = nameOfPublisher
        )
        HeaderActionsRow(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .alpha(ContentAlpha.medium)
        )
    }
}

@Composable
private fun PodcastHeaderImageWithMetadata(
    imageUrl: String,
    title: String,
    nameOfPublisher: String,
    modifier: Modifier = Modifier
) {
    var isThumbnailLoading by remember { mutableStateOf(true) }
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImageWithPlaceholder(
            modifier = Modifier
                .size(176.dp)
                .clip(RoundedCornerShape(16.dp)),
            model = imageUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            onImageLoadingFinished = { isThumbnailLoading = false },
            isLoadingPlaceholderVisible = isThumbnailLoading,
            onImageLoading = { isThumbnailLoading = true }
        )
        Spacer(modifier = Modifier.size(16.dp))
        Column {
            Text(
                text = title,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.size(8.dp))
            Text(
                text = nameOfPublisher,
                maxLines = 1,
                style = MaterialTheme.typography.caption,
                color = Color.White
            )
        }
    }
}

@Composable
private fun HeaderActionsRow(modifier: Modifier = Modifier) {
    Row(modifier = modifier) {
        OutlinedButton(
            onClick = { },
            colors = ButtonDefaults.outlinedButtonColors(
                backgroundColor = Color.Transparent,
                contentColor = Color.White,
            ),
            border = BorderStroke(1.dp, Color.White)
        ) {
            Text(
                text = "Follow",
                style = MaterialTheme.typography.caption,
                fontWeight = FontWeight.SemiBold
            )
        }
        Spacer(modifier = Modifier.size(8.dp))
        IconButton(onClick = { }) {
            Icon(
                imageVector = Icons.Outlined.Notifications,
                contentDescription = null
            )
        }
        IconButton(onClick = { }) {
            Icon(
                imageVector = Icons.Outlined.Settings,
                contentDescription = null
            )
        }
        IconButton(onClick = { }) {
            Icon(
                imageVector = Icons.Filled.MoreVert,
                contentDescription = null
            )
        }
    }
}