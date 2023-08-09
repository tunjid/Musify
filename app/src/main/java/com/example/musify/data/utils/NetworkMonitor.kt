package com.example.musify.data.utils

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest

interface NetworkMonitor {
    val isOnline: Flow<Boolean>
}

fun NetworkMonitor.onConnected() = isOnline
    // Cancel work when offline
    .mapLatest { it }
    // Notify when online
    .filter { it }
    // Wait 1s before notifying
    .debounce(1000)
    // Emit nothing
    .map { }