package com.liquidloop.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AvTimer
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.liquidloop.app.model.LoopState
import com.liquidloop.app.ui.theme.LiquidCardBorder
import com.liquidloop.app.ui.theme.LiquidCyan
import com.liquidloop.app.ui.theme.LiquidPink
import com.liquidloop.app.ui.theme.LiquidPurpleLight
import com.liquidloop.app.ui.theme.LiquidSurface
import com.liquidloop.app.ui.theme.LiquidSurfaceVariant
import com.liquidloop.app.ui.theme.LiquidTextMuted
import com.liquidloop.app.ui.theme.LiquidTextPrimary
import com.liquidloop.app.ui.theme.LiquidTextSecondary
import com.liquidloop.app.ui.theme.MarkerAColor
import com.liquidloop.app.ui.theme.MarkerBColor

import androidx.compose.material3.RangeSlider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.ExperimentalMaterial3Api

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoopControls(
    loopState: LoopState,
    durationMs: Long,
    onLoopPointsChanged: (Long, Long) -> Unit,
    onNudgeA: (Long) -> Unit,
    onNudgeB: (Long) -> Unit,
    onSetAToCurrent: () -> Unit,
    onSetBToCurrent: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Loop Summary Stats Pill
        LoopStatsBar(loopState = loopState)

        if (durationMs > 0L) {
            RangeSlider(
                value = loopState.startMs.toFloat()..loopState.endMs.toFloat(),
                onValueChange = { range ->
                    onLoopPointsChanged(range.start.toLong(), range.endInclusive.toLong())
                },
                valueRange = 0f..durationMs.toFloat(),
                modifier = Modifier.padding(horizontal = 8.dp),
                colors = SliderDefaults.colors(
                    thumbColor = LiquidCyan,
                    activeTrackColor = LiquidPurpleLight
                )
            )
        }

        // Marker A Fine Tuning Panel
        MarkerTuningPanel(
            title = "Marker A (Loop Start)",
            timestamp = loopState.formattedStart,
            markerColor = MarkerAColor,
            onNudge = onNudgeA,
            onSetCurrent = onSetAToCurrent
        )

        // Marker B Fine Tuning Panel
        MarkerTuningPanel(
            title = "Marker B (Loop End)",
            timestamp = loopState.formattedEnd,
            markerColor = MarkerBColor,
            onNudge = onNudgeB,
            onSetCurrent = onSetBToCurrent
        )
    }
}

@Composable
fun LoopStatsBar(loopState: LoopState) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = LiquidSurfaceVariant.copy(alpha = 0.7f),
        border = BorderStroke(1.dp, LiquidCardBorder)
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 10.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Loop Length
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.AvTimer,
                    contentDescription = null,
                    tint = LiquidCyan,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "Loop Length: ",
                    color = LiquidTextSecondary,
                    fontSize = 12.sp
                )
                Text(
                    text = loopState.formattedDuration,
                    color = LiquidCyan,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }

            // BPM Estimate
            val bpm = loopState.estimateBpm(4)
            if (bpm != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Speed,
                        contentDescription = null,
                        tint = LiquidPurpleLight,
                        modifier = Modifier.size(15.dp)
                    )
                    Text(
                        text = String.format(java.util.Locale.US, "~%.1f BPM", bpm),
                        color = LiquidPurpleLight,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
fun MarkerTuningPanel(
    title: String,
    timestamp: String,
    markerColor: Color,
    onNudge: (Long) -> Unit,
    onSetCurrent: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = LiquidSurface),
        border = BorderStroke(1.dp, LiquidCardBorder)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Header Row: Marker dot, title, current timestamp
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(markerColor)
                    )
                    Text(
                        text = title,
                        color = LiquidTextSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                Text(
                    text = timestamp,
                    color = markerColor,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }

            // Nudge Button Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                NudgeChip(label = "-100ms", onClick = { onNudge(-100L) }, modifier = Modifier.weight(1f))
                NudgeChip(label = "-50ms", onClick = { onNudge(-50L) }, modifier = Modifier.weight(1f))
                NudgeChip(label = "+50ms", onClick = { onNudge(50L) }, modifier = Modifier.weight(1f))
                NudgeChip(label = "+100ms", onClick = { onNudge(100L) }, modifier = Modifier.weight(1f))

                // Set to Playhead Button
                OutlinedButton(
                    onClick = onSetCurrent,
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, markerColor.copy(alpha = 0.5f)),
                    modifier = Modifier.height(34.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.MyLocation,
                        contentDescription = "Set to Playhead",
                        tint = markerColor,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Here",
                        color = markerColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
fun NudgeChip(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier.height(34.dp),
        shape = RoundedCornerShape(10.dp),
        color = LiquidSurfaceVariant,
        border = BorderStroke(1.dp, LiquidCardBorder)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = label,
                color = LiquidTextPrimary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}
