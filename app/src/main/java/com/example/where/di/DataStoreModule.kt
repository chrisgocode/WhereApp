package com.example.where.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

private val Context.userPreferencesDataStore: DataStore<Preferences> by
        preferencesDataStore(name = "user_preferences")

@Module
@InstallIn(SingletonComponent::class) // Provides DataStore as a singleton for the app
object DataStoreModule {

    @Provides
    @Singleton
    fun provideUserPreferencesDataStore(
            @ApplicationContext applicationContext: Context
    ): DataStore<Preferences> {
        return applicationContext.userPreferencesDataStore // Use the extension property
    }
}
