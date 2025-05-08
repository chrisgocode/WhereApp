package com.example.where.di

import com.example.where.BuildConfig
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ConfigModule {

    @Provides
    @Singleton
    @Named("mapsApiKey")
    fun provideMapsApiKey(): String {
        return BuildConfig.MAPS_API_KEY
    }
}
