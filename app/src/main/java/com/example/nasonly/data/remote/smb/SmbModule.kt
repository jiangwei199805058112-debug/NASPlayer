package com.example.nasonly.data.remote.smb

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.Dispatchers
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object SmbModule {
    @Provides @Singleton
    fun provideSmbScanner(): SmbScanner = SmbScanner(Dispatchers.IO)
}