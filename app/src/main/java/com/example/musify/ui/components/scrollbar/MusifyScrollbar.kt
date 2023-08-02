package com.example.musify.ui.components.scrollbar

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.Orientation.Horizontal
import androidx.compose.foundation.gestures.Orientation.Vertical
import androidx.compose.foundation.gestures.ScrollableState
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

/**
 * The time period for showing the scrollbar thumb after interacting with it, before it fades away
 */
private const val SCROLLBAR_INACTIVE_TO_DORMANT_TIME_IN_MS = 2_000L

/**
 * A [Scrollbar] that allows for fast scrolling of content by dragging its thumb.
 * Its thumb disappears when the scrolling container is dormant.
 * @param modifier a [Modifier] for the [Scrollbar]
 * @param state the driving state for the [Scrollbar]
 * @param orientation the orientation of the scrollbar
 * @param onThumbMoved the fast scroll implementation
 */
@Composable
fun ScrollableState.DraggableScrollbar(
    modifier: Modifier = Modifier,
    state: ScrollbarState,
    orientation: Orientation,
    onThumbMoved: (Float) -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    Scrollbar(
        modifier = modifier,
        orientation = orientation,
        interactionSource = interactionSource,
        state = state,
        thumb = {
            DraggableScrollbarThumb(
                interactionSource = interactionSource,
                orientation = orientation,
            )
        },
        onThumbMoved = onThumbMoved,
    )
}

/**
 * A scrollbar thumb that is intended to also be a touch target for fast scrolling.
 */
@Composable
private fun ScrollableState.DraggableScrollbarThumb(
    interactionSource: InteractionSource,
    orientation: Orientation,
) {
    val state = scrollbarThumbState(interactionSource)
    Box(
        modifier = Modifier
            .run {
                when (orientation) {
                    Vertical -> width(12.dp).fillMaxHeight()
                    Horizontal -> height(12.dp).fillMaxWidth()
                }
            },
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .run {
                    when (orientation) {
                        Vertical -> width(4.dp)
                            .fillMaxHeight()
                            .offset(x = state.scrollbarThumbOffset())
                        Horizontal -> height(4.dp)
                            .fillMaxWidth()
                            .offset(y = state.scrollbarThumbOffset())
                    }
                }
                .background(
                    color = scrollbarThumbColor(state = state),
                    shape = RoundedCornerShape(16.dp),
                ),
        )
    }
}

/**
 * The [ThumbState] of the scrollbar thumb as a function of its interaction state.
 * @param interactionSource source of interactions in the scrolling container
 */
@Composable
private fun ScrollableState.scrollbarThumbState(
    interactionSource: InteractionSource,
): ThumbState {
    var state by remember { mutableStateOf(ThumbState.Dormant) }
    val pressed by interactionSource.collectIsPressedAsState()
    val hovered by interactionSource.collectIsHoveredAsState()
    val dragged by interactionSource.collectIsDraggedAsState()
    val active = (canScrollForward || canScrollForward) &&
            (pressed || hovered || isScrollInProgress)
    LaunchedEffect(dragged, active) {
        if (dragged) state = ThumbState.Dragged
        else when (active) {
            true -> state = ThumbState.Active
            false -> if (state == ThumbState.Active || state == ThumbState.Dragged) {
                state = ThumbState.Inactive
                delay(SCROLLBAR_INACTIVE_TO_DORMANT_TIME_IN_MS)
                state = ThumbState.Dormant
            }
        }
    }

    return state
}

/**
 * The color of the scrollbar thumb as a function of its interaction state.
 */
@Composable
private fun ScrollableState.scrollbarThumbColor(
    state: ThumbState
): Color {
    val color by animateColorAsState(
        targetValue = when (state) {
            ThumbState.Dragged -> MaterialTheme.colors.primary
            ThumbState.Active,
            ThumbState.Inactive -> MaterialTheme.colors.onSurface.copy(0.5f)
            ThumbState.Dormant -> Color.Transparent
        },
        animationSpec = SpringSpec(
            stiffness = Spring.StiffnessLow,
        ),
        label = "Scrollbar thumb color",
    )

    return color
}

/**
 * The color of the scrollbar thumb as a function of its interaction state.
 */
@Composable
private fun ThumbState.scrollbarThumbOffset(): Dp {
    val offset by animateDpAsState(
        targetValue = when (this) {
            ThumbState.Dragged,
            ThumbState.Inactive,
            ThumbState.Active -> 0.dp
            ThumbState.Dormant -> 16.dp
        },
        animationSpec = SpringSpec(
            stiffness = Spring.StiffnessLow,
        ),
        label = "Scrollbar thumb offset",
    )

    return offset
}

private enum class ThumbState {
    Dragged, Active, Inactive, Dormant
}
