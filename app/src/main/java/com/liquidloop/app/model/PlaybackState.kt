package com.liquidloop.app.model

data class PlaybackState(
    val currentPositionMs: Long = 0L,
    val durationMs: Long = 0L,
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val playbackSpeed: Float = 1.0f,
    val pitch: Float = 1.0f,
    val isMetronomeEnabled: Boolean = false,
    val bpm: Float = 120f,
    val timeSignature: Int = 4
) {
    val progress: Float
        get() = if (durationMs > 0) (currentPositionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f) else 0f

    val formattedPosition: String
        get() = TrackInfo.formatTimeMs(currentPositionMs)
}
