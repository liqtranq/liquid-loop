package com.liquidloop.app.ui.viewmodel

import android.app.Application
import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.liquidloop.app.core.audio.LiquidAudioController
import com.liquidloop.app.core.audio.WaveformExtractor
import com.liquidloop.app.model.LoopState
import com.liquidloop.app.model.PlaybackState
import com.liquidloop.app.model.TrackInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PlayerViewModel(application: Application) : AndroidViewModel(application) {

    private val audioController = LiquidAudioController(application)

    val playbackState: StateFlow<PlaybackState> = audioController.playbackState
    val loopState: StateFlow<LoopState> = audioController.loopState
    val currentTrack: StateFlow<TrackInfo> = audioController.currentTrack

    private val _waveform = MutableStateFlow(FloatArray(0))
    val waveform: StateFlow<FloatArray> = _waveform.asStateFlow()

    private val _isLoadingWaveform = MutableStateFlow(false)
    val isLoadingWaveform: StateFlow<Boolean> = _isLoadingWaveform.asStateFlow()

    private val _isInPictureInPicture = MutableStateFlow(false)
    val isInPictureInPicture: StateFlow<Boolean> = _isInPictureInPicture.asStateFlow()

    fun onAudioSelected(uri: Uri) {
        viewModelScope.launch {
            val trackInfo = extractMetadata(getApplication(), uri)
            audioController.loadTrack(trackInfo)

            // Extract Waveform in background
            _isLoadingWaveform.value = true
            try {
                val wf = WaveformExtractor.extractWaveform(getApplication(), uri)
                _waveform.value = wf
            } catch (e: Exception) {
                Log.e("PlayerViewModel", "Error extracting waveform", e)
            } finally {
                _isLoadingWaveform.value = false
            }
        }
    }

    private suspend fun extractMetadata(context: Context, uri: Uri): TrackInfo =
        withContext(Dispatchers.IO) {
            var title: String? = null
            var artist: String? = null
            var durationMs = 0L
            var mimeType: String? = null
            var bitRate: Long? = null
            var sampleRate: Int? = null

            // 1. Try querying ContentResolver for display name
            try {
                context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (nameIndex >= 0 && cursor.moveToFirst()) {
                        title = cursor.getString(nameIndex)
                    }
                }
            } catch (e: Exception) {
                Log.w("PlayerViewModel", "Failed to query filename from ContentResolver", e)
            }

            // 2. Extract media tags via MediaMetadataRetriever
            val retriever = MediaMetadataRetriever()
            try {
                retriever.setDataSource(context, uri)
                val metaTitle = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
                val metaArtist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
                val metaDuration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                val metaMime = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_MIMETYPE)
                val metaBitrate = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE)

                if (!metaTitle.isNullOrBlank()) title = metaTitle
                if (!metaArtist.isNullOrBlank()) artist = metaArtist
                if (!metaDuration.isNullOrBlank()) durationMs = metaDuration.toLongOrNull() ?: 0L
                if (!metaMime.isNullOrBlank()) mimeType = metaMime
                if (!metaBitrate.isNullOrBlank()) bitRate = metaBitrate.toLongOrNull()

            } catch (e: Exception) {
                Log.w("PlayerViewModel", "Failed to read ID3 metadata from retriever", e)
            } finally {
                try {
                    retriever.release()
                } catch (e: Exception) {
                    // Ignore
                }
            }

            TrackInfo(
                uri = uri,
                title = title ?: (uri.lastPathSegment ?: "Audio Track"),
                artist = artist ?: "Unknown Artist",
                durationMs = durationMs,
                mimeType = mimeType ?: context.contentResolver.getType(uri),
                bitRate = bitRate,
                sampleRate = sampleRate
            )
        }

    fun togglePlayPause() {
        audioController.togglePlayPause()
    }

    fun seekTo(positionMs: Long) {
        audioController.seekTo(positionMs)
    }

    fun setLoopPoints(startMs: Long, endMs: Long) {
        audioController.setLoopPoints(startMs, endMs)
    }

    fun nudgeLoopA(deltaMs: Long) {
        audioController.nudgeLoopA(deltaMs)
    }

    fun nudgeLoopB(deltaMs: Long) {
        audioController.nudgeLoopB(deltaMs)
    }

    fun setPointAToCurrent() {
        audioController.setPointAToCurrent()
    }

    fun setPointBToCurrent() {
        audioController.setPointBToCurrent()
    }

    fun restartLoop() {
        audioController.restartLoop()
    }

    fun toggleLoop() {
        audioController.toggleLoop()
    }

    fun setPlaybackSpeed(speed: Float) {
        audioController.setPlaybackSpeed(speed)
    }

    fun setPitch(pitch: Float) {
        audioController.setPitch(pitch)
    }

    fun toggleMetronome() {
        audioController.toggleMetronome()
    }

    fun setBpm(bpm: Float) {
        audioController.setBpm(bpm)
    }

    fun setTimeSignature(ts: Int) {
        audioController.setTimeSignature(ts)
    }

    fun setPictureInPicture(isInPiP: Boolean) {
        _isInPictureInPicture.value = isInPiP
    }

    override fun onCleared() {
        audioController.release()
        super.onCleared()
    }
}
