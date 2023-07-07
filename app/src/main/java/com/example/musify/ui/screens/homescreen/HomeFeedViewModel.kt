package com.example.musify.ui.screens.homescreen

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.musify.data.repositories.homefeedrepository.HomeFeedRepository
import com.example.musify.data.repositories.homefeedrepository.ISO6391LanguageCode
import com.example.musify.data.utils.FetchedResource
import com.example.musify.di.MusifyApplication
import com.example.musify.domain.HomeFeedCarousel
import com.example.musify.domain.HomeFeedCarouselCardInfo
import com.example.musify.domain.MusifyErrorType
import com.example.musify.domain.PlaylistsForCategory
import com.example.musify.domain.SearchResult
import com.example.musify.domain.toHomeFeedCarousel
import com.example.musify.viewmodels.getCountryCode
import com.example.musify.ui.screens.homescreen.greetingphrasegenerator.GreetingPhraseGenerator
import com.tunjid.mutator.Mutation
import com.tunjid.mutator.coroutines.actionStateFlowProducer
import com.tunjid.mutator.coroutines.toMutationStream
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.merge
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
