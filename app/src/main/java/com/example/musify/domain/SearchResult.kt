package com.example.musify.domain

import android.content.Context
import com.example.musify.utils.generateMusifyDateAndDurationString

sealed class SearchResult {

    abstract val id: String

    /**
     * A class that models the result of a search operation for a
     * specific album.
     * Note: The [artistsString] property is meant to hold a comma separated
     * list of artists who worked on the album.
     */
    data class AlbumSearchResult(
        override val id: String,
        val name: String,
        val artistsString: String,
        val albumArtUrlString: String,
        val yearOfReleaseString: String,
    ) : SearchResult()

    /**
     * A class that models the result of a search operation for a
     * specific artist.
     */
    data class ArtistSearchResult(
        override val id: String,
        val name: String,
        val imageUrlString: String?
    ) : SearchResult()

    /**
     * A class that models the result of a search operation for a
     * specific playlist.
     */
    data class PlaylistSearchResult(
        override val id: String,
        val name: String,
        val ownerName: String,
        val totalNumberOfTracks: String,
        val imageUrlString: String?
    ) : SearchResult()

    /**
     * A class that models the result of a search operation for a
     * specific track.
     * Note: The [artistsString] property is meant to hold a comma separated
     * list of artists who worked on the track.
     */
    data class TrackSearchResult(
        override val id: String,
        val name: String,
        val imageUrlString: String,
        val artistsString: String,
        val trackUrlString: String?
    ) : SearchResult(), Streamable {
        override val streamInfo = StreamInfo(
            streamUrl = trackUrlString,
            imageUrl = imageUrlString,
            title = name,
            subtitle = artistsString
        )
    }

    data class PodcastSearchResult(
        override val id: String,
        val name: String,
        val nameOfPublisher: String,
        val imageUrlString: String,
    ) : SearchResult()

    data class EpisodeSearchResult(
        override val id: String,
        val episodeContentInfo: EpisodeContentInfo,
        val episodeReleaseDateInfo: EpisodeReleaseDateInfo,
        val episodeDurationInfo: EpisodeDurationInfo
    ) : SearchResult() {
        data class EpisodeContentInfo(
            val title: String,
            val description: String,
            val imageUrlString: String
        )

        data class EpisodeReleaseDateInfo(val month: String, val day: Int, val year: Int)
        data class EpisodeDurationInfo(val hours: Int, val minutes: Int)
    }
}

/**
 * A utility method used to get a string that contains date and duration
 * information in a formatted manner for an instance of
 * [SearchResult.EpisodeSearchResult].
 * @see generateMusifyDateAndDurationString
 */
fun SearchResult.EpisodeSearchResult.getFormattedDateAndDurationString(context: Context): String =
    generateMusifyDateAndDurationString(
        context = context,
        month = episodeReleaseDateInfo.month,
        day = episodeReleaseDateInfo.day,
        year = episodeReleaseDateInfo.year,
        hours = episodeDurationInfo.hours,
        minutes = episodeDurationInfo.minutes
    )