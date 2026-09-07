package com.liquidloop.app.core.audio

import kotlin.math.abs
import kotlin.math.max

object BpmDetector {

    /**
     * Estimates BPM given a list of RMS energies for short windows (e.g. 10ms - 20ms).
     * @param energies Array of RMS values for consecutive windows
     * @param windowDurationMs Duration of each window in milliseconds
     * @return Estimated BPM, or null if it cannot be determined reliably
     */
    fun estimateBpm(energies: FloatArray, windowDurationMs: Float): Float? {
        if (energies.size < 100) return null

        // 1. Differentiate to find sudden increases in energy (onsets)
        val onsets = FloatArray(energies.size)
        for (i in 1 until energies.size) {
            val diff = energies[i] - energies[i - 1]
            onsets[i] = max(0f, diff)
        }

        // 2. Pick peaks in the onsets
        val peakIndices = mutableListOf<Int>()
        var localMax = 0f
        var localMaxIdx = -1
        
        // Window size to consider for a local peak (~ 250ms = minimum beat interval for ~240 BPM)
        val peakWindowSize = (250f / windowDurationMs).toInt().coerceAtLeast(1)

        for (i in onsets.indices) {
            if (onsets[i] > localMax) {
                localMax = onsets[i]
                localMaxIdx = i
            }
            // If we've moved past the window, register the peak if it's significant
            if (i > 0 && i % peakWindowSize == 0) {
                if (localMaxIdx != -1 && localMax > 0.05f) { // Arbitrary threshold
                    peakIndices.add(localMaxIdx)
                }
                localMax = 0f
                localMaxIdx = -1
            }
        }

        if (peakIndices.size < 5) return null

        // 3. Compute intervals between peaks
        val intervals = mutableListOf<Float>()
        for (i in 1 until peakIndices.size) {
            val intervalMs = (peakIndices[i] - peakIndices[i - 1]) * windowDurationMs
            // Filter realistic beat intervals (e.g. between 60 BPM and 200 BPM -> 300ms to 1000ms)
            if (intervalMs in 300f..1000f) {
                intervals.add(intervalMs)
            }
        }

        if (intervals.isEmpty()) return null

        // 4. Histogram to find the most common interval
        // Bin size ~ 10ms
        val binSize = 10f
        val histogram = mutableMapOf<Int, Int>()
        for (interval in intervals) {
            val bin = (interval / binSize).toInt()
            histogram[bin] = histogram.getOrDefault(bin, 0) + 1
        }

        val bestBin = histogram.maxByOrNull { it.value }?.key ?: return null
        val bestIntervalMs = bestBin * binSize

        // Convert interval to BPM
        val bpm = 60000f / bestIntervalMs

        // Validate
        return if (bpm in 60f..200f) bpm else null
    }
}
