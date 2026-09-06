package com.liquidloop.app.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.annotation.OptIn
import androidx.core.app.NotificationCompat
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.CommandButton
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionResult
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.liquidloop.app.MainActivity
import com.liquidloop.app.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

@OptIn(UnstableApi::class)
class PlaybackService : MediaSessionService() {

    private lateinit var player: ExoPlayer
    private var mediaSession: MediaSession? = null

    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.Main + serviceJob)
    private var loopMonitorJob: Job? = null

    // Loop state
    var loopStartMs: Long = 0L
        private set
    var loopEndMs: Long = 0L
        private set
    var isLoopingEnabled: Boolean = true
        private set

    private var currentTrackUri: Uri? = null
    private var isMicroFading: Boolean = false

    companion object {
        private const val TAG = "LiquidPlaybackService"
        const val CHANNEL_ID = "liquidloop_playback_channel"
        const val NOTIFICATION_ID = 1001

        // Custom Commands
        const val CMD_SET_LOOP = "com.liquidloop.CMD_SET_LOOP"
        const val CMD_TOGGLE_LOOP = "com.liquidloop.CMD_TOGGLE_LOOP"
        const val CMD_RESTART_LOOP = "com.liquidloop.CMD_RESTART_LOOP"
        const val CMD_NUDGE_A = "com.liquidloop.CMD_NUDGE_A"
        const val CMD_NUDGE_B = "com.liquidloop.CMD_NUDGE_B"

        const val EXTRA_START_MS = "EXTRA_START_MS"
        const val EXTRA_END_MS = "EXTRA_END_MS"
        const val EXTRA_DELTA_MS = "EXTRA_DELTA_MS"
        const val EXTRA_ENABLED = "EXTRA_ENABLED"
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        initializePlayer()
        initializeMediaSession()
        startLoopMonitor()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = getString(R.string.playback_channel_name)
            val descriptionText = getString(R.string.playback_channel_description)
            val importance = NotificationManager.IMPORTANCE_LOW
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
                setShowBadge(false)
            }
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun initializePlayer() {
        val audioAttributes = AudioAttributes.Builder()
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .setUsage(C.USAGE_MEDIA)
            .build()

        player = ExoPlayer.Builder(this)
            .setAudioAttributes(audioAttributes, true) // Auto-handle audio focus
            .setHandleAudioBecomingNoisy(true) // Pause on headphones unplug
            .build().apply {
                repeatMode = Player.REPEAT_MODE_OFF
            }

        player.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_ENDED && isLoopingEnabled) {
                    seekToLoopStart()
                    player.play()
                }
            }
        })
    }

    private fun initializeMediaSession() {
        val sessionActivityPendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
            },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        // Custom action buttons for notification / media controller
        val restartButton = CommandButton.Builder()
            .setDisplayName("Restart at A")
            .setIconResId(android.R.drawable.ic_media_previous)
            .setSessionCommand(SessionCommand(CMD_RESTART_LOOP, Bundle.EMPTY))
            .build()

        val toggleLoopButton = CommandButton.Builder()
            .setDisplayName("Toggle Loop")
            .setIconResId(android.R.drawable.ic_menu_rotate)
            .setSessionCommand(SessionCommand(CMD_TOGGLE_LOOP, Bundle.EMPTY))
            .build()

        val callback = object : MediaSession.Callback {
            override fun onConnect(
                session: MediaSession,
                controller: MediaSession.ControllerInfo
            ): MediaSession.ConnectionResult {
                val sessionCommands = MediaSession.ConnectionResult.DEFAULT_SESSION_AND_LIBRARY_COMMANDS.buildUpon()
                    .add(SessionCommand(CMD_SET_LOOP, Bundle.EMPTY))
                    .add(SessionCommand(CMD_TOGGLE_LOOP, Bundle.EMPTY))
                    .add(SessionCommand(CMD_RESTART_LOOP, Bundle.EMPTY))
                    .add(SessionCommand(CMD_NUDGE_A, Bundle.EMPTY))
                    .add(SessionCommand(CMD_NUDGE_B, Bundle.EMPTY))
                    .build()

                return MediaSession.ConnectionResult.AcceptedResultBuilder(session)
                    .setAvailableSessionCommands(sessionCommands)
                    .setCustomLayout(listOf(restartButton, toggleLoopButton))
                    .build()
            }

            override fun onCustomCommand(
                session: MediaSession,
                controller: MediaSession.ControllerInfo,
                customCommand: SessionCommand,
                args: Bundle
            ): ListenableFuture<SessionResult> {
                when (customCommand.customAction) {
                    CMD_SET_LOOP -> {
                        val start = args.getLong(EXTRA_START_MS, loopStartMs)
                        val end = args.getLong(EXTRA_END_MS, loopEndMs)
                        val enabled = args.getBoolean(EXTRA_ENABLED, isLoopingEnabled)
                        updateLoopPoints(start, end, enabled)
                    }
                    CMD_TOGGLE_LOOP -> {
                        toggleLoop()
                    }
                    CMD_RESTART_LOOP -> {
                        seekToLoopStart()
                    }
                    CMD_NUDGE_A -> {
                        val delta = args.getLong(EXTRA_DELTA_MS, 0L)
                        nudgeLoopA(delta)
                    }
                    CMD_NUDGE_B -> {
                        val delta = args.getLong(EXTRA_DELTA_MS, 0L)
                        nudgeLoopB(delta)
                    }
                }
                return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
            }
        }

        mediaSession = MediaSession.Builder(this, player)
            .setSessionActivity(sessionActivityPendingIntent)
            .setCallback(callback)
            .setCustomLayout(listOf(restartButton, toggleLoopButton))
            .build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    /**
     * High-precision monitor checking playback position at 15ms intervals.
     * When nearing loopEndMs: applies micro-fade-out and seeks smoothly to loopStartMs with micro-fade-in.
     */
    private fun startLoopMonitor() {
        loopMonitorJob?.cancel()
        loopMonitorJob = serviceScope.launch {
            while (isActive) {
                if (isLoopingEnabled && player.isPlaying && loopEndMs > loopStartMs) {
                    val currentPos = player.currentPosition

                    // Check if we hit or exceeded Point B
                    if (currentPos >= loopEndMs) {
                        performSeamlessLoopJump()
                    } else if (currentPos < loopStartMs) {
                        // If position is before Point A, snap to Point A
                        player.seekTo(loopStartMs)
                    } else {
                        // Micro-fade out in last 12ms before Point B to prevent audio DC-clicks
                        val remaining = loopEndMs - currentPos
                        if (remaining in 1..15 && !isMicroFading) {
                            applyMicroFadeOut()
                        }
                    }
                }
                delay(12) // 12ms check resolution for sub-frame loop boundary precision
            }
        }
    }

    private fun performSeamlessLoopJump() {
        player.volume = 0.05f
        player.seekTo(loopStartMs)
        // Ramp back smoothly to 1.0f over next frames
        serviceScope.launch {
            delay(8)
            player.volume = 0.5f
            delay(8)
            player.volume = 1.0f
            isMicroFading = false
        }
    }

    private fun applyMicroFadeOut() {
        isMicroFading = true
        player.volume = 0.3f
    }

    fun updateLoopPoints(startMs: Long, endMs: Long, enabled: Boolean = true) {
        loopStartMs = startMs.coerceAtLeast(0L)
        loopEndMs = if (endMs > loopStartMs) endMs else (loopStartMs + 1000L)
        isLoopingEnabled = enabled

        // If player position is outside new loop boundaries, snap to start
        if (isLoopingEnabled && (player.currentPosition < loopStartMs || player.currentPosition >= loopEndMs)) {
            player.seekTo(loopStartMs)
        }
    }

    fun nudgeLoopA(deltaMs: Long) {
        val newStart = (loopStartMs + deltaMs).coerceAtLeast(0L)
        if (newStart < loopEndMs - 50) {
            loopStartMs = newStart
        }
    }

    fun nudgeLoopB(deltaMs: Long) {
        val newEnd = loopEndMs + deltaMs
        if (newEnd > loopStartMs + 50 && (player.duration <= 0 || newEnd <= player.duration)) {
            loopEndMs = newEnd
        }
    }

    fun toggleLoop() {
        isLoopingEnabled = !isLoopingEnabled
    }

    fun seekToLoopStart() {
        player.seekTo(loopStartMs)
        player.volume = 1.0f
        isMicroFading = false
    }

    override fun onDestroy() {
        loopMonitorJob?.cancel()
        serviceScope.cancel()
        mediaSession?.run {
            player.release()
            release()
            mediaSession = null
        }
        super.onDestroy()
    }
}
