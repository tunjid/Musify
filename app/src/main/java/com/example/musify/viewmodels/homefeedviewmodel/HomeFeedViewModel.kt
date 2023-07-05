package com.example.musify.viewmodels.homefeedviewmodel

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
import com.example.musify.viewmodels.homefeedviewmodel.greetingphrasegenerator.GreetingPhraseGenerator
import com.tunjid.mutator.Mutation
import com.tunjid.mutator.coroutines.actionStateFlowProducer
import com.tunjid.mutator.coroutines.toMutationStream
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.merge
import javax.inject.Inject

data class HomeState(
    val loadingState: HomeFeedViewModel.HomeFeedLoadingState = HomeFeedViewModel.HomeFeedLoadingState.LOADING,
    val homeFeedCarousels: List<HomeFeedCarousel> = emptyList(),
    val greetingPhrase: String,
)

sealed class HomeAction {
    object Retry : HomeAction()
}

@HiltViewModel
class HomeFeedViewModel @Inject constructor(
    application: Application,
    greetingPhraseGenerator: GreetingPhraseGenerator,
    homeFeedRepository: HomeFeedRepository,
) : AndroidViewModel(application) {

    private val languageCode =
        getApplication<MusifyApplication>().resources
            .configuration
            .locale
            .language
            .let(::ISO6391LanguageCode)

    private val initialLoad = listOf(
        homeFeedRepository.albumReleaseMutations(
            countryCode = getCountryCode()
        ),
        homeFeedRepository.featuredPlaylistMutations(
            languageCode = languageCode,
            countryCode = getCountryCode(),
        ),
        homeFeedRepository.playlistCategoryMutations(
            languageCode = languageCode,
            countryCode = getCountryCode(),
        ),
    )

    private val stateProducer =
        viewModelScope.actionStateFlowProducer<HomeAction, HomeState>(
            initialState = HomeState(
                greetingPhrase = greetingPhraseGenerator.generatePhrase()
            ),
            mutationFlows = initialLoad,
            actionTransform = { actions ->
                actions.toMutationStream {
                    when (val action = type()) {
                        is HomeAction.Retry -> action.flow.flatMapLatest {
                            initialLoad.merge()
                        }
                    }
                }
            }
        )

    val state = stateProducer.state
    val actions = stateProducer.accept

    /**
     * An enum class that contains the different UI states associated
     * with a screen that displays the home feed.
     */
    enum class HomeFeedLoadingState { IDLE, LOADING, ERROR }
}

private fun HomeFeedRepository.playlistCategoryMutations(
    countryCode: String,
    languageCode: ISO6391LanguageCode
) = flow {
    emit(
        fetchPlaylistsBasedOnCategoriesAvailableForCountry(
            countryCode = countryCode, languageCode = languageCode
        )
            .dataOrNull()
            ?.map(PlaylistsForCategory::toHomeFeedCarousel)
            .toMutation()
    )
}

private fun HomeFeedRepository.featuredPlaylistMutations(
    countryCode: String,
    languageCode: ISO6391LanguageCode
) = flow {
    emit(
        fetchFeaturedPlaylistsForCurrentTimeStamp(
            timestampMillis = System.currentTimeMillis(),
            countryCode = countryCode,
            languageCode = languageCode
        )
            .dataOrNull()
            ?.playlists
            ?.map<SearchResult, HomeFeedCarouselCardInfo>(::toHomeFeedCarouselCardInfo)
            ?.let { homeFeedCarouselCardInfoList ->
                listOf(
                    HomeFeedCarousel(
                        id = "Featured Playlists",
                        title = "Featured Playlists",
                        associatedCards = homeFeedCarouselCardInfoList
                    )
                )
            }.toMutation()
    )
}

private fun HomeFeedRepository.albumReleaseMutations(
    countryCode: String
) = flow {
    emit(
        fetchNewlyReleasedAlbums(countryCode)
            .dataOrNull()
            ?.map<SearchResult, HomeFeedCarouselCardInfo>(::toHomeFeedCarouselCardInfo)
            ?.let { homeFeedCarouselCardInfoList ->
                listOf(
                    HomeFeedCarousel(
                        id = "Newly Released Albums",
                        title = "Newly Released Albums",
                        associatedCards = homeFeedCarouselCardInfoList
                    )
                )
            }.toMutation()
    )
}

private fun <FetchedResourceType> FetchedResource<FetchedResourceType, MusifyErrorType>.dataOrNull() =
    when (this) {
        is FetchedResource.Failure -> null
        is FetchedResource.Success -> data
    }

private fun List<HomeFeedCarousel>?.toMutation(): Mutation<HomeState> = {
    val additions = this@toMutation
    copy(
        homeFeedCarousels = if (additions == null) homeFeedCarousels else homeFeedCarousels + additions,
        loadingState = if (additions == null) HomeFeedViewModel.HomeFeedLoadingState.ERROR else HomeFeedViewModel.HomeFeedLoadingState.IDLE
    )
}

private fun toHomeFeedCarouselCardInfo(searchResult: SearchResult): HomeFeedCarouselCardInfo =
    when (searchResult) {
        is SearchResult.AlbumSearchResult -> {
            HomeFeedCarouselCardInfo(
                id = searchResult.id,
                imageUrlString = searchResult.albumArtUrlString,
                caption = searchResult.name,
                associatedSearchResult = searchResult
            )
        }

        is SearchResult.PlaylistSearchResult -> {
            HomeFeedCarouselCardInfo(
                id = searchResult.id,
                imageUrlString = searchResult.imageUrlString ?: "",
                caption = searchResult.name,
                associatedSearchResult = searchResult
            )
        }

        else -> throw java.lang.IllegalArgumentException("The method supports only the mapping of AlbumSearchResult and PlaylistSearchResult subclasses")
    }