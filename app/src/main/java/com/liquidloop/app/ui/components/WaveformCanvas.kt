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
    onSeek: (Long) -> Unit,
    onLoopPointsChanged: (Long, Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val durationMs = playbackState.durationMs.coerceAtLeast(1L)
    var activeDragTarget by remember { mutableStateOf(DragTarget.NONE) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(200.dp)
            .clip(RoundedCornerShape(16.dp))
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(waveform, durationMs, loopState) {
                    detectTapGestures { offset ->
                        val tapRatio = (offset.x / size.width).coerceIn(0f, 1f)
                        val tappedMs = (tapRatio * durationMs).toLong()
                        onSeek(tappedMs)
                    }
                }
                .pointerInput(waveform, durationMs, loopState) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            val aX = (loopState.startMs.toFloat() / durationMs) * size.width
                            val bX = (loopState.endMs.toFloat() / durationMs) * size.width
                            val touchRadius = 60f

                            activeDragTarget = when {
                                abs(offset.x - aX) <= touchRadius -> DragTarget.MARKER_A
                                abs(offset.x - bX) <= touchRadius -> DragTarget.MARKER_B
                                else -> DragTarget.PLAYHEAD
                            }
                        },
                        onDragEnd = {
                            activeDragTarget = DragTarget.NONE
                        },
                        onDragCancel = {
                            activeDragTarget = DragTarget.NONE
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            val width = size.width.toFloat().coerceAtLeast(1f)
                            val deltaMs = ((dragAmount.x / width) * durationMs).toLong()

                            when (activeDragTarget) {
                                DragTarget.MARKER_A -> {
                                    val newStart = (loopState.startMs + deltaMs)
                                        .coerceIn(0L, (loopState.endMs - 50L).coerceAtLeast(0L))
                                    onLoopPointsChanged(newStart, loopState.endMs)
                                }
                                DragTarget.MARKER_B -> {
                                    val newEnd = (loopState.endMs + deltaMs)
                                        .coerceIn(loopState.startMs + 50L, durationMs)
                                    onLoopPointsChanged(loopState.startMs, newEnd)
                                }
                                DragTarget.PLAYHEAD -> {
                                    val currentX = (playbackState.currentPositionMs.toFloat() / durationMs) * width
                                    val newX = (currentX + dragAmount.x).coerceIn(0f, width)
                                    val newPos = ((newX / width) * durationMs).toLong()
                                    onSeek(newPos)
                                }
                                DragTarget.NONE -> {}
                            }
                        }
                    )
                }
        ) {
            val canvasWidth = size.width
            val canvasHeight = size.height
            val centerY = canvasHeight / 2f

            // 1. Draw subtle background grid / track line
            drawLine(
                color = LiquidSurfaceVariant,
                start = Offset(0f, centerY),
                end = Offset(canvasWidth, centerY),
                strokeWidth = 2f
            )

            val aX = (loopState.startMs.toFloat() / durationMs) * canvasWidth
            val bX = (loopState.endMs.toFloat() / durationMs) * canvasWidth
            val playheadX = (playbackState.currentPositionMs.toFloat() / durationMs) * canvasWidth

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
            val bpm = playbackState.bpm
            val ts = playbackState.timeSignature
            if (bpm > 0) {
                val beatIntervalMs = (60000f / bpm)
                var currentBeatMs = 0f
                var beatCount = 0
                while (currentBeatMs < durationMs) {
                    val x = (currentBeatMs / durationMs) * canvasWidth
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

            // 3. Draw Waveform Bars
            val barsCount = waveform.size
            if (barsCount > 0) {
                val barWidth = (canvasWidth / barsCount) * 0.7f
                val barGap = (canvasWidth / barsCount) * 0.3f

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
