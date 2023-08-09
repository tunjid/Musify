package com.example.musify.domain

import com.example.musify.domain.SearchResult.AlbumSearchResult
import com.example.musify.domain.SearchResult.ArtistSearchResult
import com.example.musify.domain.SearchResult.EpisodeSearchResult
import com.example.musify.domain.SearchResult.PlaylistSearchResult
import com.example.musify.domain.SearchResult.PodcastSearchResult
import com.example.musify.domain.SearchResult.TrackSearchResult

/**
 * A class that models a search result. It contains all the [tracks],
 * [albums],[artists] and [playlists] that matched a search query.
 */
data class SearchResults(
    val tracks: List<TrackSearchResult>,
    val albums: List<AlbumSearchResult>,
    val artists: List<ArtistSearchResult>,
    val playlists: List<PlaylistSearchResult>,
    val shows: List<PodcastSearchResult>,
    val episodes: List<EpisodeSearchResult>,
)

/**
 * A utility function that returns an instance of  [SearchResults] with
 * empty read-only lists.
 */
fun emptySearchResults() = SearchResults(
    tracks = emptyList(),
    albums = emptyList(),
    artists = emptyList(),
    playlists = emptyList(),
    shows = emptyList(),
    episodes = emptyList()
)
