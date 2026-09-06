package com.liquidloop.app.model

data class LoopState(
    val startMs: Long = 0L,
    val endMs: Long = 0L,
    val isEnabled: Boolean = true,
    val isSeamlessClipping: Boolean = true
) {
    val durationMs: Long
        get() = (endMs - startMs).coerceAtLeast(0L)

    val formattedDuration: String
        get() = TrackInfo.formatTimeMs(durationMs)

    val formattedStart: String
        get() = TrackInfo.formatTimeMs(startMs)

    val formattedEnd: String
        get() = TrackInfo.formatTimeMs(endMs)

    /**
     * Estimated BPM assuming the loop represents 4 beats (1 bar of 4/4) or 8 beats (2 bars).
     */
    fun estimateBpm(beats: Int = 4): Double? {
        if (durationMs <= 100) return null
        val durationSeconds = durationMs / 1000.0
        val bpm = (beats / durationSeconds) * 60.0
        return if (bpm in 40.0..300.0) bpm else null
    }
}
