package com.example.musify.ui.activities

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.musify.musicplayer.MusicPlayerV2
import com.example.musify.usecases.downloadDrawableFromUrlUseCase.DownloadDrawableFromUrlUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class PlaybackViewModel @Inject constructor(
    musicPlayer: MusicPlayerV2,
    downloadDrawableFromUrlUseCase: DownloadDrawableFromUrlUseCase
) : ViewModel() {
    private val stateProducer =
        viewModelScope.playbackStateProducer(
            musicPlayer = musicPlayer,
            downloadDrawableFromUrlUseCase = downloadDrawableFromUrlUseCase,
        )

    val state = stateProducer.state
    val actions = stateProducer.accept
}
