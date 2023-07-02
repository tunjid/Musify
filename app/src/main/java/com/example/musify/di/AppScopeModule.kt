package com.example.musify.di

import com.example.musify.data.encoder.AndroidBase64Encoder
import com.example.musify.data.encoder.Base64Encoder
import com.example.musify.data.repositories.tokenrepository.SpotifyTokenRepository
import com.example.musify.data.repositories.tokenrepository.TokenRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppScopeModule {

    @Provides
    @Singleton
    fun provideAppScope(): CoroutineScope =
        CoroutineScope(SupervisorJob() + Dispatchers.Main)
}