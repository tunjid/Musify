package com.example.musify.ui.screens.homescreen

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.musify.data.repositories.homefeedrepository.HomeFeedRepository
import com.example.musify.data.repositories.homefeedrepository.ISO6391LanguageCode
import com.example.musify.di.MusifyApplication
import com.example.musify.ui.screens.homescreen.greetingphrasegenerator.GreetingPhraseGenerator
import com.example.musify.viewmodels.getCountryCode
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject


@HiltViewModel
class HomeFeedViewModel @Inject constructor(
    application: Application,
    greetingPhraseGenerator: GreetingPhraseGenerator,
    homeFeedRepository: HomeFeedRepository,
) : AndroidViewModel(application) {
    private val stateProducer =
        viewModelScope.homeScreenStateProducer(
            countryCode = getCountryCode(),
            languageCode = getApplication<MusifyApplication>().resources
                .configuration
                .locale
                .language
                .let(::ISO6391LanguageCode),
            greetingPhraseGenerator = greetingPhraseGenerator,
            homeFeedRepository = homeFeedRepository
        )

    val state = stateProducer.state
    val actions = stateProducer.accept
}
