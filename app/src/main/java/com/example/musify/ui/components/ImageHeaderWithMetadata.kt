package com.example.musify.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.max
import kotlin.math.roundToInt

/**
 * A sealed class hierarchy that contains the different header image
 * types.
 */
sealed class HeaderImageSource {
    data class ImageFromUrlString(val urlString: String) : HeaderImageSource()
    data class ImageFromDrawableResource(@DrawableRes val resourceId: Int) : HeaderImageSource()
}

/**
 * A composable that is used to display an image together with it's
 * [title] and [subtitle]. The image will be centered. The [title]
 * and [subtitle] will be placed after the image, in a vertical
 * manner.
 * @param title the title associated with the image.
 * @param headerImageSource the source to be used for the image.
 * @param subtitle the subtitle associated with the image.
 * back button is pressed.
 * @param isLoadingPlaceholderVisible used to indicate whether the loading
 * placeholder for the image is visible.
 * @param onImageLoading the lambda to execute when the image is
 * loading.
 * @param onImageLoaded the lambda to execute when the image has finished
 * loading. It is also provided with a nullable [Throwable] parameter that
 * can be used to determine whether the image was loaded successfully.
 * @param additionalMetadataContent the lambda the can be used to add additional
 * items that will appear after the [subtitle].
 */
@Composable
fun ImageHeaderWithMetadata(
    title: CharSequence,
    headerImageSource: HeaderImageSource,
    subtitle: CharSequence,
    modifier: Modifier = Modifier,
    headerTranslation: Float = 0f,
    translationProgress: Float = 1f,
    isLoadingPlaceholderVisible: Boolean = false,
    onImageLoading: () -> Unit = {},
    onImageLoaded: (Throwable?) -> Unit = {},
    additionalMetadataContent: @Composable () -> Unit,
) {
    val density = LocalDensity.current
    val maxImageHeight = 220.dp
    val minImageHeight = 100.dp
    val rawTranslatedImageHeight = maxImageHeight - with(density) { headerTranslation.toDp() }
    val translatedImageHeight = max(
        a = rawTranslatedImageHeight,
        b = minImageHeight
    )
    val imageTranslation = when {
        rawTranslatedImageHeight >= minImageHeight -> headerTranslation
        else -> headerTranslation + with(density) { rawTranslatedImageHeight.toPx() - minImageHeight.toPx() }
    }.roundToInt()
    Box(
        modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(top = 16.dp)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(maxImageHeight)
                    .align(Alignment.CenterHorizontally)
                    .alpha(translationProgress.toHeaderImageTransparency())
                    .offset { IntOffset(x = 0, y = imageTranslation) },
            ) {
                val imageModifier = Modifier
                    .height(translatedImageHeight)
                    .aspectRatio(1f)
                    .align(Alignment.TopCenter)
                    .shadow(8.dp)
                when (headerImageSource) {
                    is HeaderImageSource.ImageFromUrlString -> {
                        AsyncImageWithPlaceholder(
                            modifier = imageModifier,
                            model = headerImageSource.urlString,
                            contentDescription = null,
                            isLoadingPlaceholderVisible = isLoadingPlaceholderVisible,
                            onImageLoading = onImageLoading,
                            onImageLoadingFinished = onImageLoaded,
                            contentScale = ContentScale.Crop
                        )
                    }

                    is HeaderImageSource.ImageFromDrawableResource -> {
                        Image(
                            painter = painterResource(id = headerImageSource.resourceId),
                            modifier = imageModifier,
                            contentScale = ContentScale.Crop,
                            contentDescription = null,
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.size(16.dp))
            if (title is AnnotatedString) Text(
                text = title,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.h5
            )
            else Text(
                text = title.toString(),
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.h5
            )
            if (subtitle is AnnotatedString) Text(
                text = subtitle,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.subtitle1
            )
            else Text(
                text = subtitle.toString(),
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.subtitle1
            )
            additionalMetadataContent()
        }
    }
}

private fun Float.toHeaderImageTransparency() = when (this) {
    in 0f..thumbnailShrinkRange.start -> 1f
    in thumbnailShrinkRange -> 1f - (
            (this - thumbnailShrinkRange.start) /
                    (thumbnailShrinkRange.endInclusive - thumbnailShrinkRange.start)
            )

    else -> 0f
}

private val thumbnailShrinkRange = 0.3f..0.5f