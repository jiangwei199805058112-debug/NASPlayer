package com.example.nasonly.core.player

import android.content.Context
import android.net.Uri
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import com.example.nasonly.data.smb.SmbDataSource

class ExoPlayerManager(
    private val context: Context,
    private val smbDataSource: SmbDataSource,
) : DefaultLifecycleObserver {
    private var exoPlayer: ExoPlayer? = null

    fun createPlayer(): ExoPlayer {
        if (exoPlayer == null) {
            exoPlayer = ExoPlayer.Builder(context).build()
        }
        return exoPlayer!!
    }

    fun prepare(uri: Uri) {
        val player = createPlayer()
        val dataSourceFactory = SmbMediaDataSource.Factory(smbDataSource)
        val mediaSource = ProgressiveMediaSource.Factory(dataSourceFactory)
            .createMediaSource(MediaItem.fromUri(uri))
        player.setMediaSource(mediaSource)
        player.prepare()
    }

    fun play() {
        exoPlayer?.play()
    }

    fun pause() {
        exoPlayer?.pause()
    }

    fun seekTo(positionMs: Long) {
        exoPlayer?.seekTo(positionMs)
    }

    fun setPlaybackSpeed(speed: Float) {
        exoPlayer?.setPlaybackParameters(
            exoPlayer!!.playbackParameters.withSpeed(speed),
        )
    }

    fun release() {
        exoPlayer?.release()
        exoPlayer = null
    }

    override fun onStop(owner: LifecycleOwner) {
        super.onStop(owner)
        release()
    }

    override fun onDestroy(owner: LifecycleOwner) {
        super.onDestroy(owner)
        release()
    }

    fun getPlayer(): ExoPlayer? = exoPlayer
}
