package com.example.musify.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NamedNavArgument
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.navigation.navigation
import com.example.musify.R
import com.example.musify.domain.PodcastEpisode
import com.example.musify.domain.SearchResult
import com.example.musify.domain.Streamable
import com.example.musify.ui.components.DefaultMusifyErrorMessage
import com.example.musify.ui.components.DefaultMusifyLoadingAnimation
import com.example.musify.ui.screens.detailscreens.AlbumDetailScreen
import com.example.musify.ui.screens.detailscreens.ArtistDetailScreen
import com.example.musify.ui.screens.detailscreens.PlaylistDetailScreen
import com.example.musify.ui.screens.detailscreens.PodcastEpisodeDetailScreen
import com.example.musify.ui.screens.podcastshowdetailscreen.PodcastShowDetailScreen
import com.example.musify.viewmodels.AlbumDetailLoadingState
import com.example.musify.viewmodels.AlbumDetailViewModel
import com.example.musify.viewmodels.PlaylistDetailAction
import com.example.musify.viewmodels.PlaylistDetailViewModel
import com.example.musify.viewmodels.PodcastEpisodeAction
import com.example.musify.viewmodels.PodcastEpisodeDetailViewModel
import com.example.musify.viewmodels.PodcastShowDetailAction
import com.example.musify.viewmodels.PodcastShowDetailViewModel
import com.example.musify.viewmodels.artistviewmodel.ArtistDetailAction
import com.example.musify.viewmodels.artistviewmodel.ArtistDetailScreenLoadingState
import com.example.musify.viewmodels.artistviewmodel.ArtistDetailViewModel
import com.example.musify.viewmodels.isEpisodeCurrentlyPlaying

/**
 * A nested navigation graph that consists of detail screens.
 *
 * It uses [prefixedWithRouteOfNavGraphRoute] for the nested destinations.
 * For information on why [prefixedWithRouteOfNavGraphRoute] see
 * docs of [NavGraphWithDetailScreensNestedController] class.
 *
 * @param navGraphRoute the destination's unique route
 * @param navController the nav controller to be associated with the nav graph.
 * @param startDestination the route for the start destination.
 * @param playStreamable lambda to execute when a [Streamable] is to be played.
 * @param builder the builder used to define other composables that belong
 * to this nested graph.
 * @see NavGraphBuilder.artistDetailScreen
 * @see NavGraphBuilder.albumDetailScreen
 * @see NavGraphBuilder.playlistDetailScreen
 */
@ExperimentalMaterialApi
fun NavGraphBuilder.navGraphWithDetailScreens(
    navGraphRoute: String,
    navController: NavHostController,
    playStreamable: (Streamable) -> Unit,
    onPausePlayback: () -> Unit,
    startDestination: String,
    builder: NavGraphBuilder.(nestedController: NavGraphWithDetailScreensNestedController) -> Unit
) {
    val onBackButtonClicked = {
        navController.popBackStack()
        Unit // Need to specify explicitly inorder to avoid compilation errors
    }
    val nestedController = NavGraphWithDetailScreensNestedController(
        navController = navController,
        associatedNavGraphRoute = navGraphRoute,
        playTrack = playStreamable
    )
    navigation(
        route = navGraphRoute,
        startDestination = startDestination
    ) {
        builder(nestedController)
        artistDetailScreen(
            route = MusifyNavigationDestinations
                .ArtistDetailScreen
                .prefixedWithRouteOfNavGraphRoute(navGraphRoute),
            arguments = listOf(
                navArgument(MusifyNavigationDestinations.ArtistDetailScreen.NAV_ARG_ENCODED_IMAGE_URL_STRING) {
                    nullable = true
                }
            ),
            onBackButtonClicked = onBackButtonClicked,
            onAlbumClicked = nestedController::navigateToDetailScreen,
            onPlayTrack = playStreamable
        )
        albumDetailScreen(
            route = MusifyNavigationDestinations
                .AlbumDetailScreen
                .prefixedWithRouteOfNavGraphRoute(navGraphRoute),
            onBackButtonClicked = onBackButtonClicked,
            onPlayTrack = playStreamable
        )
        playlistDetailScreen(
            route = MusifyNavigationDestinations
                .PlaylistDetailScreen
                .prefixedWithRouteOfNavGraphRoute(navGraphRoute),
            onBackButtonClicked = onBackButtonClicked,
            onPlayTrack = playStreamable
        )
        podcastEpisodeDetailScreen(
            route = MusifyNavigationDestinations
                .PodcastEpisodeDetailScreen
                .prefixedWithRouteOfNavGraphRoute(navGraphRoute),
            onBackButtonClicked = onBackButtonClicked,
            onPlayButtonClicked = playStreamable,
            onPauseButtonClicked = onPausePlayback,
            navigateToPodcastShowDetailScreen = nestedController::navigateToDetailScreen
        )

        podcastShowDetailScreen(
            route = MusifyNavigationDestinations
                .PodcastShowDetailScreen
                .prefixedWithRouteOfNavGraphRoute(navGraphRoute),
            onEpisodePlayButtonClicked = playStreamable,
            onEpisodePauseButtonClicked = { onPausePlayback() },
            onEpisodeClicked = playStreamable,
            onBackButtonClicked = onBackButtonClicked
        )

    }
}

@ExperimentalMaterialApi
private fun NavGraphBuilder.artistDetailScreen(
    route: String,
    onBackButtonClicked: () -> Unit,
    onPlayTrack: (SearchResult.TrackSearchResult) -> Unit,
    onAlbumClicked: (SearchResult.AlbumSearchResult) -> Unit,
    arguments: List<NamedNavArgument> = emptyList()
) {
    composable(route, arguments) { backStackEntry ->
        val viewModel = hiltViewModel<ArtistDetailViewModel>(backStackEntry)
        val state by viewModel.state.collectAsState()
        val actions = remember { viewModel.actions }

        ArtistDetailScreen(
            artistName = state.artistName,
            artistImageUrlString = state.artistImageUrlString,
            popularTracks = state.popularTracks,
            releases = state.releases,
            onQueryChanged = { actions(ArtistDetailAction.LoadAround(it)) },
            currentlyPlayingTrack = state.currentlyPlayingTrack,
            onBackButtonClicked = onBackButtonClicked,
            onPlayButtonClicked = {},
            onTrackClicked = onPlayTrack,
            onAlbumClicked = onAlbumClicked,
            isLoading = state.loadingState is ArtistDetailScreenLoadingState.Loading,
            fallbackImageRes = R.drawable.ic_outline_account_circle_24,
            isErrorMessageVisible = state.loadingState is ArtistDetailScreenLoadingState.Error
        )
    }
}

@ExperimentalMaterialApi
private fun NavGraphBuilder.albumDetailScreen(
    route: String,
    onBackButtonClicked: () -> Unit,
    onPlayTrack: (SearchResult.TrackSearchResult) -> Unit
) {
    composable(route) {
        val viewModel = hiltViewModel<AlbumDetailViewModel>()
        val state by viewModel.state.collectAsState()

        AlbumDetailScreen(
            albumName = state.albumName,
            artistsString = state.artists,
            yearOfRelease = state.yearOfRelease,
            albumArtUrlString = state.albumArtUrl,
            trackList = state.tracks,
            onTrackItemClick = onPlayTrack,
            onBackButtonClicked = onBackButtonClicked,
            isLoading = state.loadingState is AlbumDetailLoadingState.Loading,
            isErrorMessageVisible = state.loadingState is AlbumDetailLoadingState.Error,
            currentlyPlayingTrack = state.currentlyPlayingTrack
        )
    }
}

@ExperimentalMaterialApi
private fun NavGraphBuilder.playlistDetailScreen(
    route: String,
    onBackButtonClicked: () -> Unit,
    onPlayTrack: (SearchResult.TrackSearchResult) -> Unit,
    navigationArguments: List<NamedNavArgument> = emptyList()
) {
    composable(route = route, arguments = navigationArguments) {
        val viewModel = hiltViewModel<PlaylistDetailViewModel>()
        val state by viewModel.state.collectAsState()
        val actions = remember { viewModel.actions }

        PlaylistDetailScreen(
            playlistName = state.playlistName,
            playlistImageUrlString = state.imageUrlString,
            nameOfPlaylistOwner = state.ownerName,
            totalNumberOfTracks = state.totalNumberOfTracks,
            imageResToUseWhenImageUrlStringIsNull = R.drawable.ic_outline_account_circle_24,
            tracks = state.tracks,
            onQueryChanged = { actions(PlaylistDetailAction.LoadAround(it)) },
            currentlyPlayingTrack = state.currentlyPlayingTrack,
            onBackButtonClicked = onBackButtonClicked,
            onTrackClicked = onPlayTrack,
        )
    }
}

/**
 * A class that acts a controller that is used to navigate within
 * destinations defined in [NavGraphBuilder.navGraphWithDetailScreens].
 *
 * Navigation component doesn't work deterministically when the same
 * nested graph is used more than once in the same graph. Since the
 * same destinations defined in [NavGraphBuilder.navGraphWithDetailScreens] are
 * reused (with the same routes) multiple times within the same graph,
 * navigation component chooses the destination that appears in the first call
 * to [NavGraphBuilder.navGraphWithDetailScreens] when ever the client
 * chooses to navigate to one of the screens defined in
 * [NavGraphBuilder.navGraphWithDetailScreens].
 * Eg:
 * Let's assume that NavGraphBuilder.navGraphWithDetailScreens has an artist
 * and album detail screen.
 * ```
 * NavHost(...){
 *
 *      // (1) contains detail screens
 *      navGraphWithDetailScreens(){
 *         /* Other composable destinations */
 *      }
 *
 *      // (2) contains the same detail screens as (1)
 *      navGraphWithDetailScreens(){
 *         /* Other composable destinations */
 *      }
 * }
 *```
 * When the client wants to navigate to a detail screen (lets take album detail
 * screen for example), then, the navigation component will navigate to the
 * album detail screen defined in (1) and not the detail screen defined in (2)
 * even if the client is navigating from one of the composable destinations defined
 * in the second call since the route strings for the detail screens are the same in
 * both graphs ((1) and (2)). This results in navigating to a destination that has an
 * unexpected parent navGraph. In order to avoid this, the destinations defined
 * in [NavGraphBuilder.navGraphWithDetailScreens] are prefixed with the route
 * of the said navGraph using [prefixedWithRouteOfNavGraphRoute]. The
 * [NavGraphWithDetailScreensNestedController.navigateToDetailScreen]
 * prefixes [associatedNavGraphRoute] before navigating in-order to accommodate
 * for this.
 */
class NavGraphWithDetailScreensNestedController(
    private val navController: NavHostController,
    private val associatedNavGraphRoute: String,
    private val playTrack: (SearchResult.TrackSearchResult) -> Unit
) {
    fun navigateToDetailScreen(podcastEpisode: PodcastEpisode) {
        val route = MusifyNavigationDestinations
            .PodcastShowDetailScreen
            .buildRoute(podcastEpisode.podcastShowInfo.id)
        navController.navigate(associatedNavGraphRoute + route) { launchSingleTop = true }
    }

    fun navigateToDetailScreen(searchResult: SearchResult) {
        val route = when (searchResult) {
            is SearchResult.AlbumSearchResult -> MusifyNavigationDestinations
                .AlbumDetailScreen
                .buildRoute(searchResult)

            is SearchResult.ArtistSearchResult -> MusifyNavigationDestinations
                .ArtistDetailScreen
                .buildRoute(searchResult)

            is SearchResult.PlaylistSearchResult -> MusifyNavigationDestinations
                .PlaylistDetailScreen
                .buildRoute(searchResult)

            is SearchResult.TrackSearchResult -> {
                playTrack(searchResult)
                return
            }

            is SearchResult.PodcastSearchResult -> {
                MusifyNavigationDestinations.PodcastShowDetailScreen.buildRoute(searchResult.id)
            }

            is SearchResult.EpisodeSearchResult -> {
                MusifyNavigationDestinations.PodcastEpisodeDetailScreen.buildRoute(searchResult.id)
            }
        }
        navController.navigate(associatedNavGraphRoute + route)
    }
}

private fun NavGraphBuilder.podcastEpisodeDetailScreen(
    route: String,
    onPlayButtonClicked: (PodcastEpisode) -> Unit,
    onPauseButtonClicked: () -> Unit,
    onBackButtonClicked: () -> Unit,
    navigateToPodcastShowDetailScreen: (PodcastEpisode) -> Unit
) {
    composable(route = route) {
        val viewModel = hiltViewModel<PodcastEpisodeDetailViewModel>()
        val state by viewModel.state.collectAsState()
        val actions = remember { viewModel.actions }

        when (val podcastEpisode = state.podcastEpisode) {
            null -> {
                Box(modifier = Modifier.fillMaxSize()) {
                    if (state.loadingState == PodcastEpisodeDetailViewModel.LoadingState.LOADING) {
                        DefaultMusifyLoadingAnimation(
                            modifier = Modifier.align(Alignment.Center),
                            isVisible = true
                        )
                    }
                    if (state.loadingState == PodcastEpisodeDetailViewModel.LoadingState.ERROR) {
                        DefaultMusifyErrorMessage(
                            modifier = Modifier.align(Alignment.Center),
                            title = "Oops! Something doesn't look right",
                            subtitle = "Please check the internet connection",
                            onRetryButtonClicked = { actions(PodcastEpisodeAction.Retry) }
                        )
                    }
                }
            }

            else -> {
                PodcastEpisodeDetailScreen(
                    podcastEpisode = podcastEpisode,
                    isEpisodeCurrentlyPlaying = state.isEpisodeCurrentlyPlaying,
                    isPlaybackLoading = state.loadingState == PodcastEpisodeDetailViewModel.LoadingState.PLAYBACK_LOADING,
                    onPlayButtonClicked = {
                        onPlayButtonClicked(podcastEpisode)
                    },
                    onPauseButtonClicked = { onPauseButtonClicked() },
                    onShareButtonClicked = {},
                    onAddButtonClicked = {},
                    onDownloadButtonClicked = {},
                    onBackButtonClicked = onBackButtonClicked,
                    navigateToPodcastDetailScreen = {
                        state.podcastEpisode?.let { navigateToPodcastShowDetailScreen(it) }
                    }
                )
            }
        }
    }
}

@ExperimentalMaterialApi
private fun NavGraphBuilder.podcastShowDetailScreen(
    route: String,
    onEpisodePlayButtonClicked: (PodcastEpisode) -> Unit,
    onEpisodePauseButtonClicked: (PodcastEpisode) -> Unit,
    onEpisodeClicked: (PodcastEpisode) -> Unit,
    onBackButtonClicked: () -> Unit
) {
    composable(route = route) {
        val viewModel = hiltViewModel<PodcastShowDetailViewModel>()
        val state by viewModel.state.collectAsState()
        val actions = remember { viewModel.actions }
        val episodesForShow = state.episodesForShow

        if (state.podcastShow == null) {
            Box(modifier = Modifier.fillMaxSize()) {
                if (state.loadingState == PodcastShowDetailViewModel.LoadingState.LOADING) {
                    DefaultMusifyLoadingAnimation(
                        modifier = Modifier.align(Alignment.Center),
                        isVisible = true
                    )
                }
                if (state.loadingState == PodcastShowDetailViewModel.LoadingState.ERROR) {
                    DefaultMusifyErrorMessage(
                        modifier = Modifier.align(Alignment.Center),
                        title = "Oops! Something doesn't look right",
                        subtitle = "Please check the internet connection",
                        onRetryButtonClicked = { actions(PodcastShowDetailAction.Retry) }
                    )
                }
            }
        } else {
            PodcastShowDetailScreen(
                podcastShow = state.podcastShow!!,
                onBackButtonClicked = onBackButtonClicked,
                onEpisodePlayButtonClicked = onEpisodePlayButtonClicked,
                onEpisodePauseButtonClicked = onEpisodePauseButtonClicked,
                currentlyPlayingEpisode = state.currentlyPlayingEpisode,
                isCurrentlyPlayingEpisodePaused = state.isCurrentlyPlayingEpisodePaused,
                isPlaybackLoading = state.loadingState == PodcastShowDetailViewModel.LoadingState.PLAYBACK_LOADING,
                onEpisodeClicked = onEpisodeClicked,
                episodes = episodesForShow,
                onQueryChanged = { actions(PodcastShowDetailAction.LoadAround(it)) },
            )
        }
    }
}

/**
 * A utility function that appends the [routeOfNavGraph] to [MusifyNavigationDestinations.route]
 * as prefix. See docs of [NavGraphWithDetailScreensNestedController] for more information.
 */
private fun MusifyNavigationDestinations.prefixedWithRouteOfNavGraphRoute(routeOfNavGraph: String) =
    routeOfNavGraph + this.route

