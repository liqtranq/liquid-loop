package com.liquidloop.app.core.audio

import android.content.ComponentName
import android.content.Context
import android.net.Uri
import android.os.Bundle
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import com.liquidloop.app.model.LoopState
import com.liquidloop.app.model.PlaybackState
import com.liquidloop.app.model.TrackInfo
import com.liquidloop.app.service.PlaybackService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

@OptIn(UnstableApi::class)
class LiquidAudioController(private val context: Context) {

    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var mediaController: MediaController? = null

    private val _playbackState = MutableStateFlow(PlaybackState())
    val playbackState: StateFlow<PlaybackState> = _playbackState.asStateFlow()

    private val _loopState = MutableStateFlow(LoopState())
    val loopState: StateFlow<LoopState> = _loopState.asStateFlow()

    private val _currentTrack = MutableStateFlow(TrackInfo.EMPTY)
    val currentTrack: StateFlow<TrackInfo> = _currentTrack.asStateFlow()

    private val coroutineScope = CoroutineScope(Dispatchers.Main)
    private var progressPollingJob: Job? = null

    init {
        connectToService()
    }

    private fun connectToService() {
        val sessionToken = SessionToken(
            context,
            ComponentName(context, PlaybackService::class.java)
        )

        controllerFuture = MediaController.Builder(context, sessionToken).buildAsync()
        controllerFuture?.addListener({
            try {
                mediaController = controllerFuture?.get()
                setupControllerListener()
                startProgressPolling()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }, MoreExecutors.directExecutor())
    }

    private fun setupControllerListener() {
        val controller = mediaController ?: return
        controller.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _playbackState.update { it.copy(isPlaying = isPlaying) }
            }

            override fun onPlaybackStateChanged(state: Int) {
                val isBuffering = state == Player.STATE_BUFFERING
                _playbackState.update {
                    it.copy(
                        isBuffering = isBuffering,
                        durationMs = controller.duration.coerceAtLeast(0L),
                        currentPositionMs = controller.currentPosition.coerceAtLeast(0L)
                    )
                }
            }

            override fun onPlaybackParametersChanged(playbackParameters: PlaybackParameters) {
                _playbackState.update { it.copy(playbackSpeed = playbackParameters.speed) }
            }
        })
    }

    private fun startProgressPolling() {
        progressPollingJob?.cancel()
        progressPollingJob = coroutineScope.launch {
            while (isActive) {
                val controller = mediaController
                if (controller != null) {
                    val pos = controller.currentPosition.coerceAtLeast(0L)
                    val dur = controller.duration.coerceAtLeast(0L)
                    val isPlaying = controller.isPlaying

                    _playbackState.update {
                        it.copy(
                            currentPositionMs = pos,
                            durationMs = if (dur > 0) dur else it.durationMs,
                            isPlaying = isPlaying
                        )
                    }
                }
                delay(16) // Smooth 60fps playhead polling
            }
        }
    }

    fun loadTrack(track: TrackInfo) {
        val controller = mediaController ?: return
        _currentTrack.value = track

        val mediaMetadata = MediaMetadata.Builder()
            .setTitle(track.title)
            .setArtist(track.artist)
            .build()

        val mediaItem = MediaItem.Builder()
            .setUri(track.uri)
            .setMediaId(track.uri.toString())
            .setMediaMetadata(mediaMetadata)
            .build()

        controller.setMediaItem(mediaItem)
        controller.prepare()

        // Default loop: full track or first 15 seconds if long track
        val defaultStart = 0L
        val defaultEnd = if (track.durationMs > 0) {
            track.durationMs.coerceAtMost(30000L)
        } else {
            10000L
        }

        setLoopPoints(defaultStart, defaultEnd)
    }

    fun play() {
        mediaController?.play()
    }

    fun pause() {
        mediaController?.pause()
    }

    fun togglePlayPause() {
        val controller = mediaController ?: return
        if (controller.isPlaying) {
            controller.pause()
        } else {
            controller.play()
        }
    }

    fun seekTo(positionMs: Long) {
        val controller = mediaController ?: return
        val clamped = positionMs.coerceIn(0L, _playbackState.value.durationMs.coerceAtLeast(1L))
        controller.seekTo(clamped)
        _playbackState.update { it.copy(currentPositionMs = clamped) }
    }

    fun setLoopPoints(startMs: Long, endMs: Long) {
        val start = startMs.coerceAtLeast(0L)
        val end = if (endMs > start + 50) endMs else (start + 1000L)

        _loopState.update {
            it.copy(startMs = start, endMs = end)
        }

        val args = Bundle().apply {
            putLong(PlaybackService.EXTRA_START_MS, start)
            putLong(PlaybackService.EXTRA_END_MS, end)
            putBoolean(PlaybackService.EXTRA_ENABLED, _loopState.value.isEnabled)
        }
        mediaController?.sendCustomCommand(
            SessionCommand(PlaybackService.CMD_SET_LOOP, Bundle.EMPTY),
            args
        )
    }

    fun nudgeLoopA(deltaMs: Long) {
        val current = _loopState.value
        val newStart = (current.startMs + deltaMs).coerceIn(0L, current.endMs - 50L)
        setLoopPoints(newStart, current.endMs)
    }

    fun nudgeLoopB(deltaMs: Long) {
        val current = _loopState.value
        val maxDur = _playbackState.value.durationMs.let { if (it > 0) it else Long.MAX_VALUE }
        val newEnd = (current.endMs + deltaMs).coerceIn(current.startMs + 50L, maxDur)
        setLoopPoints(current.startMs, newEnd)
    }

    fun setPointAToCurrent() {
        val curPos = _playbackState.value.currentPositionMs
        val curEnd = _loopState.value.endMs
        if (curPos < curEnd - 50) {
            setLoopPoints(curPos, curEnd)
        }
    }

    fun setPointBToCurrent() {
        val curPos = _playbackState.value.currentPositionMs
        val curStart = _loopState.value.startMs
        if (curPos > curStart + 50) {
            setLoopPoints(curStart, curPos)
        }
    }

    fun restartLoop() {
        val start = _loopState.value.startMs
        seekTo(start)
        mediaController?.sendCustomCommand(
            SessionCommand(PlaybackService.CMD_RESTART_LOOP, Bundle.EMPTY),
            Bundle.EMPTY
        )
    }

    fun toggleLoop() {
        val newState = !_loopState.value.isEnabled
        _loopState.update { it.copy(isEnabled = newState) }
        val args = Bundle().apply {
            putBoolean(PlaybackService.EXTRA_ENABLED, newState)
            putLong(PlaybackService.EXTRA_START_MS, _loopState.value.startMs)
            putLong(PlaybackService.EXTRA_END_MS, _loopState.value.endMs)
        }
        mediaController?.sendCustomCommand(
            SessionCommand(PlaybackService.CMD_SET_LOOP, Bundle.EMPTY),
            args
        )
    }

    fun setPlaybackSpeed(speed: Float) {
        mediaController?.playbackParameters = PlaybackParameters(speed)
        _playbackState.update { it.copy(playbackSpeed = speed) }
    }

    fun release() {
        progressPollingJob?.cancel()
        controllerFuture?.let {
            MediaController.releaseFuture(it)
        }
        mediaController = null
    }
}
