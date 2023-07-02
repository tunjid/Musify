package com.example.musify.data.tiling

import com.example.musify.data.repositories.searchrepository.ContentQuery
import com.example.musify.data.repositories.searchrepository.itemsFor
import com.example.musify.domain.SearchResult
import com.example.musify.domain.SearchResults
import com.tunjid.tiler.PivotRequest
import com.tunjid.tiler.Tile
import com.tunjid.tiler.emptyTiledList
import com.tunjid.tiler.listTiler
import com.tunjid.tiler.toPivotedTileInputs
import com.tunjid.tiler.toTiledList
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.retry
import java.io.IOException

private const val LIMIT = 20

interface PagedQuery {
    val page: Page
}

data class Page(
    val offset: Int,
    val limit: Int = LIMIT,
)

fun <Query : PagedQuery, Item> Flow<Query>.toTiledList(
    startQuery: Query,
    queryFor: Query.(Page) -> Query,
    fetcher: suspend (Query) -> Flow<List<Item>>
) =
    toPivotedTileInputs(
        pivotRequest<Query, Item>(queryFor)
    )
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

private fun <T : PagedQuery, R> pivotRequest(
    queryFor: T.(Page) -> T
) = PivotRequest<T, R>(
    onCount = 5,
    offCount = 4,
    comparator = compareBy { it.page.offset },
    nextQuery = {
        queryFor(Page(offset = page.offset + page.limit))
    },
    previousQuery = {
        if (page.offset == 0) null
        else queryFor(Page(offset = page.offset - page.limit))
    },
)
