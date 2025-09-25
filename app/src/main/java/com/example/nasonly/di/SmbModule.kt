package com.example.nasonly.di

import com.example.nasonly.data.smb.SmbManager
import com.example.nasonly.data.smb.SmbConnectionManager
import com.example.nasonly.data.smb.SmbDataSource
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object SmbModule {
    @Provides
    @Singleton
    fun provideSmbManager(): SmbManager = SmbConnectionManager()

    @Provides
    @Singleton
    fun provideSmbDataSource(smbManager: SmbManager): SmbDataSource = SmbDataSource(smbManager)
}
