package com.example.musify.ui.activities

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.musify.musicplayer.MusicPlayerV2
import com.example.musify.usecases.downloadDrawableFromUrlUseCase.DownloadDrawableFromUrlUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class PlaybackViewModel @Inject constructor(
    application: Application,
    musicPlayer: MusicPlayerV2,
    downloadDrawableFromUrlUseCase: DownloadDrawableFromUrlUseCase
) : AndroidViewModel(application) {
    private val stateProducer =
        viewModelScope.playbackStateProducer(
            context = getApplication(),
            musicPlayer = musicPlayer,
            downloadDrawableFromUrlUseCase = downloadDrawableFromUrlUseCase,
        )

    val state = stateProducer.state
    val actions = stateProducer.accept
}
