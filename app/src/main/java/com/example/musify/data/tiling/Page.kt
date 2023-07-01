package com.example.musify.data.tiling

import com.example.musify.domain.SearchResult
import com.tunjid.tiler.PivotRequest

private const val LIMIT = 20

data class Page(
    val offset: Int,
    val limit: Int = LIMIT,
)
