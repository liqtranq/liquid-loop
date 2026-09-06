package com.liquidloop.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.AllInclusive
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PictureInPictureAlt
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.liquidloop.app.model.LoopState
import com.liquidloop.app.model.PlaybackState
import com.liquidloop.app.ui.theme.LiquidBackground
import com.liquidloop.app.ui.theme.LiquidCardBorder
import com.liquidloop.app.ui.theme.LiquidCyan
import com.liquidloop.app.ui.theme.LiquidPink
import com.liquidloop.app.ui.theme.LiquidPurpleLight
import com.liquidloop.app.ui.theme.LiquidSurface
import com.liquidloop.app.ui.theme.LiquidSurfaceVariant
import com.liquidloop.app.ui.theme.LiquidTextMuted
import com.liquidloop.app.ui.theme.LiquidTextPrimary
import com.liquidloop.app.ui.theme.LiquidTextSecondary

@Composable
fun PlaybackControls(
    playbackState: PlaybackState,
    loopState: LoopState,
    onTogglePlayPause: () -> Unit,
    onRestartLoop: () -> Unit,
    onToggleLoop: () -> Unit,
    onSpeedChange: (Float) -> Unit,
    onLaunchPiP: () -> Unit,
    onLaunchFloatingWidget: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Speed selector chips
        SpeedSelectorRow(
            currentSpeed = playbackState.playbackSpeed,
            onSpeedChange = onSpeedChange
        )

        // Main action row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Picture-in-Picture / Overlay trigger
            IconButton(
                onClick = onLaunchFloatingWidget,
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(LiquidSurfaceVariant)
            ) {
                Icon(
                    imageVector = Icons.Default.Layers,
                    contentDescription = "Floating Overlay Widget",
                    tint = LiquidCyan,
                    modifier = Modifier.size(22.dp)
                )
            }

            // Restart from Loop Point A
            IconButton(
                onClick = onRestartLoop,
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(LiquidSurfaceVariant)
            ) {
                Icon(
                    imageVector = Icons.Default.Replay,
                    contentDescription = "Restart Loop at A",
                    tint = LiquidTextPrimary,
                    modifier = Modifier.size(26.dp)
                )
            }

            // Glowing Liquid Play / Pause FAB
            val fabBrush = Brush.linearGradient(
                listOf(LiquidCyan, LiquidPurpleLight)
            )
            Box(
                modifier = Modifier
                    .size(76.dp)
                    .shadow(16.dp, CircleShape, spotColor = LiquidCyan, ambientColor = LiquidCyan)
                    .clip(CircleShape)
                    .background(fabBrush),
                contentAlignment = Alignment.Center
            ) {
                IconButton(
                    onClick = onTogglePlayPause,
                    modifier = Modifier.size(76.dp)
                ) {
                    Icon(
                        imageVector = if (playbackState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (playbackState.isPlaying) "Pause" else "Play",
                        tint = LiquidBackground,
                        modifier = Modifier.size(40.dp)
                    )
                }
            }

            // Loop Mode Toggle (Looping vs Free Play)
            val loopActiveColor by animateColorAsState(
                targetValue = if (loopState.isEnabled) LiquidCyan else LiquidTextMuted,
                label = "loopColor"
            )
            IconButton(
                onClick = onToggleLoop,
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(if (loopState.isEnabled) LiquidCyan.copy(alpha = 0.15f) else LiquidSurfaceVariant)
            ) {
                Icon(
                    imageVector = Icons.Default.AllInclusive,
                    contentDescription = "Toggle Loop Mode",
                    tint = loopActiveColor,
                    modifier = Modifier.size(28.dp)
                )
            }

            // Native PiP button
            IconButton(
                onClick = onLaunchPiP,
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(LiquidSurfaceVariant)
            ) {
                Icon(
                    imageVector = Icons.Default.PictureInPictureAlt,
                    contentDescription = "Picture in Picture",
                    tint = LiquidTextSecondary,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}

@Composable
fun SpeedSelectorRow(
    currentSpeed: Float,
    onSpeedChange: (Float) -> Unit
) {
    val speeds = listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f)

    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        speeds.forEach { speed ->
            val isSelected = currentSpeed == speed
            Surface(
                onClick = { onSpeedChange(speed) },
                shape = RoundedCornerShape(8.dp),
                color = if (isSelected) LiquidCyan else LiquidSurfaceVariant,
                border = BorderStroke(1.dp, if (isSelected) LiquidCyan else LiquidCardBorder),
                modifier = Modifier.height(28.dp)
            ) {
                Box(
                    modifier = Modifier.padding(horizontal = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "${speed}x",
                        color = if (isSelected) LiquidBackground else LiquidTextSecondary,
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }
    }
}
