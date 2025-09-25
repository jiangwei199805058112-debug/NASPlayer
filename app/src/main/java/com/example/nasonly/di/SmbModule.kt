package com.example.nasonly.di

import com.example.nasonly.data.smb.SmbManager
import com.example.nasonly.data.smb.SmbConnectionManager
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class SmbModule {
    @Binds
    @Singleton
    abstract fun bindSmbManager(smbConnectionManager: SmbConnectionManager): SmbManager
}
