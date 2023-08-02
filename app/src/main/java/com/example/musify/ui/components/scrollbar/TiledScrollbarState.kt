package com.example.musify.ui.components.scrollbar

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import com.example.musify.data.tiling.Page
import com.example.musify.data.tiling.PagedItem
import com.example.musify.data.tiling.PagedQuery
import com.tunjid.tiler.TiledList
import com.tunjid.tiler.queryAtOrNull
import kotlinx.coroutines.flow.first

/**
 * Returns a [ScrollbarState] compatible with a [TiledList] that is a sublist in a backing
 * data source with [itemsAvailable]
 */
@Composable
fun <Query : PagedQuery<Query>, Item> LazyListState.tiledListScrollbarState(
    itemsAvailable: Int,
    tiledItems: TiledList<Query, Item>,
): ScrollbarState {
    val firstQuery by rememberUpdatedState(newValue = tiledItems.queryAtOrNull(index = 0))
    return scrollbarState(
        itemsAvailable = itemsAvailable,
        itemIndex = itemIndex@{ itemInfo ->
            when (val query = firstQuery) {
                null -> return@itemIndex 0
                else -> query.page.offset + itemInfo.index
            }
        }
    )
}

/**
 * Returns a draggable scrollbar function compatible with pagination by tiling.
 * [itemsAvailable] The total amount of items that may be paged through.
 * [tiledItems] A [TiledList] representing a sublist of the total items that may paginated through
 * [onQueryChanged] A lambda to be invoked when the scrollbar has been dragged outside of what
 * the current [TiledList] sublist displays. It fetches items around the appropriate position
 */
@Composable
fun <Query : PagedQuery<Query>, Item : PagedItem> LazyListState.rememberTiledDraggableScroller(
    itemsAvailable: Int,
    tiledItems: TiledList<Query, Item>,
    onQueryChanged: (Query) -> Unit,
): (Float) -> Unit {
    var percentage by remember { mutableStateOf<Float?>(null) }
    val updatedItems by rememberUpdatedState(tiledItems)

    // Trigger the load to fetch the data required
    LaunchedEffect(percentage) {
        val currentPercentage = percentage ?: return@LaunchedEffect
        val firstQuery = updatedItems.queryAtOrNull(0) ?: return@LaunchedEffect
        val indexToFind = (itemsAvailable * currentPercentage).toInt()
        val pageToFind = Page(
            offset = indexToFind - (indexToFind % firstQuery.page.limit),
            limit = firstQuery.page.limit
        )
        // Trigger loading more if needed
        onQueryChanged(firstQuery.updatePage(pageToFind))

        // Fast path
        val fastIndex = updatedItems.binarySearch { it.pagedIndex - indexToFind }
        // Item to scroll to is in the current sublist
        if (fastIndex >= 0) return@LaunchedEffect scrollToItem(fastIndex)

        // Slow path
        val slowIndex = snapshotFlow {
            updatedItems.binarySearch { it.pagedIndex - indexToFind }
        }
            // Suspend until the item to scroll to is present in the current sublist
            .first { it >= 0 }
        scrollToItem(slowIndex)
    }
    return remember {
        { percentage = it }
    }
}
