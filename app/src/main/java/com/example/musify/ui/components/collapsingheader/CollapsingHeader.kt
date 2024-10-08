package com.example.musify.ui.components.collapsingheader

import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.AnchoredDraggableState
import androidx.compose.foundation.gestures.DraggableAnchors
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.anchoredDraggable
import androidx.compose.foundation.gestures.rememberScrollableState
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.util.packFloats
import androidx.compose.ui.util.unpackFloat1
import androidx.compose.ui.util.unpackFloat2
import kotlin.math.roundToInt

enum class CollapsingHeaderStatus {
    Collapsed, Expanded
}

@Stable
@OptIn(ExperimentalFoundationApi::class)
class CollapsingHeaderState(
    initialCollapsedHeight: Float,
    initialExpandedHeight: Float,
) {

    private var anchors by mutableLongStateOf(
        Anchors(
            collapsedHeight = initialCollapsedHeight,
            expandedHeight = initialExpandedHeight
        ).packedValue
    )

    var expandedHeight
        get() = Anchors(anchors).expandedHeight
        set(value) {
            anchors = Anchors(
                collapsedHeight = collapsedHeight,
                expandedHeight = value
            ).packedValue
            updateAnchors()
        }

    var collapsedHeight
        get() = Anchors(anchors).collapsedHeight
        set(value) {
            anchors = Anchors(
                collapsedHeight = value,
                expandedHeight = expandedHeight
            ).packedValue
            updateAnchors()
        }

    val translation get() = expandedHeight - anchoredDraggableState.requireOffset()

    val progress get() = translation / (expandedHeight - collapsedHeight)

    // This should not be externally visible. It is an implementation detail
    internal val anchoredDraggableState = AnchoredDraggableState(
        initialValue = CollapsingHeaderStatus.Collapsed,
        positionalThreshold = { distance: Float -> distance * 0.5f },
        velocityThreshold = { 100f },
        animationSpec = tween(),
        anchors = currentDraggableAnchors()
    )

    private fun updateAnchors() = anchoredDraggableState.updateAnchors(
        currentDraggableAnchors()
    )

    private fun currentDraggableAnchors() = DraggableAnchors {
        CollapsingHeaderStatus.Collapsed at expandedHeight
        CollapsingHeaderStatus.Expanded at collapsedHeight
    }
}

/**
 * A collapsing header implementation that has anchored positions.
 */
@Composable
@OptIn(ExperimentalFoundationApi::class)
fun CollapsingHeader(
    state: CollapsingHeaderState,
    headerContent: @Composable () -> Unit,
    body: @Composable () -> Unit,
) {
    val scrollableState = rememberScrollableState(
        consumeScrollDelta = state.anchoredDraggableState::dispatchRawDelta
    )
    Box(
        // TODO: Make this composable nestable by implementing nested scroll here as well
        modifier = Modifier.scrollable(
            state = scrollableState,
            orientation = Orientation.Vertical,
        )
    ) {
        Box(
            modifier = Modifier
                .onSizeChanged { state.expandedHeight = it.height.toFloat() },
            content = {
                headerContent()
            }
        )
        Box(
            modifier = Modifier
                .offset {
                    IntOffset(
                        x = 0,
                        y = state.anchoredDraggableState.offset.roundToInt()
                    )
                }
                .anchoredDraggable(
                    state = state.anchoredDraggableState,
                    orientation = Orientation.Vertical
                )
                .nestedScroll(
                    connection = state.anchoredDraggableState.nestedScrollConnection(),
                ),
            content = {
                body()
            }
        )
    }
}

/**
 * Packed float class to use [mutableLongStateOf] to hold state for expanded and collapsed heights.
 */
@Immutable
@JvmInline
private value class Anchors(
    val packedValue: Long,
)

private fun Anchors(
    collapsedHeight: Float,
    expandedHeight: Float,
) = Anchors(
    packFloats(
        val1 = collapsedHeight,
        val2 = expandedHeight,
    ),
)

private val Anchors.collapsedHeight
    get() = unpackFloat1(packedValue)


private val Anchors.expandedHeight
    get() = unpackFloat2(packedValue)

@OptIn(ExperimentalFoundationApi::class)
private fun AnchoredDraggableState<CollapsingHeaderStatus>.nestedScrollConnection() =
    object : NestedScrollConnection {
        override fun onPreScroll(
            available: Offset,
            source: NestedScrollSource
        ): Offset = when (val delta = available.y) {
            in -Float.MAX_VALUE..-Float.MIN_VALUE -> dispatchRawDelta(delta).toOffset()
            else -> Offset.Zero
        }

        override fun onPostScroll(
            consumed: Offset,
            available: Offset,
            source: NestedScrollSource
        ): Offset = dispatchRawDelta(delta = available.y).toOffset()

        override suspend fun onPostFling(
            consumed: Velocity,
            available: Velocity
        ): Velocity {
            settle(velocity = available.y)
            return super.onPostFling(consumed, available)
        }
    }

private fun Float.toOffset() = Offset(0f, this)
