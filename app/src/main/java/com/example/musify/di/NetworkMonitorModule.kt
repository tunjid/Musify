package com.example.musify.di

import com.example.musify.data.utils.ConnectivityManagerNetworkMonitor
import com.example.musify.data.utils.NetworkMonitor
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class NetworkMonitorModule {

    @Binds
    abstract fun networkMonitor(impl: ConnectivityManagerNetworkMonitor): NetworkMonitor

}