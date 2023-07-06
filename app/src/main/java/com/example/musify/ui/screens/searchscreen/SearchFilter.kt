package com.example.musify.ui.screens.searchscreen

/**
 * An enum that contains the different filters that can be applied to
 * the search results.
 */
enum class SearchFilter(val filterLabel: String) {
    ALBUMS("Albums"),
    TRACKS("Tracks"),
    ARTISTS("Artists"),
    PLAYLISTS("Playlists"),
    PODCASTS("Podcasts & Shows")
}
