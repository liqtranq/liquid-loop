package com.liquidloop.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Waves
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.liquidloop.app.model.TrackInfo
import com.liquidloop.app.ui.components.LoopControls
import com.liquidloop.app.ui.components.PlaybackControls
import com.liquidloop.app.ui.components.TrackHeader
import com.liquidloop.app.ui.components.WaveformCanvas
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
import com.liquidloop.app.ui.viewmodel.PlayerViewModel

@Composable
fun PlayerScreen(
    viewModel: PlayerViewModel,
    onImportAudio: () -> Unit,
    onEnterPiP: () -> Unit,
    onLaunchFloatingWidget: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currentTrack by viewModel.currentTrack.collectAsState()
    val playbackState by viewModel.playbackState.collectAsState()
    val loopState by viewModel.loopState.collectAsState()
    val waveform by viewModel.waveform.collectAsState()
    val isLoadingWaveform by viewModel.isLoadingWaveform.collectAsState()
    val isInPiP by viewModel.isInPictureInPicture.collectAsState()

    if (isInPiP) {
        // Compact Picture-in-Picture UI
        PiPPlayerContent(
            trackInfo = currentTrack,
            isPlaying = playbackState.isPlaying,
            currentPosition = playbackState.formattedPosition,
            loopDuration = loopState.formattedDuration,
            onTogglePlayPause = { viewModel.togglePlayPause() },
            onRestartLoop = { viewModel.restartLoop() }
        )
    } else {
        // Fullscreen Liquid Player UI
        Scaffold(
            containerColor = LiquidBackground,
            topBar = {
                LiquidTopBar()
            }
        ) { innerPadding ->
            Column(
                modifier = modifier
                    .fillMaxSize()
                    .systemBarsPadding()
                    .padding(horizontal = 16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 1. Track Info Header Card
                TrackHeader(
                    trackInfo = currentTrack,
                    onImportClick = onImportAudio
                )

                // 2. Waveform Visualizer Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = LiquidSurface),
                    border = BorderStroke(1.dp, LiquidCardBorder)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Waveform Header: Current Position / Total Track Time
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = playbackState.formattedPosition,
                                color = LiquidCyan,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = currentTrack.formattedDuration,
                                color = LiquidTextSecondary,
                                fontSize = 13.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        // Canvas Waveform with A-B handles
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            WaveformCanvas(
                                waveform = waveform,
                                playbackState = playbackState,
                                loopState = loopState,
                                onSeek = { viewModel.seekTo(it) },
                                onLoopPointsChanged = { a, b -> viewModel.setLoopPoints(a, b) }
                            )

                            if (isLoadingWaveform) {
                                CircularProgressIndicator(
                                    color = LiquidCyan,
                                    strokeWidth = 3.dp,
                                    modifier = Modifier.size(36.dp)
                                )
                            }
                        }
                    }
                }

                // 3. Precision Loop Tuning Controls
                LoopControls(
                    loopState = loopState,
                    durationMs = playbackState.durationMs,
                    onLoopPointsChanged = { a, b -> viewModel.setLoopPoints(a, b) },
                    onNudgeA = { viewModel.nudgeLoopA(it) },
                    onNudgeB = { viewModel.nudgeLoopB(it) },
                    onSetAToCurrent = { viewModel.setPointAToCurrent() },
                    onSetBToCurrent = { viewModel.setPointBToCurrent() }
                )

                // 4. Main Playback Controls
                PlaybackControls(
                    playbackState = playbackState,
                    loopState = loopState,
                    onTogglePlayPause = { viewModel.togglePlayPause() },
                    onRestartLoop = { viewModel.restartLoop() },
                    onToggleLoop = { viewModel.toggleLoop() },
                    onSpeedChange = { viewModel.setPlaybackSpeed(it) },
                    onLaunchPiP = onEnterPiP,
                    onLaunchFloatingWidget = onLaunchFloatingWidget
                )

                // 5. Musical Tools (Pitch, Metronome)
                MusicalControls(
                    playbackState = playbackState,
                    onPitchChange = { viewModel.setPitch(it) },
                    onToggleMetronome = { viewModel.toggleMetronome() },
                    onBpmChange = { viewModel.setBpm(it) },
                    onTimeSignatureChange = { viewModel.setTimeSignature(it) }
                )

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
fun LiquidTopBar() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        Brush.linearGradient(listOf(LiquidCyan, LiquidPurpleLight))
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Waves,
                    contentDescription = null,
                    tint = LiquidBackground,
                    modifier = Modifier.size(20.dp)
                )
            }

            Text(
                text = "LiquidLoop",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 0.5.sp
            )
        }

        Surface(
            shape = RoundedCornerShape(20.dp),
            color = LiquidSurfaceVariant,
            border = BorderStroke(0.5.dp, LiquidCardBorder)
        ) {
            Text(
                text = "Gapless A-B Looper",
                color = LiquidCyan,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
            )
        }
    }
}

@Composable
fun PiPPlayerContent(
    trackInfo: TrackInfo,
    isPlaying: Boolean,
    currentPosition: String,
    loopDuration: String,
    onTogglePlayPause: () -> Unit,
    onRestartLoop: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = LiquidBackground
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = trackInfo.title,
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "$currentPosition · Loop: $loopDuration",
                color = LiquidCyan,
                fontSize = 9.sp,
                fontFamily = FontFamily.Monospace
            )

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onRestartLoop,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Replay,
                        contentDescription = "Restart Loop",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }

                IconButton(
                    onClick = onTogglePlayPause,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(LiquidCyan)
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) "Pause" else "Play",
                        tint = LiquidBackground,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun MusicalControls(
    playbackState: com.liquidloop.app.model.PlaybackState,
    onPitchChange: (Float) -> Unit,
    onToggleMetronome: () -> Unit,
    onBpmChange: (Float) -> Unit,
    onTimeSignatureChange: (Int) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = LiquidSurface),
        border = BorderStroke(1.dp, LiquidCardBorder)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Pitch / Transpose
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(Icons.Default.Tune, contentDescription = "Pitch", tint = LiquidPurpleLight)
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = "Pitch (Transpose): ${if (playbackState.pitch >= 1f) "+" else ""}${String.format(java.util.Locale.US, "%.1f", (playbackState.pitch - 1f) * 12f)} st", color = LiquidTextPrimary, fontSize = 13.sp)
                    Slider(
                        value = playbackState.pitch,
                        onValueChange = onPitchChange,
                        valueRange = 0.5f..2.0f,
                        steps = 23,
                        colors = androidx.compose.material3.SliderDefaults.colors(
                            thumbColor = LiquidPurpleLight,
                            activeTrackColor = LiquidPurpleLight.copy(alpha = 0.7f)
                        )
                    )
                }
            }

            // Metronome
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(Icons.Default.Timer, contentDescription = "Metronome", tint = LiquidCyan)
                    Column {
                        Text(text = "Metronome Grid", color = LiquidTextPrimary, fontSize = 13.sp)
                        Text(text = "${playbackState.bpm.toInt()} BPM  |  ${playbackState.timeSignature}/4", color = LiquidCyan, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                    }
                }
                
                Switch(
                    checked = playbackState.isMetronomeEnabled,
                    onCheckedChange = { onToggleMetronome() },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = LiquidBackground,
                        checkedTrackColor = LiquidCyan
                    )
                )
            }
        }
    }
}
