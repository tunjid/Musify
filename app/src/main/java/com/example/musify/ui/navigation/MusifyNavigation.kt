package com.example.musify.ui.navigation

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.musify.domain.HomeFeedCarouselCardInfo
import com.example.musify.domain.HomeFeedFilters
import com.example.musify.domain.SearchResult
import com.example.musify.domain.Streamable
import com.example.musify.ui.dynamicTheme.dynamicbackgroundmodifier.DynamicBackgroundResource
import com.example.musify.ui.dynamicTheme.dynamicbackgroundmodifier.dynamicBackground
import com.example.musify.ui.screens.GetPremiumScreen
import com.example.musify.ui.screens.homescreen.HomeScreen
import com.example.musify.ui.screens.searchscreen.SearchScreen
import com.example.musify.viewmodels.homefeedviewmodel.HomeAction
import com.example.musify.viewmodels.homefeedviewmodel.HomeFeedViewModel
import com.example.musify.viewmodels.searchviewmodel.SearchAction
import com.example.musify.viewmodels.searchviewmodel.SearchFilter
import com.example.musify.viewmodels.searchviewmodel.SearchViewModel

@ExperimentalAnimationApi
@ExperimentalMaterialApi
@ExperimentalComposeUiApi
@ExperimentalFoundationApi
@Composable
fun MusifyNavigation(
    navController: NavHostController,
    playStreamable: (Streamable) -> Unit,
    onPausePlayback: () -> Unit,
    isFullScreenNowPlayingOverlayScreenVisible: Boolean
) {
    NavHost(
        navController = navController,
        startDestination = MusifyBottomNavigationDestinations.Home.route
    ) {
        navGraphWithDetailScreens(
            navGraphRoute = MusifyBottomNavigationDestinations.Home.route,
            startDestination = MusifyNavigationDestinations.HomeScreen.route,
            navController = navController,
            playStreamable = playStreamable,
            onPausePlayback = onPausePlayback
        ) { nestedController ->
            homeScreen(
                route = MusifyNavigationDestinations.HomeScreen.route,
                onCarouselCardClicked = {
                    nestedController.navigateToDetailScreen(searchResult = it.associatedSearchResult)
                }
            )
        }
        navGraphWithDetailScreens(
            navGraphRoute = MusifyBottomNavigationDestinations.Search.route,
            startDestination = MusifyNavigationDestinations.SearchScreen.route,
            navController = navController,
            playStreamable = playStreamable,
            onPausePlayback = onPausePlayback
        ) { nestedController ->
            searchScreen(
                route = MusifyNavigationDestinations.SearchScreen.route,
                onSearchResultClicked = nestedController::navigateToDetailScreen,
                isFullScreenNowPlayingScreenOverlayVisible = isFullScreenNowPlayingOverlayScreenVisible
            )
        }

        composable(MusifyBottomNavigationDestinations.Premium.route) {
            GetPremiumScreen()
        }
    }
}

@ExperimentalMaterialApi
@ExperimentalFoundationApi
private fun NavGraphBuilder.homeScreen(
    route: String,
    onCarouselCardClicked: (HomeFeedCarouselCardInfo) -> Unit
) {
    composable(route) {
        val homeFeedViewModel = hiltViewModel<HomeFeedViewModel>()
        val state by homeFeedViewModel.state.collectAsState()
        val actions = remember { homeFeedViewModel.actions }
        val filters = remember {
            listOf(
                HomeFeedFilters.Music,
                HomeFeedFilters.PodcastsAndShows
            )
        }
        HomeScreen(
            timeBasedGreeting = state.greetingPhrase,
            homeFeedFilters = filters,
            currentlySelectedHomeFeedFilter = HomeFeedFilters.None,
            onHomeFeedFilterClick = {},
            carousels = state.homeFeedCarousels,
            onHomeFeedCarouselCardClick = onCarouselCardClicked,
            isErrorMessageVisible = state.loadingState == HomeFeedViewModel.HomeFeedLoadingState.ERROR,
            isLoading = state.loadingState == HomeFeedViewModel.HomeFeedLoadingState.LOADING,
            onErrorRetryButtonClick = { actions(HomeAction.Retry) }
        )
    }
}

@ExperimentalAnimationApi
@ExperimentalFoundationApi
@ExperimentalComposeUiApi
@ExperimentalMaterialApi
private fun NavGraphBuilder.searchScreen(
    route: String,
    onSearchResultClicked: (SearchResult) -> Unit,
    isFullScreenNowPlayingScreenOverlayVisible: Boolean,
) {
    composable(route = route) {
        val viewModel = hiltViewModel<SearchViewModel>()
        val state by viewModel.state.collectAsState()
        val actions = remember { viewModel.actions }
        val controller = LocalSoftwareKeyboardController.current
        val filters = remember { SearchFilter.values().toList() }
        val dynamicBackgroundResource by remember {
            derivedStateOf {
                val imageUrl = when (state.selectedSearchFilter) {
                    SearchFilter.ALBUMS -> state.tiledItems.albumTiledListFlow.value.firstOrNull()?.albumArtUrlString
                    SearchFilter.TRACKS -> state.tiledItems.trackTiledListFlow.value.firstOrNull()?.imageUrlString
                    SearchFilter.ARTISTS -> state.tiledItems.artistTiledListFLow.value.firstOrNull()?.imageUrlString
                    SearchFilter.PLAYLISTS -> state.tiledItems.playlistTiledListFlow.value.firstOrNull()?.imageUrlString
                    SearchFilter.PODCASTS -> state.tiledItems.podcastTiledListFlow.value.firstOrNull()?.imageUrlString
                }
                if (imageUrl == null) DynamicBackgroundResource.Empty
                else DynamicBackgroundResource.FromImageUrl(imageUrl)
            }
        }
        Box(modifier = Modifier.dynamicBackground(dynamicBackgroundResource)) {
            SearchScreen(
                isOnline = state.isOnline,
                genreList = state.genres,
                searchScreenFilters = filters,
                tiledListFlows = state.tiledItems,
                currentlyPlayingTrack = state.currentlyPlayingTrack,
                currentlySelectedFilter = state.selectedSearchFilter,
                onQueryChanged = { actions(SearchAction.Searches.LoadAround(it)) },
                onSearchFilterChanged = { actions(SearchAction.SearchFilterChange(it)) },
                onGenreItemClick = {},
                onSearchTextChanged = { actions(SearchAction.Searches.Search(it)) },
                onSearchQueryItemClicked = onSearchResultClicked,
                onImeDoneButtonClicked = {
                    controller?.hide()
                },
                isFullScreenNowPlayingOverlayScreenVisible = isFullScreenNowPlayingScreenOverlayVisible
            )
        }
    }
}
