package com.liquidloop.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Add
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
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
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
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.window.Popup
import androidx.compose.ui.input.pointer.pointerInput
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
    val updateInfo by viewModel.updateInfo.collectAsState()
    val manualUpdateInfo by viewModel.manualUpdateInfo.collectAsState()
    
    val showSpeedControl by viewModel.showSpeedControl.collectAsState()
    val showPitchControl by viewModel.showPitchControl.collectAsState()
    val showMetronomeControl by viewModel.showMetronomeControl.collectAsState()
    val isBeatGridEnabled by viewModel.isBeatGridEnabled.collectAsState()

    updateInfo?.let { info ->
        UpdateDialog(
            updateInfo = info,
            onDismiss = { viewModel.dismissUpdate() }
        )
    }

    manualUpdateInfo?.let { info ->
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { viewModel.dismissManualUpdate() },
            containerColor = LiquidSurface,
            titleContentColor = LiquidCyan,
            textContentColor = LiquidTextPrimary,
            title = {
                Text(text = "App is up to date!", fontWeight = FontWeight.Bold)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(text = "Version ${info.latestVersion}", fontWeight = FontWeight.SemiBold)
                    Text(text = "What's new:", color = LiquidTextSecondary, fontSize = 12.sp)
                    val cleanNotes = info.releaseNotes
                        .replace(Regex("(?m)^#+\\s*"), "") // remove headers
                        .replace(Regex("\\*\\*(.*?)\\*\\*"), "$1") // remove bold
                        .replace(Regex("__(.*?)__"), "$1") // remove bold
                        .replace(Regex("\\*(.*?)\\*"), "$1") // remove italic
                        .replace(Regex("_(.*?)_"), "$1") // remove italic
                        .replace(Regex("`([^`]+)`"), "$1") // remove code
                        .replace(Regex("\\[(.*?)\\]\\(.*?\\)"), "$1") // remove links
                    Text(text = cleanNotes, fontSize = 14.sp)
                }
            },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = { viewModel.dismissManualUpdate() }) {
                    Text("Close", color = LiquidCyan)
                }
            }
        )
    }

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
                LiquidTopBar(
                    onUpdateClick = { viewModel.checkUpdatesManually() },
                    showSpeedControl = showSpeedControl,
                    showPitchControl = showPitchControl,
                    showMetronomeControl = showMetronomeControl,
                    isBeatGridEnabled = isBeatGridEnabled,
                    onToggleSpeedControl = { viewModel.toggleSpeedControl() },
                    onTogglePitchControl = { viewModel.togglePitchControl() },
                    onToggleMetronomeControl = { viewModel.toggleMetronomeControl() },
                    onToggleBeatGrid = { viewModel.toggleBeatGrid() }
                )
            }
        ) { innerPadding ->
            Column(
                modifier = modifier
                    .fillMaxSize()
                    .padding(innerPadding)
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
                                isBeatGridEnabled = isBeatGridEnabled,
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


                // 3. Loop Controls (General A-B Slider & Stats)
                com.liquidloop.app.ui.components.LoopControls(
                    loopState = loopState,
                    durationMs = playbackState.durationMs,
                    onLoopPointsChanged = { a, b -> viewModel.setLoopPoints(a, b) },
                    onNudgeA = { viewModel.nudgeLoopA(it) },
                    onNudgeB = { viewModel.nudgeLoopB(it) },
                    onSetAToCurrent = { viewModel.setPointAToCurrent() },
                    onSetBToCurrent = { viewModel.setPointBToCurrent() }
                )
                PlaybackControls(
                    playbackState = playbackState,
                    loopState = loopState,
                    onTogglePlayPause = { viewModel.togglePlayPause() },
                    onRestartLoop = { viewModel.restartLoop() },
                    onToggleLoop = { viewModel.toggleLoop() },
                    onSpeedChange = { viewModel.setPlaybackSpeed(it) },
                    showSpeedControl = showSpeedControl
                )

                // 5. Musical Tools (Pitch, Metronome)
                MusicalControls(
                    playbackState = playbackState,
                    showPitchControl = showPitchControl,
                    showMetronomeControl = showMetronomeControl,
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
fun LiquidTopBar(
    onUpdateClick: () -> Unit,
    showSpeedControl: Boolean,
    showPitchControl: Boolean,
    showMetronomeControl: Boolean,
    isBeatGridEnabled: Boolean,
    onToggleSpeedControl: () -> Unit,
    onTogglePitchControl: () -> Unit,
    onToggleMetronomeControl: () -> Unit,
    onToggleBeatGrid: () -> Unit
) {
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
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.clickable { onUpdateClick() }
        ) {
            androidx.compose.foundation.Image(
                painter = androidx.compose.ui.res.painterResource(id = com.liquidloop.app.R.mipmap.ic_launcher_foreground),
                contentDescription = "Check for updates",
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.Black)
            )

            Text(
                text = "Liquid Loop",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 0.5.sp
            )
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            var showSettingsMenu by remember { mutableStateOf(false) }
            var hoverIndex by remember { mutableStateOf(-1) }

            Box(
                modifier = Modifier.pointerInput(Unit) {
                    awaitEachGesture {
                        val down = awaitFirstDown()
                        val wasOpenBeforeTap = showSettingsMenu
                        
                        if (!wasOpenBeforeTap) {
                            showSettingsMenu = true
                        }
                        
                        hoverIndex = -1
                        var didDrag = false
                        val startPos = down.position
                        
                        do {
                            val event = awaitPointerEvent()
                            val ptr = event.changes.firstOrNull { it.id == down.id }
                            if (ptr != null) {
                                val y = ptr.position.y
                                val dist = (ptr.position - startPos).getDistance()
                                if (dist > 10f) {
                                    didDrag = true
                                }
                                
                                val topPadding = 40.dp.toPx()
                                val itemHeight = 48.dp.toPx()
                                
                                if (y > topPadding) {
                                    val newHover = ((y - topPadding) / itemHeight).toInt()
                                    hoverIndex = newHover.coerceIn(0, 3)
                                } else {
                                    hoverIndex = -1
                                }
                            }
                        } while (event.changes.any { it.pressed })
                        
                        // Pointer released
                        if (didDrag) {
                            if (hoverIndex != -1) {
                                when (hoverIndex) {
                                    0 -> onToggleSpeedControl()
                                    1 -> onTogglePitchControl()
                                    2 -> onToggleMetronomeControl()
                                    3 -> onToggleBeatGrid()
                                }
                                showSettingsMenu = false
                            } else {
                                showSettingsMenu = false
                            }
                        } else {
                            // Tap behavior
                            if (wasOpenBeforeTap) {
                                showSettingsMenu = false
                            }
                        }
                        hoverIndex = -1
                    }
                }
            ) {
                // Settings Icon
                Box(
                    modifier = Modifier.size(32.dp).clip(CircleShape).background(if (showSettingsMenu) LiquidSurface else LiquidSurfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = androidx.compose.material.icons.Icons.Default.Tune,
                        contentDescription = "Settings",
                        tint = LiquidCyan,
                        modifier = Modifier.size(18.dp)
                    )
                }

                if (showSettingsMenu) {
                    Popup(
                        alignment = Alignment.TopEnd,
                        offset = androidx.compose.ui.unit.IntOffset(0, 100),
                        onDismissRequest = { showSettingsMenu = false }
                    ) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = LiquidSurface),
                            border = BorderStroke(1.dp, LiquidCardBorder),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.width(200.dp)
                        ) {
                            Column(modifier = Modifier.padding(vertical = 8.dp)) {
                                val items = listOf(
                                    if (showSpeedControl) "Hide Speed Control" else "Show Speed Control",
                                    if (showPitchControl) "Hide Pitch Control" else "Show Pitch Control",
                                    if (showMetronomeControl) "Hide Metronome" else "Show Metronome",
                                    if (isBeatGridEnabled) "Hide Grid" else "Show Grid"
                                )
                                items.forEachIndexed { index, text ->
                                    val isHovered = index == hoverIndex
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(48.dp)
                                            .background(if (isHovered) LiquidSurfaceVariant else Color.Transparent)
                                            .clickable {
                                                when (index) {
                                                    0 -> onToggleSpeedControl()
                                                    1 -> onTogglePitchControl()
                                                    2 -> onToggleMetronomeControl()
                                                    3 -> onToggleBeatGrid()
                                                }
                                                showSettingsMenu = false
                                            }
                                            .padding(horizontal = 16.dp),
                                        contentAlignment = Alignment.CenterStart
                                    ) {
                                        Text(text = text, color = if (isHovered) LiquidCyan else LiquidTextPrimary, fontSize = 14.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
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
    showPitchControl: Boolean,
    showMetronomeControl: Boolean,
    onPitchChange: (Float) -> Unit,
    onToggleMetronome: () -> Unit,
    onBpmChange: (Float) -> Unit,
    onTimeSignatureChange: (Int) -> Unit
) {
    if (!showPitchControl && !showMetronomeControl) return

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
            if (showPitchControl) {
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
            }

            // Metronome
            if (showMetronomeControl) {
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
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(text = "Metronome", color = LiquidTextPrimary, fontSize = 13.sp)
                                
                                var tapTimes by remember { mutableStateOf(listOf<Long>()) }
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = LiquidSurfaceVariant,
                                    border = BorderStroke(1.dp, LiquidCardBorder),
                                    modifier = Modifier.clickable {
                                        val now = System.currentTimeMillis()
                                        // Keep taps within the last 3 seconds
                                        val recentTaps = tapTimes.filter { now - it < 3000 }.toMutableList()
                                        recentTaps.add(now)
                                        tapTimes = recentTaps
                                        
                                        if (recentTaps.size >= 2) {
                                            val intervals = recentTaps.zipWithNext { a, b -> b - a }
                                            val avgInterval = intervals.average()
                                            if (avgInterval > 0) {
                                                val newBpm = (60000.0 / avgInterval).toFloat().coerceIn(40f, 300f)
                                                onBpmChange(newBpm)
                                            }
                                        }
                                    }
                                ) {
                                    Text(
                                        text = "TAP",
                                        color = LiquidCyan,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                var showBpmDialog by remember { mutableStateOf(false) }
                                
                                if (showBpmDialog) {
                                    var bpmInput by remember { mutableStateOf(playbackState.bpm.toInt().toString()) }
                                    androidx.compose.material3.AlertDialog(
                                        onDismissRequest = { showBpmDialog = false },
                                        containerColor = LiquidSurface,
                                        title = { Text("Set BPM", color = LiquidCyan) },
                                        text = {
                                            androidx.compose.material3.OutlinedTextField(
                                                value = bpmInput,
                                                onValueChange = { bpmInput = it },
                                                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                                    keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                                                ),
                                                textStyle = androidx.compose.ui.text.TextStyle(color = LiquidTextPrimary),
                                                colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                                                    focusedBorderColor = LiquidCyan,
                                                    unfocusedBorderColor = LiquidCardBorder
                                                )
                                            )
                                        },
                                        confirmButton = {
                                            androidx.compose.material3.TextButton(onClick = {
                                                bpmInput.toFloatOrNull()?.let {
                                                    onBpmChange(it.coerceIn(40f, 300f))
                                                }
                                                showBpmDialog = false
                                            }) {
                                                Text("Set", color = LiquidCyan)
                                            }
                                        },
                                        dismissButton = {
                                            androidx.compose.material3.TextButton(onClick = { showBpmDialog = false }) {
                                                Text("Cancel", color = LiquidTextSecondary)
                                            }
                                        }
                                    )
                                }
                                
                                Text(
                                    text = "${playbackState.bpm.toInt()} BPM  |  ",
                                    color = LiquidCyan,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    modifier = Modifier.clickable { showBpmDialog = true }.padding(vertical = 4.dp)
                                )

                                var showTimeSigDialog by remember { mutableStateOf(false) }
                                if (showTimeSigDialog) {
                                    androidx.compose.material3.AlertDialog(
                                        onDismissRequest = { showTimeSigDialog = false },
                                        containerColor = LiquidSurface,
                                        title = { Text("Time Signature", color = LiquidCyan) },
                                        text = {
                                            Column {
                                                listOf(3, 4, 5, 6, 7).forEach { sig ->
                                                    Text(
                                                        text = "$sig/4",
                                                        color = LiquidTextPrimary,
                                                        fontSize = 18.sp,
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .clickable {
                                                                onTimeSignatureChange(sig)
                                                                showTimeSigDialog = false
                                                            }
                                                            .padding(12.dp)
                                                    )
                                                }
                                            }
                                        },
                                        confirmButton = {}
                                    )
                                }

                                Text(
                                    text = "${playbackState.timeSignature}/4",
                                    color = LiquidCyan,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    modifier = Modifier.clickable { showTimeSigDialog = true }.padding(vertical = 4.dp)
                                )
                            }
                        }
                    }
                    
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        androidx.compose.material3.IconButton(
                            onClick = { onBpmChange((playbackState.bpm - 1).coerceAtLeast(40f)) },
                            modifier = Modifier.size(32.dp).clip(CircleShape).background(LiquidSurfaceVariant)
                        ) {
                            Text("-", color = LiquidCyan, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }
                        
                        Switch(
                            checked = playbackState.isMetronomeEnabled,
                            onCheckedChange = { onToggleMetronome() },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = LiquidBackground,
                                checkedTrackColor = LiquidCyan
                            )
                        )
                        
                        androidx.compose.material3.IconButton(
                            onClick = { onBpmChange((playbackState.bpm + 1).coerceAtMost(300f)) },
                            modifier = Modifier.size(32.dp).clip(CircleShape).background(LiquidSurfaceVariant)
                        ) {
                            Text("+", color = LiquidCyan, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun UpdateDialog(
    updateInfo: com.liquidloop.app.core.network.UpdateChecker.UpdateInfo,
    onDismiss: () -> Unit
) {
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = LiquidSurface,
        titleContentColor = LiquidCyan,
        textContentColor = LiquidTextPrimary,
        title = {
            Text(text = "Update Available", fontWeight = FontWeight.Bold)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(text = "Version ${updateInfo.latestVersion} is available!", fontWeight = FontWeight.SemiBold)
                val cleanNotes = updateInfo.releaseNotes
                    .replace(Regex("(?m)^#+\\s*"), "")
                    .replace(Regex("\\*\\*(.*?)\\*\\*"), "$1")
                    .replace(Regex("__(.*?)__"), "$1")
                    .replace(Regex("\\*(.*?)\\*"), "$1")
                    .replace(Regex("_(.*?)_"), "$1")
                    .replace(Regex("`([^`]+)`"), "$1")
                    .replace(Regex("\\[(.*?)\\]\\(.*?\\)"), "$1")
                Text(text = cleanNotes, fontSize = 14.sp, color = LiquidTextSecondary)
            }
        },
        confirmButton = {
            val context = androidx.compose.ui.platform.LocalContext.current
            androidx.compose.material3.Button(
                onClick = {
                    val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(updateInfo.downloadUrl))
                    context.startActivity(intent)
                    onDismiss()
                },
                colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = LiquidCyan)
            ) {
                Text("Download", color = LiquidBackground)
            }
        },
        dismissButton = {
            androidx.compose.material3.TextButton(onClick = onDismiss) {
                Text("Later", color = LiquidTextSecondary)
            }
        }
    )
}


@Composable
fun GridControls(
    playbackState: com.liquidloop.app.model.PlaybackState,
    onTimeSignatureChange: (Int) -> Unit,
    onGridResolutionChange: (Int) -> Unit,
    onToggleSnap: () -> Unit
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
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(androidx.compose.material.icons.Icons.Default.Menu, contentDescription = "Grid", tint = LiquidCyan)
                    Text(text = "Grid & Snap", color = LiquidTextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
                
                // Time Signature Dialog
                var showTimeSigDialog by remember { mutableStateOf(false) }
                if (showTimeSigDialog) {
                    androidx.compose.material3.AlertDialog(
                        onDismissRequest = { showTimeSigDialog = false },
                        containerColor = LiquidSurface,
                        title = { Text("Time Signature", color = LiquidCyan) },
                        text = {
                            Column {
                                listOf(3, 4, 5, 6, 7).forEach { sig ->
                                    Text(
                                        text = "/4",
                                        color = LiquidTextPrimary,
                                        fontSize = 18.sp,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                onTimeSignatureChange(sig)
                                                showTimeSigDialog = false
                                            }
                                            .padding(12.dp)
                                    )
                                }
                            }
                        },
                        confirmButton = {}
                    )
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = LiquidSurfaceVariant,
                    modifier = Modifier.clickable { showTimeSigDialog = true }
                ) {
                    Text(
                        text = "Sig: /4",
                        color = LiquidCyan,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Snap Toggle
                IconButton(
                    onClick = onToggleSnap,
                    modifier = Modifier.size(36.dp).clip(CircleShape).background(if (playbackState.isSnapEnabled) LiquidCyan else LiquidSurfaceVariant)
                ) {
                    Icon(
                        imageVector = androidx.compose.material.icons.Icons.Default.Add,
                        contentDescription = "Snap to Grid",
                        tint = if (playbackState.isSnapEnabled) Color.Black else LiquidCyan,
                        modifier = Modifier.size(18.dp)
                    )
                }
                
                // Resolution Slider
                Column(modifier = Modifier.weight(1f)) {
                    val labels = listOf("2 Bars", "1 Bar", "1", "1/2", "1/4")
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        labels.forEachIndexed { index, label ->
                            Text(
                                text = label,
                                color = if (playbackState.gridResolutionIndex == index) LiquidCyan else LiquidTextSecondary,
                                fontSize = 11.sp,
                                fontWeight = if (playbackState.gridResolutionIndex == index) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                    androidx.compose.material3.Slider(
                        value = playbackState.gridResolutionIndex.toFloat(),
                        onValueChange = { onGridResolutionChange(it.toInt()) },
                        valueRange = 0f..4f,
                        steps = 3,
                        colors = androidx.compose.material3.SliderDefaults.colors(
                            thumbColor = LiquidCyan,
                            activeTrackColor = LiquidCyan.copy(alpha = 0.7f),
                            inactiveTrackColor = LiquidSurfaceVariant
                        )
                    )
                }
            }
        }
    }
}
