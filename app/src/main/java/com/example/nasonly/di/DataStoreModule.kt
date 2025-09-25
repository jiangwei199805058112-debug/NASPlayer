package com.example.nasonly.di

import android.content.Context
import com.example.nasonly.data.datastore.NasPrefs
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
}