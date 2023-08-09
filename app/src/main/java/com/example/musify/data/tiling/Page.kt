package com.example.musify.data.tiling

import com.tunjid.tiler.PivotRequest
import com.tunjid.tiler.Tile
import com.tunjid.tiler.TiledList
import com.tunjid.tiler.emptyTiledList
import com.tunjid.tiler.listTiler
import com.tunjid.tiler.toPivotedTileInputs
import com.tunjid.tiler.toTiledList
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.retry
import java.io.IOException

private const val LIMIT = 20

/**
 * A query for items for a certain [Page]
 */
interface PagedQuery<out T> {
    val page: Page

    fun updatePage(page: Page): T
}

/**
 * An Item in a [TiledList] whose queries are [PagedQuery] instances
 */
interface PagedItem {
    val pagedIndex: Int
}

data class Page(
    val offset: Int,
    val limit: Int = LIMIT,
)

/**
 * Creates a [Flow] of [TiledList] from a network backed resource with retry semantics.
 */
fun <Query : PagedQuery<Query>, Item> Flow<Query>.toNetworkBackedTiledList(
    startQuery: Query,
    fetcher: suspend (Query) -> Flow<List<Item>>
): Flow<TiledList<Query, Item>> = toPivotedTileInputs(pivotRequest<Query, Item>())
    .toTiledList(
        listTiler(
            order = Tile.Order.PivotSorted(
                query = startQuery,
                comparator = compareBy { it.page.offset }
            ),
            limiter = Tile.Limiter(
                maxQueries = 3
            ),
            fetcher = { query ->
                fetcher(query)
                    .retry(retries = 10) { e ->
                        e.printStackTrace()
                        // retry on any IOException but also introduce delay if retrying
                        val shouldRetry = e is IOException
                        if (shouldRetry) delay(1000)
                        shouldRetry
                    }
                    .catch { emit(emptyTiledList<Query, Item>()) }
            }
        )
    )

private fun <T : PagedQuery<T>, R> pivotRequest(
) = PivotRequest<T, R>(
    onCount = 5,
    offCount = 4,
    comparator = compareBy { it.page.offset },
    nextQuery = {
        updatePage(Page(offset = page.offset + page.limit))
    },
    previousQuery = {
        if (page.offset == 0) null
        else updatePage(Page(offset = page.offset - page.limit))
    },
)

/**
 * Converts the receiving fetcher lambda to a fetcher lambda that returns placeholders immediately,
 * before fetching the original items defined by the fetcher. This facilitates draggable scrollbars
 * for asynchronously fetched items.
 * [cachedItems] items that may have existed before in case state production was stopped and later
 * resumed. This prevents overwriting items that existed before.
 * [placeholderMapper] creates interstitial placeholders while the actual items are fetched
 * [loadedMapper] Wraps the actual items after they have been fetched
 */
fun <T, Query : PagedQuery<Query>, Item : PagedItem> ((Query) -> Flow<List<T>>).withPlaceholders(
    cachedItems: TiledList<Query, Item>,
    placeholderMapper: (Int) -> Item,
    loadedMapper: (Int, T) -> Item,
): (Query) -> Flow<List<Item>> {
    val cachedQueriesToItems = mutableMapOf<Query, List<Item>>().apply {
        (0 until cachedItems.tileCount).forEach { index ->
            val tile = cachedItems.tileAt(index)
            put(
                key = cachedItems.queryAtTile(index),
                value = cachedItems.subList(tile.start, tile.end)
            )
        }
    }
    return { query ->
        // The following works by returning the placeholders immediately, so items may be scrolled,
        // after which the loaded items are then fetched asynchronously.
        flow {
            val pageIndices = (query.page.offset until query.page.offset + query.page.limit)
                .toList()
            // Return placeholders, or cached items immediately. Cached items may only be used once
            emit(
                cachedQueriesToItems.remove(query) ?: pageIndices.map(placeholderMapper)
            )
            // Fetch actual items using the same page indices as the place holders
            emitAll(
                this@withPlaceholders(query).map { items ->
                    items.mapIndexed { index, item ->
                        loadedMapper(pageIndices[index], item)
                    }
                }
            )
        }
    }
}
