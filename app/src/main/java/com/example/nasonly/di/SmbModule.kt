package com.example.nasonly.di

import android.content.Context
import com.example.nasonly.data.discovery.NasDiscoveryManager
import com.example.nasonly.data.repository.NasRepository
import com.example.nasonly.data.smb.SmbConnectionManager
import com.example.nasonly.data.smb.SmbDataSource
import com.example.nasonly.data.smb.SmbManager
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class SmbModule {
    @Binds
    @Singleton
    abstract fun bindSmbManager(smbConnectionManager: SmbConnectionManager): SmbManager

    companion object {
        @Provides
        @Singleton
        fun provideNasDiscoveryManager(@ApplicationContext context: Context): NasDiscoveryManager {
            return NasDiscoveryManager(context)
        }

        @Provides
        @Singleton
        fun provideNasRepository(
            smbConnectionManager: SmbConnectionManager,
            smbDataSource: SmbDataSource,
            nasDiscoveryManager: NasDiscoveryManager,
        ): NasRepository {
            return NasRepository(smbConnectionManager, smbDataSource, nasDiscoveryManager)
        }
    }
}
