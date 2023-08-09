package com.example.musify.ui.screens.homescreen

import com.example.musify.data.repositories.homefeedrepository.HomeFeedRepository
import com.example.musify.data.repositories.homefeedrepository.ISO6391LanguageCode
import com.example.musify.data.utils.FetchedResource
import com.example.musify.data.utils.NetworkMonitor
import com.example.musify.data.utils.onConnected
import com.example.musify.domain.HomeFeedCarousel
import com.example.musify.domain.HomeFeedCarouselCardInfo
import com.example.musify.domain.MusifyErrorType
import com.example.musify.domain.PlaylistsForCategory
import com.example.musify.domain.SearchResult
import com.example.musify.domain.toHomeFeedCarousel
import com.example.musify.ui.screens.homescreen.greetingphrasegenerator.GreetingPhraseGenerator
import com.tunjid.mutator.Mutation
import com.tunjid.mutator.coroutines.actionStateFlowProducer
import com.tunjid.mutator.coroutines.toMutationStream
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge

sealed class HomeAction {
    object Retry : HomeAction()
}

data class HomeUiState(
    val loadingState: HomeFeedLoadingState = HomeFeedLoadingState.LOADING,
    val homeFeedCarousels: List<HomeFeedCarousel> = emptyList(),
    val greetingPhrase: String,
)

/**
 * An enum class that contains the different UI states associated
 * with a screen that displays the home feed.
 */
enum class HomeFeedLoadingState { IDLE, LOADING, ERROR }

fun CoroutineScope.homeScreenStateProducer(
    countryCode: String,
    languageCode: ISO6391LanguageCode,
    networkMonitor: NetworkMonitor,
    greetingPhraseGenerator: GreetingPhraseGenerator,
    homeFeedRepository: HomeFeedRepository,
) = actionStateFlowProducer<HomeAction, HomeUiState>(
    initialState = HomeUiState(
        greetingPhrase = greetingPhraseGenerator.generatePhrase()
    ),
    mutationFlows = listOf(
        homeFeedRepository.albumReleaseMutations(
            countryCode = countryCode,
            networkMonitor = networkMonitor,
        ),
        homeFeedRepository.featuredPlaylistMutations(
            languageCode = languageCode,
            countryCode = countryCode,
            networkMonitor = networkMonitor,
        ),
        homeFeedRepository.playlistCategoryMutations(
            languageCode = languageCode,
            countryCode = countryCode,
            networkMonitor = networkMonitor,
        ),
    ),
    actionTransform = { actions ->
        actions.toMutationStream {
            when (val action = type()) {
                is HomeAction.Retry -> action.flow.flatMapLatest {
                    listOf(
                        homeFeedRepository.albumReleaseMutations(
                            countryCode = countryCode,
                            networkMonitor = networkMonitor,
                        ),
                        homeFeedRepository.featuredPlaylistMutations(
                            languageCode = languageCode,
                            countryCode = countryCode,
                            networkMonitor = networkMonitor,
                        ),
                        homeFeedRepository.playlistCategoryMutations(
                            languageCode = languageCode,
                            countryCode = countryCode,
                            networkMonitor = networkMonitor,
                        ),
                    ).merge()
                }
            }
        }
    }
)

private fun HomeFeedRepository.playlistCategoryMutations(
    countryCode: String,
    languageCode: ISO6391LanguageCode,
    networkMonitor: NetworkMonitor,
) = networkMonitor.onConnected().map {
    fetchPlaylistsBasedOnCategoriesAvailableForCountry(
        countryCode = countryCode, languageCode = languageCode
    )
        .dataOrNull()
        ?.map(PlaylistsForCategory::toHomeFeedCarousel)
        .toMutation()
}

private fun HomeFeedRepository.featuredPlaylistMutations(
    countryCode: String,
    languageCode: ISO6391LanguageCode,
    networkMonitor: NetworkMonitor,
) = networkMonitor.onConnected().map {
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
}

private fun HomeFeedRepository.albumReleaseMutations(
    countryCode: String,
    networkMonitor: NetworkMonitor,
) = networkMonitor.onConnected()
    .map {
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
    }

private fun <FetchedResourceType> FetchedResource<FetchedResourceType, MusifyErrorType>.dataOrNull() =
    when (this) {
        is FetchedResource.Failure -> null
        is FetchedResource.Success -> data
    }

private fun List<HomeFeedCarousel>?.toMutation(): Mutation<HomeUiState> = {
    val additions = this@toMutation
    copy(
        homeFeedCarousels = when (additions) {
            null -> homeFeedCarousels
            else -> homeFeedCarousels + additions
        },
        loadingState = when (additions) {
            null -> HomeFeedLoadingState.ERROR
            else -> HomeFeedLoadingState.IDLE
        }
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

        else -> throw java.lang.IllegalArgumentException(
            "The method supports only the mapping of AlbumSearchResult and PlaylistSearchResult subclasses"
        )
    }