package com.example.musify.data.repositories.tracksrepository

import androidx.paging.PagingConfig
import com.example.musify.data.remote.musicservice.SpotifyService
import com.example.musify.data.remote.response.getTracks
import com.example.musify.data.remote.response.toTrackSearchResult
import com.example.musify.data.repositories.tokenrepository.TokenRepository
import com.example.musify.data.repositories.tokenrepository.runCatchingWithToken
import com.example.musify.data.utils.FetchedResource
import com.example.musify.domain.Genre
import com.example.musify.domain.MusifyErrorType
import com.example.musify.domain.SearchResult
import com.example.musify.domain.toSupportedSpotifyGenreType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class MusifyTracksRepository @Inject constructor(
    private val tokenRepository: TokenRepository,
    private val spotifyService: SpotifyService,
    private val pagingConfig: PagingConfig
) : TracksRepository {
    override suspend fun fetchTopTenTracksForArtistWithId(
        artistId: String,
        countryCode: String
    ): FetchedResource<List<SearchResult.TrackSearchResult>, MusifyErrorType> =
        tokenRepository.runCatchingWithToken {
            spotifyService.getTopTenTracksForArtistWithId(
                artistId = artistId,
                market = countryCode,
                token = it,
            ).value.map { trackDTOWithAlbumMetadata ->
                trackDTOWithAlbumMetadata.toTrackSearchResult()
            }
        }

    override suspend fun fetchTracksForGenre(
        genre: Genre,
        countryCode: String
    ): FetchedResource<List<SearchResult.TrackSearchResult>, MusifyErrorType> =
        tokenRepository.runCatchingWithToken {
            spotifyService.getTracksForGenre(
                genre = genre.genreType.toSupportedSpotifyGenreType(),
                market = countryCode,
                token = it
            ).value.map { trackDTOWithAlbumMetadata ->
                trackDTOWithAlbumMetadata.toTrackSearchResult()
            }
        }

    override suspend fun fetchTracksForAlbumWithId(
        albumId: String,
        countryCode: String
    ): FetchedResource<List<SearchResult.TrackSearchResult>, MusifyErrorType> =
        tokenRepository.runCatchingWithToken {
            spotifyService.getAlbumWithId(albumId, countryCode, it).getTracks()
        }

    override fun playListsFor(
        playListQuery: PlaylistQuery
    ): Flow<List<SearchResult.TrackSearchResult>> = flow {
        emit(
            spotifyService.getTracksForPlaylist(
                playlistId = playListQuery.id,
                market = playListQuery.countryCode,
                token = tokenRepository.getValidBearerToken(),
                limit = playListQuery.page.limit,
                offset = playListQuery.page.offset
            ).items.map { it.track.toTrackSearchResult() }
        )
    }
}