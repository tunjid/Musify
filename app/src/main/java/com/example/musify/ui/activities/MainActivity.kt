package com.example.musify.ui.activities

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.MaterialTheme
import androidx.compose.material.SnackbarHost
import androidx.compose.material.SnackbarHostState
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.rememberNavController
import com.example.musify.ui.navigation.MusifyBottomNavigationConnectedWithBackStack
import com.example.musify.ui.navigation.MusifyBottomNavigationDestinations
import com.example.musify.ui.navigation.MusifyNavigation
import com.example.musify.ui.screens.homescreen.ExpandableMiniPlayerWithSnackbar
import com.example.musify.ui.theme.MusifyTheme
import dagger.hilt.android.AndroidEntryPoint

@ExperimentalAnimationApi
@ExperimentalMaterialApi
@ExperimentalComposeUiApi
@ExperimentalFoundationApi
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        setContent {
            MusifyTheme {
                Surface(modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colors.background,
                    content = { MusifyApp() })
            }
        }
    }
}

@ExperimentalAnimationApi
@ExperimentalFoundationApi
@ExperimentalComposeUiApi
@ExperimentalMaterialApi
@Composable
private fun MusifyApp() {
    val playbackViewModel = hiltViewModel<PlaybackViewModel>()
    val state by playbackViewModel.state.collectAsState()
    val actions = remember { playbackViewModel.actions }

    val snackbarHostState = remember { SnackbarHostState() }


    val miniPlayerStreamable = state.playbackState.currentlyPlayingStreamable
        ?: state.playbackState.previouslyPlayingStreamable
    var isNowPlayingScreenVisible by remember { mutableStateOf(false) }
    val playbackState by remember {
        derivedStateOf { state.playbackState }
    }

    LaunchedEffect(key1 = playbackState) {
        when (val currentPlaybackState = playbackState) {
            is PlaybackViewModel.PlaybackState.Error -> {
                snackbarHostState.currentSnackbarData?.dismiss()
                snackbarHostState.showSnackbar(
                    message = currentPlaybackState.errorMessage,
                )
            }

            else -> Unit
        }
    }
    val isPlaybackPaused = state.playbackState is PlaybackViewModel.PlaybackState.Paused
            || state.playbackState is PlaybackViewModel.PlaybackState.PlaybackEnded

    BackHandler(isNowPlayingScreenVisible) {
        isNowPlayingScreenVisible = false
    }
    val bottomNavigationItems = remember {
        listOf(
            MusifyBottomNavigationDestinations.Home,
            MusifyBottomNavigationDestinations.Search,
            MusifyBottomNavigationDestinations.Premium
        )
    }
    val navController = rememberNavController()
    Box(modifier = Modifier.fillMaxSize()) {
        // the playbackState.currentlyPlayingTrack will automatically be set
        // to null when the playback is stopped
        MusifyNavigation(
            navController = navController,
            playStreamable = { actions(PlaybackScreenAction.Play(it)) },
            isFullScreenNowPlayingOverlayScreenVisible = isNowPlayingScreenVisible,
            onPausePlayback = { actions(PlaybackScreenAction.Pause) }
        )
        Column(modifier = Modifier.align(Alignment.BottomCenter)) {
            AnimatedContent(
                modifier = Modifier.fillMaxWidth(),
                targetState = miniPlayerStreamable
            ) { streamable ->
                if (streamable == null) {
                    SnackbarHost(hostState = snackbarHostState)
                } else {
                    ExpandableMiniPlayerWithSnackbar(
                        modifier = Modifier
                            .animateEnterExit(
                                enter = fadeIn() + slideInVertically { it },
                                exit = fadeOut() + slideOutVertically { -it }
                            ),
                        streamable = miniPlayerStreamable!!,
                        onPauseButtonClicked = { actions(PlaybackScreenAction.Pause) },
                        onPlayButtonClicked = { actions(PlaybackScreenAction.Toggle(it)) },
                        isPlaybackPaused = isPlaybackPaused,
                        timeElapsedString = state.currentTrackProgressText,
                        playbackProgress = state.currentTrackProgress,
                        totalDurationOfCurrentTrackText = state.totalDurationOfCurrentTrackTimeText,
                        snackbarHostState = snackbarHostState
                    )
                }
            }

            MusifyBottomNavigationConnectedWithBackStack(
                navController = navController,
                modifier = Modifier.navigationBarsPadding(),
                navigationItems = bottomNavigationItems,
            )
        }
    }
}

