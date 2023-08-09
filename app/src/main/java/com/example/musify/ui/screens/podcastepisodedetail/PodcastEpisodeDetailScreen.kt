package com.example.musify.ui.screens.podcastepisodedetail

import android.text.Spanned
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.ContentAlpha
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Share
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.text.HtmlCompat
import com.example.musify.R
import com.example.musify.domain.PodcastEpisode
import com.example.musify.domain.getFormattedDateAndDurationString
import com.example.musify.ui.components.AsyncImageWithPlaceholder
import com.example.musify.ui.components.DefaultMusifyLoadingAnimation
import com.example.musify.ui.components.DetailScreenTopAppBar
import com.example.musify.ui.components.HtmlTextView
import com.example.musify.ui.components.MusifyBottomNavigationConstants
import com.example.musify.ui.components.collapsingheader.CollapsingHeader
import com.example.musify.ui.components.detailCollapsingHeaderState
import com.example.musify.ui.components.detailTopAppBarGradient
import com.example.musify.ui.components.toNormalizedHeaderProgress
import com.example.musify.ui.dynamicTheme.dynamicbackgroundmodifier.DynamicBackgroundResource
import com.example.musify.ui.dynamicTheme.dynamicbackgroundmodifier.backgroundColor
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import com.google.android.material.R as materialR


@Composable
fun PodcastEpisodeDetailScreen(
    podcastEpisode: PodcastEpisode,
    isPlaybackLoading: Boolean,
    isEpisodeCurrentlyPlaying: Boolean,
    onPlayButtonClicked: () -> Unit,
    onPauseButtonClicked: () -> Unit,
    onShareButtonClicked: () -> Unit,
    onAddButtonClicked: () -> Unit,
    onDownloadButtonClicked: () -> Unit,
    onBackButtonClicked: () -> Unit,
    navigateToPodcastDetailScreen: () -> Unit
) {
    val context = LocalContext.current
    val lazyListState = rememberLazyListState()
    val dynamicBackgroundResource = remember(podcastEpisode) {
        DynamicBackgroundResource.FromImageUrl(podcastEpisode.podcastShowInfo.imageUrl)
    }
    val coroutineScope = rememberCoroutineScope()
    val descriptionSpannedText = remember { HtmlCompat.fromHtml(podcastEpisode.htmlDescription, 0) }

    val dynamicBackgroundColor by dynamicBackgroundResource.backgroundColor()
    val collapsingHeaderState = detailCollapsingHeaderState()

    Box {
        CollapsingHeader(
            state = collapsingHeaderState,
            headerContent = {
                PodcastEpisodeHeader(
                    modifier = Modifier.offset {
                        IntOffset(x = 0, y = -collapsingHeaderState.translation.roundToInt())
                    },
                    dynamicBackgroundColor = dynamicBackgroundColor,
                    headerProgress = collapsingHeaderState.progress,
                    onPodcastShowTitleClicked = navigateToPodcastDetailScreen,
                    episodeImageUrl = podcastEpisode.podcastShowInfo.imageUrl,
                    episodeTitle = podcastEpisode.title,
                    podcastName = podcastEpisode.podcastShowInfo.name,
                    dateAndDurationString = podcastEpisode.getFormattedDateAndDurationString(context)
                )
            },
            body = {
                LazyColumn(state = lazyListState) {
                    item {
                        // The episode header has a background gradient that needs to
                        // be edge-to-edge. Therefore, use a separate column to add
                        // horizontal padding to the rest of the content.
                        Spacer(Modifier.height(16.dp))
                        PodcastEpisodeScreenContent(
                            isEpisodePlaying = isEpisodeCurrentlyPlaying,
                            htmlDescription = descriptionSpannedText,
                            onPlayButtonClicked = onPlayButtonClicked,
                            onPauseButtonClicked = onPauseButtonClicked,
                            onShareButtonClicked = onShareButtonClicked,
                            onAddButtonClicked = onAddButtonClicked,
                            onDownloadButtonClicked = onDownloadButtonClicked,
                            onSeeAllEpisodesButtonClicked = navigateToPodcastDetailScreen
                        )
                    }
                }
            }
        )

        DetailScreenTopAppBar(
            modifier = Modifier
                .detailTopAppBarGradient(
                    startColor = dynamicBackgroundColor,
                    endColor = MaterialTheme.colors.surface,
                    progress = collapsingHeaderState.progress.toNormalizedHeaderProgress()
                )
                .statusBarsPadding()
                .fillMaxWidth(),
            title = podcastEpisode.title,
            contentAlpha = collapsingHeaderState.progress.toNormalizedHeaderProgress(),
            onBackButtonClicked = onBackButtonClicked,
            onClick = {
                coroutineScope.launch { lazyListState.animateScrollToItem(0) }
            },
        )
        DefaultMusifyLoadingAnimation(
            modifier = Modifier.align(Alignment.Center),
            isVisible = isPlaybackLoading
        )
    }
}

@Composable
private fun PodcastEpisodeScreenContent(
    isEpisodePlaying: Boolean,
    htmlDescription: Spanned,
    onPlayButtonClicked: () -> Unit,
    onPauseButtonClicked: () -> Unit,
    onShareButtonClicked: () -> Unit,
    onAddButtonClicked: () -> Unit,
    onDownloadButtonClicked: () -> Unit,
    onSeeAllEpisodesButtonClicked: () -> Unit
) {
    Column(
        modifier = Modifier.padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        ActionsRow(
            modifier = Modifier.fillMaxWidth(),
            isPlaying = isEpisodePlaying,
            onPlayButtonClicked = onPlayButtonClicked,
            onPauseButtonClicked = onPauseButtonClicked,
            onShareButtonClicked = onShareButtonClicked,
            onAddButtonClicked = onAddButtonClicked,
            onDownloadButtonClicked = onDownloadButtonClicked,
        )
        // make text expandable once support for spanned
        //  text is made available for compose.
        HtmlTextView(
            text = htmlDescription,
            textAppearanceResId = materialR.style.TextAppearance_MaterialComponents_Subtitle2,
            color = Color.White.copy(alpha = ContentAlpha.medium)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onSeeAllEpisodesButtonClicked() }
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween) {
            Text(
                text = "See all episodes",
                color = Color.White,
                fontWeight = FontWeight.Bold,
            )
            Icon(
                imageVector = ImageVector.vectorResource(id = R.drawable.ic_baseline_chevron_right_24),
                contentDescription = null
            )
        }
        Spacer(modifier = Modifier.size(MusifyBottomNavigationConstants.navigationHeight))
    }
}

@Composable
private fun PodcastEpisodeHeader(
    modifier: Modifier = Modifier,
    dynamicBackgroundColor: Color,
    headerProgress: Float,
    onPodcastShowTitleClicked: () -> Unit,
    episodeImageUrl: String,
    episodeTitle: String,
    podcastName: String,
    dateAndDurationString: String
) {
    var isImageLoadingPlaceholderVisible by remember { mutableStateOf(true) }

    Column(
        modifier = modifier
            .detailTopAppBarGradient(
                startColor = dynamicBackgroundColor,
                endColor = MaterialTheme.colors.surface,
                progress = headerProgress.toNormalizedHeaderProgress()
            )
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(
                vertical = 56.dp,
                horizontal = 16.dp
            ),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        AsyncImageWithPlaceholder(
            modifier = Modifier
                .size(60.dp)
                .clip(RoundedCornerShape(4.dp)),
            model = episodeImageUrl,
            contentDescription = null,
            onImageLoadingFinished = { isImageLoadingPlaceholderVisible = false },
            isLoadingPlaceholderVisible = isImageLoadingPlaceholderVisible,
            onImageLoading = {
                if (!isImageLoadingPlaceholderVisible) isImageLoadingPlaceholderVisible = true
            },
            contentScale = ContentScale.Crop
        )
        Spacer(modifier = Modifier.height(32.dp))
        Text(
            text = episodeTitle,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold
        )
        // using a column to prevent the text being spaced by 16dp (set by the parent column)
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                modifier = Modifier.clickable { onPodcastShowTitleClicked() },
                text = podcastName,
                style = MaterialTheme.typography.caption,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = dateAndDurationString,
                style = MaterialTheme.typography.caption,
                color = Color.White.copy(alpha = ContentAlpha.medium)
            )
            Spacer(modifier = Modifier.size(8.dp))
        }
    }
}

@Composable
fun ActionsRow(
    modifier: Modifier = Modifier,
    isPlaying: Boolean,
    onPlayButtonClicked: () -> Unit,
    onPauseButtonClicked: () -> Unit,
    onShareButtonClicked: () -> Unit,
    onAddButtonClicked: () -> Unit,
    onDownloadButtonClicked: () -> Unit
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Button(
            modifier = Modifier.width(width = 120.dp),
            onClick = {
                if (isPlaying) onPauseButtonClicked()
                else onPlayButtonClicked()
            },
            shape = RoundedCornerShape(50),
            contentPadding = PaddingValues(vertical = 14.dp)
        ) {
            Text(
                text = if (isPlaying) "Pause" else "Play",
                fontWeight = FontWeight.SemiBold
            )
        }
        // Use a separate row to group the secondary action buttons. The primary
        // action button is the play button. This will make the parent row collectively
        // consider the secondary action buttons as a single row composable. This
        // ensures that the "SpaceBetween" arrangement (applied by the parent row)
        // creates space between the play button and the secondary action buttons
        // and not between all the buttons including the secondary action buttons.
        Row(verticalAlignment = Alignment.CenterVertically) {
            val iconTintColor = Color.White.copy(alpha = ContentAlpha.medium)
            IconButton(onClick = onShareButtonClicked) {
                Icon(
                    imageVector = Icons.Outlined.Share,
                    contentDescription = null,
                    tint = iconTintColor
                )
            }
            IconButton(onClick = onAddButtonClicked) {
                Icon(
                    imageVector = ImageVector.vectorResource(id = R.drawable.ic_baseline_add_circle_outline_24),
                    contentDescription = null,
                    tint = iconTintColor
                )
            }
            IconButton(onClick = onDownloadButtonClicked) {
                Icon(
                    imageVector = ImageVector.vectorResource(id = R.drawable.ic_outline_download_for_offline_24),
                    contentDescription = null,
                    tint = iconTintColor
                )
            }
        }
    }
}
