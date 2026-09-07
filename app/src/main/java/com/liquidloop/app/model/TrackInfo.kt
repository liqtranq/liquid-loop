package com.liquidloop.app.model

import android.net.Uri

data class TrackInfo(
    val uri: Uri,
    val title: String,
    val artist: String,
    val durationMs: Long,
    val mimeType: String? = null,
    val bitRate: Long? = null,
    val sampleRate: Int? = null,
    val channelCount: Int? = null,
    val artwork: ByteArray? = null
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as TrackInfo

        if (uri != other.uri) return false
        if (title != other.title) return false
        if (durationMs != other.durationMs) return false

        return true
    }

    override fun hashCode(): Int {
        var result = uri.hashCode()
        result = 31 * result + title.hashCode()
        result = 31 * result + durationMs.hashCode()
        return result
    }
    val formattedDuration: String
        get() = formatTimeMs(durationMs)

    companion object {
        val EMPTY = TrackInfo(
            uri = Uri.EMPTY,
            title = "No track loaded",
            artist = "Tap import to select audio file",
            durationMs = 0L
        )

        fun formatTimeMs(ms: Long): String {
            if (ms <= 0) return "00:00.000"
            val totalSeconds = ms / 1000
            val minutes = totalSeconds / 60
            val seconds = totalSeconds % 60
            val millis = ms % 1000
            return String.format(java.util.Locale.US, "%02d:%02d.%03d", minutes, seconds, millis)
        }

        fun formatShortTimeMs(ms: Long): String {
            if (ms <= 0) return "0.00s"
            val totalSeconds = ms.toDouble() / 1000.0
            return String.format(java.util.Locale.US, "%.2fs", totalSeconds)
        }
    }
}
