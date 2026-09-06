package com.liquidloop.app.core.audio

import android.content.Context
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri
import android.util.Log
import android.util.LruCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.sin
import kotlin.math.sqrt

object WaveformExtractor {
    private const val TAG = "WaveformExtractor"
    private const val DEFAULT_BARS_COUNT = 256
    private const val TIMEOUT_US = 5000L

    // In-memory cache for extracted waveforms: URI string -> FloatArray
    private val memoryCache = LruCache<String, FloatArray>(20)

    suspend fun extractWaveform(
        context: Context,
        uri: Uri,
        targetBars: Int = DEFAULT_BARS_COUNT
    ): FloatArray = withContext(Dispatchers.IO) {
        val cacheKey = uri.toString()
        memoryCache.get(cacheKey)?.let {
            return@withContext it
        }

        try {
            val amplitudes = decodeAmplitudes(context, uri, targetBars)
            if (amplitudes != null && amplitudes.isNotEmpty()) {
                val normalized = normalizeAmplitudes(amplitudes)
                memoryCache.put(cacheKey, normalized)
                return@withContext normalized
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error decoding audio for waveform from $uri", e)
        }

        // Fallback: generate smooth aesthetic procedural waveform if format unsupported by decoder
        val fallback = generateFallbackWaveform(targetBars)
        memoryCache.put(cacheKey, fallback)
        fallback
    }

    private fun decodeAmplitudes(
        context: Context,
        uri: Uri,
        targetBars: Int
    ): FloatArray? {
        val extractor = MediaExtractor()
        var codec: MediaCodec? = null

        try {
            extractor.setDataSource(context, uri, null)
            val trackIndex = selectAudioTrack(extractor)
            if (trackIndex < 0) {
                Log.w(TAG, "No audio track found in media source")
                return null
            }
            extractor.selectTrack(trackIndex)
            val format = extractor.getTrackFormat(trackIndex)
            val mime = format.getString(MediaFormat.KEY_MIME) ?: return null

            val durationUs = if (format.containsKey(MediaFormat.KEY_DURATION)) {
                format.getLong(MediaFormat.KEY_DURATION)
            } else {
                0L
            }

            codec = MediaCodec.createDecoderByType(mime)
            codec.configure(format, null, null, 0)
            codec.start()

            val rawAmplitudes = ArrayList<Float>(targetBars * 2)
            val bufferInfo = MediaCodec.BufferInfo()
            var isEOS = false
            var inputEos = false

            var sampleCount = 0
            var sampleSum = 0.0

            while (!isEOS) {
                if (!inputEos) {
                    val inputIndex = codec.dequeueInputBuffer(TIMEOUT_US)
                    if (inputIndex >= 0) {
                        val inputBuffer = codec.getInputBuffer(inputIndex)
                        if (inputBuffer != null) {
                            val sampleSize = extractor.readSampleData(inputBuffer, 0)
                            if (sampleSize < 0) {
                                codec.queueInputBuffer(
                                    inputIndex, 0, 0, 0L,
                                    MediaCodec.BUFFER_FLAG_END_OF_STREAM
                                )
                                inputEos = true
                            } else {
                                val sampleTime = extractor.sampleTime
                                codec.queueInputBuffer(
                                    inputIndex, 0, sampleSize, sampleTime, 0
                                )
                                extractor.advance()
                            }
                        }
                    }
                }

                val outputIndex = codec.dequeueOutputBuffer(bufferInfo, TIMEOUT_US)
                if (outputIndex >= 0) {
                    val outputBuffer = codec.getOutputBuffer(outputIndex)
                    if (outputBuffer != null && bufferInfo.size > 0) {
                        outputBuffer.position(bufferInfo.offset)
                        outputBuffer.limit(bufferInfo.offset + bufferInfo.size)
                        outputBuffer.order(ByteOrder.LITTLE_ENDIAN)

                        val shortBuffer = outputBuffer.asShortBuffer()
                        while (shortBuffer.hasRemaining()) {
                            val sample = shortBuffer.get().toInt()
                            sampleSum += sample * sample
                            sampleCount++

                            // Aggregate every 512 samples to calculate RMS chunk
                            if (sampleCount >= 512) {
                                val rms = sqrt(sampleSum / sampleCount).toFloat()
                                rawAmplitudes.add(rms)
                                sampleSum = 0.0
                                sampleCount = 0
                            }
                        }
                    }
                    codec.releaseOutputBuffer(outputIndex, false)

                    if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                        isEOS = true
                    }
                }
            }

            if (rawAmplitudes.isEmpty()) {
                return null
            }

            // Downsample or interpolate rawAmplitudes to exact targetBars count
            return resampleAmplitudes(rawAmplitudes, targetBars)

        } finally {
            try {
                codec?.stop()
                codec?.release()
            } catch (e: Exception) {
                Log.e(TAG, "Failed releasing MediaCodec", e)
            }
            try {
                extractor.release()
            } catch (e: Exception) {
                Log.e(TAG, "Failed releasing MediaExtractor", e)
            }
        }
    }

    private fun selectAudioTrack(extractor: MediaExtractor): Int {
        for (i in 0 until extractor.trackCount) {
            val format = extractor.getTrackFormat(i)
            val mime = format.getString(MediaFormat.KEY_MIME) ?: continue
            if (mime.startsWith("audio/")) {
                return i
            }
        }
        return -1
    }

    private fun resampleAmplitudes(source: List<Float>, targetSize: Int): FloatArray {
        val result = FloatArray(targetSize)
        val step = source.size.toFloat() / targetSize.toFloat()

        for (i in 0 until targetSize) {
            val startIdx = (i * step).toInt().coerceIn(0, source.size - 1)
            val endIdx = ((i + 1) * step).toInt().coerceIn(startIdx + 1, source.size)

            var peak = 0f
            for (j in startIdx until endIdx) {
                peak = max(peak, source[j])
            }
            result[i] = peak
        }
        return result
    }

    private fun normalizeAmplitudes(amplitudes: FloatArray): FloatArray {
        var maxVal = 0.001f
        for (amp in amplitudes) {
            if (amp > maxVal) maxVal = amp
        }

        val result = FloatArray(amplitudes.size)
        for (i in amplitudes.indices) {
            // Apply gentle logarithmic/compression curve so quiet sections remain visible
            val ratio = (amplitudes[i] / maxVal).coerceIn(0f, 1f)
            result[i] = (sqrt(ratio.toDouble()).toFloat()).coerceIn(0.08f, 1.0f)
        }
        return result
    }

    private fun generateFallbackWaveform(targetBars: Int): FloatArray {
        val array = FloatArray(targetBars)
        for (i in 0 until targetBars) {
            val progress = i.toFloat() / targetBars
            val wave1 = abs(sin(progress * 18.0)).toFloat()
            val wave2 = abs(sin(progress * 42.0)).toFloat() * 0.4f
            val wave3 = abs(sin(progress * 6.0)).toFloat() * 0.3f
            array[i] = ((wave1 + wave2 + wave3) / 1.7f).coerceIn(0.15f, 0.95f)
        }
        return array
    }
}
