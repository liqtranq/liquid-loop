package com.liquidloop.app.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.liquidloop.app.model.LoopState
import com.liquidloop.app.model.PlaybackState
import com.liquidloop.app.model.TrackInfo
import com.liquidloop.app.ui.theme.LiquidCyan
import com.liquidloop.app.ui.theme.LiquidCyanDim
import com.liquidloop.app.ui.theme.LiquidPurpleLight
import com.liquidloop.app.ui.theme.LiquidSurfaceVariant
import com.liquidloop.app.ui.theme.LiquidTextMuted
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.drawscope.translate
import com.liquidloop.app.ui.theme.LoopRegionOverlay
import com.liquidloop.app.ui.theme.MarkerAColor
import com.liquidloop.app.ui.theme.MarkerBColor
import com.liquidloop.app.ui.theme.PlayheadColor
import com.liquidloop.app.ui.theme.WaveformActive
import com.liquidloop.app.ui.theme.WaveformInactive
import kotlin.math.abs

private enum class DragTarget {
    NONE, MARKER_A, MARKER_B, PLAYHEAD
}

@Composable
fun WaveformCanvas(
    waveform: FloatArray,
    playbackState: PlaybackState,
    loopState: LoopState,
    isBeatGridEnabled: Boolean,
    onSeek: (Long) -> Unit,
    onLoopPointsChanged: (Long, Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val durationMs = playbackState.durationMs.coerceAtLeast(1L)
    var activeDragTarget by remember { mutableStateOf(DragTarget.NONE) }
    var scale by remember { androidx.compose.runtime.mutableFloatStateOf(1f) }
    var offsetX by remember { androidx.compose.runtime.mutableFloatStateOf(0f) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(200.dp)
            .clip(RoundedCornerShape(16.dp))
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTransformGestures { centroid, pan, zoom, _ ->
                        val oldScale = scale
                        scale = (oldScale * zoom).coerceIn(1f, 20f)
                        
                        // Scale around centroid
                        offsetX = (offsetX - centroid.x) * (scale / oldScale) + centroid.x
                        
                        // Add pan
                        offsetX += pan.x
                        
                        // Bound offset
                        val minOffset = -(size.width * scale - size.width)
                        offsetX = offsetX.coerceIn(minOffset, 0f)
                    }
                }
                .pointerInput(waveform, durationMs, scale, offsetX) {
                    detectTapGestures { offset ->
                        val virtualWidth = size.width * scale
                        val tapRatio = ((offset.x - offsetX) / virtualWidth).coerceIn(0f, 1f)
                        val tappedMs = (tapRatio * durationMs).toLong()
                        onSeek(tappedMs)
                    }
                }
        ) {
            val canvasWidth = size.width
            val canvasHeight = size.height
            val centerY = canvasHeight / 2f
            
            val virtualWidth = canvasWidth * scale

            withTransform({
                translate(left = offsetX, top = 0f)
            }) {
                // 1. Draw subtle background grid / track line
                drawLine(
                    color = LiquidSurfaceVariant,
                    start = Offset(0f, centerY),
                    end = Offset(virtualWidth, centerY),
                    strokeWidth = 2f
                )

                val aX = (loopState.startMs.toFloat() / durationMs) * virtualWidth
                val bX = (loopState.endMs.toFloat() / durationMs) * virtualWidth
                val playheadX = (playbackState.currentPositionMs.toFloat() / durationMs) * virtualWidth

            // 2. Draw Active Loop Region Highlight
            if (loopState.isEnabled && bX > aX) {
                drawRoundRect(
                    brush = Brush.verticalGradient(
                        listOf(
                            LiquidCyan.copy(alpha = 0.22f),
                            LiquidPurpleLight.copy(alpha = 0.12f),
                            LiquidCyan.copy(alpha = 0.22f)
                        )
                    ),
                    topLeft = Offset(aX, 10f),
                    size = Size(bX - aX, canvasHeight - 20f),
                    cornerRadius = CornerRadius(8f, 8f)
                )

                // Top & Bottom boundary glow lines for the loop region
                drawLine(
                    color = LiquidCyan.copy(alpha = 0.6f),
                    start = Offset(aX, 10f),
                    end = Offset(bX, 10f),
                    strokeWidth = 2f
                )
                drawLine(
                    color = LiquidPurpleLight.copy(alpha = 0.6f),
                    start = Offset(aX, canvasHeight - 10f),
                    end = Offset(bX, canvasHeight - 10f),
                    strokeWidth = 2f
                )
            }

            // 2.5 Draw Beat Grid
            if (isBeatGridEnabled) {
                val bpm = playbackState.bpm
                val ts = playbackState.timeSignature
                if (bpm > 0) {
                    val beatIntervalMs = (60000f / bpm)
                    var currentBeatMs = 0f
                    var beatCount = 0
                    while (currentBeatMs < durationMs) {
                        val x = (currentBeatMs / durationMs) * virtualWidth
                        val isDownbeat = (beatCount % ts == 0)
                        
                        drawLine(
                            color = if (isDownbeat) LiquidPurpleLight.copy(alpha = 0.3f) else LiquidTextMuted.copy(alpha = 0.1f),
                            start = Offset(x, 0f),
                            end = Offset(x, canvasHeight),
                            strokeWidth = if (isDownbeat) 2f else 1f
                        )
                        
                        currentBeatMs += beatIntervalMs
                        beatCount++
                    }
                }
            }

            // 3. Draw Waveform Bars
            val barsCount = waveform.size
            if (barsCount > 0) {
                val barWidth = (virtualWidth / barsCount) * 0.7f
                val barGap = (virtualWidth / barsCount) * 0.3f

                val maxBarHeight = (canvasHeight * 0.75f) / 2f

                for (i in 0 until barsCount) {
                    val x = i * (barWidth + barGap) + (barWidth / 2f)
                    val amp = waveform[i].coerceIn(0.05f, 1.0f)
                    val barHeight = amp * maxBarHeight

                    val isInLoop = loopState.isEnabled && (x in aX..bX)
                    val isPastPlayhead = x <= playheadX

                    val barBrush = when {
                        isInLoop -> Brush.verticalGradient(
                            listOf(LiquidCyan, LiquidPurpleLight)
                        )
                        isPastPlayhead -> Brush.verticalGradient(
                            listOf(LiquidCyan.copy(alpha = 0.5f), LiquidCyanDim.copy(alpha = 0.3f))
                        )
                        else -> Brush.verticalGradient(
                            listOf(WaveformInactive, WaveformInactive.copy(alpha = 0.6f))
                        )
                    }

                    // Draw symmetric top & bottom rounded bar
                    drawRoundRect(
                        brush = barBrush,
                        topLeft = Offset(x - barWidth / 2f, centerY - barHeight),
                        size = Size(barWidth, barHeight * 2f),
                        cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
                    )
                }
            }

            // 4. Draw Marker A (Start of Loop)
            if (loopState.isEnabled) {
                drawLoopMarker(
                    x = aX,
                    color = MarkerAColor,
                    label = "A",
                    timeText = TrackInfo.formatShortTimeMs(loopState.startMs),
                    isStart = true,
                    canvasHeight = canvasHeight
                )

                // 5. Draw Marker B (End of Loop)
                drawLoopMarker(
                    x = bX,
                    color = MarkerBColor,
                    label = "B",
                    timeText = TrackInfo.formatShortTimeMs(loopState.endMs),
                    isStart = false,
                    canvasHeight = canvasHeight
                )
            }

            // 6. Draw Playhead (Glowing Cursor)
            drawPlayhead(
                x = playheadX,
                canvasHeight = canvasHeight
            )
            } // close withTransform
        }
    }
}

private fun DrawScope.drawLoopMarker(
    x: Float,
    color: Color,
    label: String,
    timeText: String,
    isStart: Boolean,
    canvasHeight: Float
) {
    // Vertical guideline with glow
    drawLine(
        color = color.copy(alpha = 0.4f),
        start = Offset(x, 0f),
        end = Offset(x, canvasHeight),
        strokeWidth = 6f
    )
    drawLine(
        color = color,
        start = Offset(x, 0f),
        end = Offset(x, canvasHeight),
        strokeWidth = 2.5f
    )

    // Top Flag Handle
    val handleWidth = 44f
    val handleHeight = 28f
    val handleTop = 8f
    val handleLeft = if (isStart) x - handleWidth + 4f else x - 4f

    drawRoundRect(
        color = color,
        topLeft = Offset(handleLeft, handleTop),
        size = Size(handleWidth, handleHeight),
        cornerRadius = CornerRadius(6f, 6f)
    )

    // Bottom Anchor Bead
    drawCircle(
        color = color,
        radius = 8f,
        center = Offset(x, canvasHeight - 12f)
    )
    drawCircle(
        color = Color(0xFF0B0E14),
        radius = 4f,
        center = Offset(x, canvasHeight - 12f)
    )

    // Draw Marker Letter inside handle
    val paint = android.graphics.Paint().apply {
        this.color = android.graphics.Color.BLACK
        this.textSize = 28f
        this.isFakeBoldText = true
        this.textAlign = android.graphics.Paint.Align.CENTER
        this.isAntiAlias = true
    }
    drawContext.canvas.nativeCanvas.drawText(
        label,
        handleLeft + handleWidth / 2f,
        handleTop + handleHeight * 0.72f,
        paint
    )
}

private fun DrawScope.drawPlayhead(
    x: Float,
    canvasHeight: Float
) {
    // Outer halo glow
    drawLine(
        color = LiquidCyan.copy(alpha = 0.35f),
        start = Offset(x, 0f),
        end = Offset(x, canvasHeight),
        strokeWidth = 8f
    )

    // Sharp center playhead line
    drawLine(
        color = PlayheadColor,
        start = Offset(x, 0f),
        end = Offset(x, canvasHeight),
        strokeWidth = 2.5f
    )

    // Top Liquid Teardrop Indicator
    val trianglePath = Path().apply {
        moveTo(x - 8f, 0f)
        lineTo(x + 8f, 0f)
        lineTo(x, 14f)
        close()
    }
    drawPath(path = trianglePath, color = LiquidCyan)

    // Center pulsating bead
    drawCircle(
        color = LiquidCyan,
        radius = 5f,
        center = Offset(x, canvasHeight / 2f)
    )
    drawCircle(
        color = Color.White,
        radius = 2.5f,
        center = Offset(x, canvasHeight / 2f)
    )
}
