package com.example.nasonly.core.player

import android.content.Context
import android.net.Uri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import com.example.nasonly.data.smb.SmbDataSource
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.upstream.DataSource
import com.google.android.exoplayer2.upstream.DefaultDataSource
import com.google.android.exoplayer2.util.Util

class ExoPlayerManager(
    private val context: Context,
    private val smbDataSource: SmbDataSource
) : LifecycleObserver {
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
            exoPlayer!!.playbackParameters.withSpeed(speed)
        )
    }

    fun release() {
        exoPlayer?.release()
        exoPlayer = null
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    fun onStop() {
        release()
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    fun onDestroy() {
        release()
    }

    fun getPlayer(): ExoPlayer? = exoPlayer
}
