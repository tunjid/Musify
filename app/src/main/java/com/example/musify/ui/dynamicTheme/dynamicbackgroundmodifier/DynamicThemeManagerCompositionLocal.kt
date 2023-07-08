package com.example.musify.ui.dynamicTheme.dynamicbackgroundmodifier

import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import com.example.musify.ui.dynamicTheme.manager.DynamicThemeManager
import com.example.musify.ui.dynamicTheme.manager.MusifyDynamicThemeManager
import com.example.musify.usecases.downloadDrawableFromUrlUseCase.MusifyDownloadDrawableFromUrlUseCase
import kotlinx.coroutines.Dispatchers

val LocalDynamicThemeManager: ProvidableCompositionLocal<DynamicThemeManager> =
    staticCompositionLocalOf {
        object : DynamicThemeManager {
            override suspend fun getBackgroundColorForImageFromUrl(url: String): Color? = null
        }
    }