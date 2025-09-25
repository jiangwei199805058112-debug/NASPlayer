package com.example.nasonly.di

import android.content.Context
import com.example.nasonly.data.datastore.NasPrefs
import com.example.nasonly.data.preferences.UserPreferences
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DataStoreModule {
    @Provides
    @Singleton
    fun provideNasPrefs(@ApplicationContext context: Context): NasPrefs = NasPrefs(context)

    @Provides
    @Singleton
    fun provideUserPreferences(@ApplicationContext context: Context): UserPreferences = UserPreferences(context)
}