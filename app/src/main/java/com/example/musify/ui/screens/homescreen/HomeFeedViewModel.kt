package com.example.musify.ui.screens.homescreen

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.musify.data.repositories.homefeedrepository.HomeFeedRepository
import com.example.musify.data.repositories.homefeedrepository.ISO6391LanguageCode
import com.example.musify.data.utils.NetworkMonitor
import com.example.musify.ui.screens.homescreen.greetingphrasegenerator.GreetingPhraseGenerator
import com.example.musify.utils.countryCode
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

@HiltViewModel
class HomeFeedViewModel @Inject constructor(
    @ApplicationContext context: Context,
    networkMonitor: NetworkMonitor,
    greetingPhraseGenerator: GreetingPhraseGenerator,
    homeFeedRepository: HomeFeedRepository,
) : ViewModel() {
    private val stateProducer =
        viewModelScope.homeScreenStateProducer(
            countryCode = context.countryCode,
            languageCode = context.resources
                .configuration
                .locale
                .language
                .let(::ISO6391LanguageCode),
            networkMonitor = networkMonitor,
            greetingPhraseGenerator = greetingPhraseGenerator,
            homeFeedRepository = homeFeedRepository
        )

    val state = stateProducer.state
    val actions = stateProducer.accept
}
