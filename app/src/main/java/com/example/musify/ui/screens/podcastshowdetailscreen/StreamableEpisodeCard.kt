package com.example.musify.ui.screens.podcastshowdetailscreen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.ContentAlpha
import androidx.compose.material.Divider
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Share
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.musify.R
import com.example.musify.domain.PodcastEpisode
import com.example.musify.domain.getFormattedDateAndDurationString
import com.example.musify.ui.components.AsyncImageWithPlaceholder
import com.google.accompanist.placeholder.PlaceholderHighlight
import com.google.accompanist.placeholder.material.placeholder
import com.google.accompanist.placeholder.material.shimmer


@ExperimentalMaterialApi
@Composable
fun StreamableEpisodeCard(
    episode: PodcastEpisode,
    isEpisodePlaying: Boolean,
    isCardHighlighted: Boolean,
    onPlayButtonClicked: () -> Unit,
    onPauseButtonClicked: () -> Unit,
    onClicked: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    StreamableEpisodeCard(
        isEpisodePlaying = isEpisodePlaying,
        isCardHighlighted = isCardHighlighted,
        onPlayButtonClicked = onPlayButtonClicked,
        onPauseButtonClicked = onPauseButtonClicked,
        onClicked = onClicked,
        thumbnailImageUrlString = episode.episodeImageUrl,
        title = episode.title,
        description = episode.description,
        dateAndDurationString = episode.getFormattedDateAndDurationString(context),
        modifier = modifier
    )
}

@ExperimentalMaterialApi
@Composable
fun StreamableEpisodeLoadingCard(
    modifier: Modifier = Modifier,
) {
    StreamableEpisodeCard(
        isEpisodePlaying = false,
        isCardHighlighted = false,
        onPlayButtonClicked = { },
        onPauseButtonClicked = { },
        onClicked = { },
        thumbnailImageUrlString = null,
        title = null,
        description = null,
        dateAndDurationString = null,
        modifier = modifier
    )
}

@ExperimentalMaterialApi
@Composable
private fun StreamableEpisodeCard(
    isEpisodePlaying: Boolean,
    isCardHighlighted: Boolean,
    onPlayButtonClicked: () -> Unit,
    onPauseButtonClicked: () -> Unit,
    onClicked: () -> Unit,
    thumbnailImageUrlString: String?,
    title: String?,
    description: String?,
    dateAndDurationString: String?,
    modifier: Modifier = Modifier
) {
    var isThumbnailLoading by remember { mutableStateOf(true) }
    val innerColumnVerticalArrangementSpacing = remember { 8.dp }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .then(modifier),
        elevation = 0.dp,
        shape = RectangleShape,
        onClick = onClicked
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .padding(top = 16.dp),
            verticalArrangement = Arrangement.spacedBy(innerColumnVerticalArrangementSpacing)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AsyncImageWithPlaceholder(
                    modifier = Modifier
                        .size(45.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    model = thumbnailImageUrlString,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    onImageLoadingFinished = {
                        if (isThumbnailLoading) isThumbnailLoading = false
                    },
                    isLoadingPlaceholderVisible = isThumbnailLoading,
                    onImageLoading = { isThumbnailLoading = true })
                Text(
                    modifier = Modifier
                        .fillMaxWidth()
                        .placeholder(
                            visible = title == null,
                            highlight = PlaceholderHighlight.shimmer(),
                        ),
                    text = title ?: "",
                    style = MaterialTheme.typography.subtitle2,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = if (isCardHighlighted) MaterialTheme.colors.primary
                    else Color.Unspecified
                )

            }
            Text(
                modifier = Modifier
                    .fillMaxWidth()
                    .placeholder(
                        visible = description == null,
                        highlight = PlaceholderHighlight.shimmer(),
                    ),
                text = description ?: "",
                style = MaterialTheme.typography.caption.copy(
                    Color.White.copy(alpha = ContentAlpha.medium)
                ),
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                modifier = Modifier.placeholder(visible = dateAndDurationString == null),
                text = dateAndDurationString ?: "",
                style = MaterialTheme
                    .typography
                    .caption
                    .copy(Color.White.copy(alpha = ContentAlpha.medium)),
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            // The footer composables shouldn't be affected by the parent's Arrangement.spacedBy().
            // Therefore, wrap them into another column so that the parent column sees these
            // composables as a single composable.
            Column {
                ActionsRow(
                    isEpisodePlaying = isEpisodePlaying,
                    onPlayButtonClicked = onPlayButtonClicked,
                    onPauseButtonClicked = onPauseButtonClicked,
                    onAddToLibraryButtonClicked = {},
                    onDownloadButtonClicked = {},
                    onShareButtonClick = {},
                    onMoreInfoButtonClick = {}
                )
                Divider(modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
private fun ActionsRow(
    isEpisodePlaying: Boolean,
    onPlayButtonClicked: () -> Unit,
    onPauseButtonClicked: () -> Unit,
    onAddToLibraryButtonClicked: () -> Unit,
    onDownloadButtonClicked: () -> Unit,
    onShareButtonClick: () -> Unit,
    onMoreInfoButtonClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(modifier),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.offset(x = (-16).dp) // to accommodate for touch target sizing
        ) {
            IconButton(onClick = onAddToLibraryButtonClicked) {
                Icon(
                    imageVector = ImageVector.vectorResource(id = R.drawable.ic_baseline_add_circle_outline_24),
                    contentDescription = null
                )
            }
            IconButton(onClick = onDownloadButtonClicked) {
                Icon(
                    imageVector = ImageVector.vectorResource(id = R.drawable.ic_outline_download_for_offline_24),
                    contentDescription = null
                )
            }
            IconButton(onClick = onShareButtonClick) {
                Icon(
                    imageVector = Icons.Outlined.Share,
                    contentDescription = null
                )
            }
            IconButton(onClick = onMoreInfoButtonClick) {
                Icon(
                    imageVector = Icons.Outlined.MoreVert,
                    contentDescription = null
                )
            }
        }
        IconButton(
            onClick = {
                if (isEpisodePlaying) onPauseButtonClicked()
                else onPlayButtonClicked()
            }
        ) {
            Icon(
                modifier = Modifier.size(40.dp),
                tint = Color.White,
                imageVector = ImageVector
                    .vectorResource(
                        if (isEpisodePlaying) R.drawable.ic_pause_circle_filled_24
                        else R.drawable.ic_play_circle_filled_24
                    ),
                contentDescription = null
            )
        }
    }
}