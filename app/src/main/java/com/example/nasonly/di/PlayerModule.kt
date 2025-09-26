package com.example.nasonly.di

import android.content.Context
import com.example.nasonly.core.player.ExoPlayerManager
import com.example.nasonly.data.smb.SmbDataSource
import androidx.media3.exoplayer.ExoPlayer
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object PlayerModule {
    @Provides
    @Singleton
    fun provideExoPlayer(@ApplicationContext context: Context): ExoPlayer =
        ExoPlayer.Builder(context).build()

    @Provides
    @Singleton
    fun provideExoPlayerManager(
        @ApplicationContext context: Context,
        smbDataSource: SmbDataSource
    ): ExoPlayerManager = ExoPlayerManager(context, smbDataSource)
}
