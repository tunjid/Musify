package com.example.musify.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material.ContentAlpha
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Slider
import androidx.compose.material.SliderDefaults
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Share
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.musify.R
import com.example.musify.domain.Streamable
import com.example.musify.ui.components.AsyncImageWithPlaceholder
import com.example.musify.ui.dynamicTheme.dynamicbackgroundmodifier.DynamicBackgroundResource
import com.example.musify.ui.dynamicTheme.dynamicbackgroundmodifier.dynamicBackground

// collecting the flow within the composable scopes the collector to the composable.
// This ensures that the collection of flow is stopped as soon this composable
// is removed from composition. Therefore, this composables use parameters of type
// Flow.
@Composable
fun NowPlayingScreen(
    streamable: Streamable,
    timeElapsedString: String,
    playbackProgress: Float,
    playbackDurationRange: ClosedFloatingPointRange<Float>,
    isPlaybackPaused: Boolean,
    totalDurationOfCurrentTrackProvider: () -> String,
    onCloseButtonClicked: () -> Unit,
    onShuffleButtonClicked: () -> Unit,
    onSkipPreviousButtonClicked: () -> Unit,
    onPlayButtonClicked: () -> Unit,
    onPauseButtonClicked: () -> Unit,
    onSkipNextButtonClicked: () -> Unit,
    onRepeatButtonClicked: () -> Unit
) {
    var isImageLoadingPlaceholderVisible by remember { mutableStateOf(true) }
    val dynamicBackgroundResource = remember {
        DynamicBackgroundResource.FromImageUrl(streamable.streamInfo.imageUrl)
    }
    // All built-in compose layouts don't use a surface to display the content.
    // This means, if there is a list of clickable tracks displayed behind
    // the layout, then it will be possible to click them even if they are
    // not visible. To prevent such a behavior, surround the NowPlayingScreen
    // content with a surface.
    Surface {
        Column(
            modifier = Modifier
                .dynamicBackground(dynamicBackgroundResource)
                .fillMaxSize()
                .systemBarsPadding()
                .padding(start = 16.dp, end = 16.dp)
        ) {
            Header(
                modifier = Modifier.fillMaxWidth(),
                onCloseButtonClicked = onCloseButtonClicked,
                onTrailingButtonClick = {}
            )
            Spacer(modifier = Modifier.size(64.dp))
            AsyncImageWithPlaceholder(
                modifier = Modifier
                    .size(330.dp)
                    .align(Alignment.CenterHorizontally),
                model = streamable.streamInfo.imageUrl,
                contentDescription = null,
                onImageLoadingFinished = { isImageLoadingPlaceholderVisible = false },
                isLoadingPlaceholderVisible = isImageLoadingPlaceholderVisible,
                onImageLoading = { isImageLoadingPlaceholderVisible = true }
            )
            Spacer(modifier = Modifier.size(64.dp))
            Text(
                text = streamable.streamInfo.title,
                fontWeight = FontWeight.Bold,
                overflow = TextOverflow.Ellipsis,
                maxLines = 1,
                style = MaterialTheme.typography.h6
            )
            Text(
                text = streamable.streamInfo.subtitle,
                fontWeight = FontWeight.SemiBold,
                style = MaterialTheme.typography.subtitle1.copy(
                    color = MaterialTheme.colors.onBackground.copy(alpha = ContentAlpha.medium)
                ),
                overflow = TextOverflow.Ellipsis,
                maxLines = 1
            )
            Spacer(modifier = Modifier.size(8.dp))
            Box {
                ProgressSliderWithTimeText(modifier = Modifier.fillMaxWidth(),
                    timeElapsedString = timeElapsedString,
                    playbackProgress = playbackProgress,
                    totalDurationOfTrack = totalDurationOfCurrentTrackProvider(),
                    playbackDurationRange = playbackDurationRange,
                    onSliderValueChange = {})
            }
            PlaybackControls(
                modifier = Modifier.fillMaxWidth(),
                isPlayIconVisible = isPlaybackPaused,
                onSkipPreviousButtonClicked = onSkipPreviousButtonClicked,
                onPlayButtonClicked = onPlayButtonClicked,
                onPauseButtonClicked = onPauseButtonClicked,
                onSkipNextButtonClicked = onSkipNextButtonClicked,
                onRepeatButtonClicked = onRepeatButtonClicked,
                onShuffleButtonClicked = onShuffleButtonClicked
            )
            Footer(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding(),
                onAvailableDevicesButtonClicked = {},
                onShareButtonClicked = {}
            )
        }
    }
}

@Composable
private fun Header(
    modifier: Modifier = Modifier,
    onCloseButtonClicked: () -> Unit,
    onTrailingButtonClick: () -> Unit
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        val expandMoreIcon = painterResource(R.drawable.ic_expand_more_24)
        val moreHorizIcon = painterResource(id = R.drawable.ic_more_horiz_24)
        IconButton(modifier = Modifier.offset(x = (-16).dp), // accommodate for increased size of icon because of touch target sizing
            onClick = onCloseButtonClicked,
            content = { Icon(painter = expandMoreIcon, contentDescription = null) })
        Text(
            text = "Now playing",
            style = MaterialTheme.typography.subtitle1,
            fontWeight = FontWeight.SemiBold
        )
        IconButton(modifier = Modifier.offset(x = (16).dp), // accommodate for increased size of icon because of touch target sizing
            onClick = onTrailingButtonClick,
            content = { Icon(painter = moreHorizIcon, contentDescription = null) })
    }
}

@Composable
private fun Footer(
    modifier: Modifier = Modifier,
    onShareButtonClicked: () -> Unit,
    onAvailableDevicesButtonClicked: () -> Unit
) {

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        val availableDevicesIcon = painterResource(id = R.drawable.ic_available_devices)
        IconButton(onClick = onAvailableDevicesButtonClicked,
            content = { Icon(painter = availableDevicesIcon, contentDescription = null) })
        IconButton(onClick = onShareButtonClicked,
            content = { Icon(imageVector = Icons.Filled.Share, contentDescription = null) })
    }
}

@Composable
private fun PlaybackControls(
    modifier: Modifier = Modifier,
    isPlayIconVisible: Boolean,
    onSkipPreviousButtonClicked: () -> Unit,
    onShuffleButtonClicked: () -> Unit,
    onPlayButtonClicked: () -> Unit,
    onPauseButtonClicked: () -> Unit,
    onSkipNextButtonClicked: () -> Unit,
    onRepeatButtonClicked: () -> Unit
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onShuffleButtonClicked) {
            Icon(
                painter = painterResource(R.drawable.ic_round_shuffle_24), contentDescription = null
            )
        }
        IconButton(onClick = onSkipPreviousButtonClicked) {
            Icon(
                modifier = Modifier.size(40.dp),
                painter = painterResource(R.drawable.ic_skip_previous_24),
                contentDescription = null
            )
        }
        IconButton(onClick = if (isPlayIconVisible) onPlayButtonClicked else onPauseButtonClicked) {
            Icon(
                modifier = Modifier.size(72.dp),
                painter = if (isPlayIconVisible) painterResource(R.drawable.ic_play_circle_filled_24)
                else painterResource(R.drawable.ic_pause_circle_filled_24),
                contentDescription = null
            )
        }
        IconButton(onClick = onSkipNextButtonClicked) {
            Icon(
                modifier = Modifier.size(40.dp),
                painter = painterResource(R.drawable.ic_skip_next_24),
                contentDescription = null
            )
        }
        IconButton(onClick = onRepeatButtonClicked) {
            Icon(
                painter = painterResource(R.drawable.ic_round_repeat_24), contentDescription = null
            )
        }
    }
}

@Composable
private fun ProgressSliderWithTimeText(
    modifier: Modifier = Modifier,
    timeElapsedString: String,
    playbackProgress: Float,
    totalDurationOfTrack: String,
    playbackDurationRange: ClosedFloatingPointRange<Float>,
    onSliderValueChange: (Float) -> Unit
) {
    Column(modifier = modifier) {
        Slider(
            modifier = Modifier.fillMaxWidth(),
            value = playbackProgress,
            valueRange = playbackDurationRange,
            colors = SliderDefaults.colors(
                thumbColor = Color.White, activeTrackColor = Color.White
            ),
            onValueChange = onSliderValueChange
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = timeElapsedString, style = MaterialTheme.typography.caption
            )
            Text(
                text = totalDurationOfTrack, style = MaterialTheme.typography.caption
            )
        }
    }
}
