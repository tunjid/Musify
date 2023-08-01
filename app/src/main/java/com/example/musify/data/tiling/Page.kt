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
import kotlinx.coroutines.flow.retry
import java.io.IOException

private const val LIMIT = 20

interface PagedQuery<out T> {
    val page: Page

    fun updatePage(page: Page): T
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
                maxQueries = 5
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
