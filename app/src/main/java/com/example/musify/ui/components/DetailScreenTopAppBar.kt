package com.example.musify.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.musify.R
import com.example.musify.ui.components.collapsingheader.CollapsingHeaderState
import com.example.musify.ui.dynamicTheme.dynamicbackgroundmodifier.dynamicBackground
import com.example.musify.ui.dynamicTheme.dynamicbackgroundmodifier.gradientBackground
import com.example.musify.ui.theme.MusifyTheme

/**
 * An appbar that is meant to be used in a detail screen. It is mainly
 * used to display the [title] with a back button. This overload
 * uses the [Modifier.dynamicBackground] modifier.
 *
 * @param title the title to be displayed.
 * @param onBackButtonClicked the lambda to execute with the user clicks
 * on the back button.
 * @param modifier the modifier to be applied to the app bar.
 * @param onClick the lambda to execute when the app bar clicked. This is
 * usually used to scroll a list to the first item.
 */
@Composable
fun DetailScreenTopAppBar(
    title: String,
    modifier: Modifier = Modifier,
    contentAlpha: Float,
    onBackButtonClicked: () -> Unit,
    onClick: () -> Unit = {},
) {
    // Since the top app bar's background color is transparent,
    // any elevation to the app bar would make it look like it has
    // a border. Therefore, set the elevation to 0dp.
    TopAppBar(
        modifier = modifier
            .clickable(onClick = onClick),
        backgroundColor = Color.Transparent,
        elevation = 0.dp
    ) {
        IconButton(
            modifier = Modifier
                .clip(CircleShape)
                .align(Alignment.CenterVertically)
                .offset(y = 1.dp),
            onClick = onBackButtonClicked
        ) {
            Icon(
                imageVector = ImageVector.vectorResource(id = R.drawable.ic_baseline_chevron_left_24),
                contentDescription = null,
                tint = Color.White
            )
        }
        Text(
            modifier = Modifier
                .alpha(contentAlpha)
                .align(Alignment.CenterVertically),
            text = title,
            style = MaterialTheme.typography.body1,
            fontWeight = FontWeight.SemiBold,
            color = Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Preview
@Composable
fun DetailScreenTopAppBarPreview() {
    MusifyTheme {
        DetailScreenTopAppBar(
            title = "Title",
            contentAlpha = 1f,
            onBackButtonClicked = {},
        )
    }
}

@Composable
fun detailCollapsingHeaderState(): CollapsingHeaderState {
    val density = LocalDensity.current
    val collapsedHeight = with(density) { 56.dp.toPx() } +
            WindowInsets.statusBars.getTop(density).toFloat() +
            WindowInsets.statusBars.getBottom(density).toFloat()
    return remember(collapsedHeight) {
        CollapsingHeaderState(
            initialExpandedHeight = with(density) { 400.dp.toPx() },
            initialCollapsedHeight = collapsedHeight,
        )
    }
}

fun Modifier.detailTopAppBarGradient(
    startColor: Color,
    endColor: Color,
    progress: Float
) = gradientBackground(
    startColor = lerp(
        start = startColor,
        stop = endColor,
        fraction = .6f
    ).copy(alpha = progress),
    endColor = lerp(
        start = startColor,
        stop = endColor,
        fraction = .8f
    ).copy(alpha = progress)
)

fun Float.toNormalizedHeaderProgress() = when (this) {
    in 0f..headerProgressRange.start -> 0f
    in headerProgressRange -> (this - headerProgressRange.start) /
            (headerProgressRange.endInclusive - headerProgressRange.start)

    else -> 1f
}

private val headerProgressRange = 0.5f..0.7f
