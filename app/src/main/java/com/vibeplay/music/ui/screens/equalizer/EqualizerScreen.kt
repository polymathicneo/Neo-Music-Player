package com.vibeplay.music.ui.screens.equalizer

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.vibeplay.music.domain.model.EqualizerBand
import com.vibeplay.music.domain.model.EqualizerPreset
import com.vibeplay.music.domain.model.EqualizerState
import com.vibeplay.music.ui.theme.VibeBackground
import com.vibeplay.music.ui.theme.VibeGreen
import com.vibeplay.music.ui.theme.VibeMutedText
import com.vibeplay.music.ui.theme.VibePrimaryText
import com.vibeplay.music.ui.theme.VibeSecondaryText
import com.vibeplay.music.ui.theme.VibeSurface
import com.vibeplay.music.ui.theme.VibeSurfaceElevated
import java.util.Locale
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

@Composable
fun EqualizerScreen(
    uiState: EqualizerState,
    onBack: () -> Unit,
    onSetEnabled: (Boolean) -> Unit,
    onBandLevelChange: (Int, Int) -> Unit,
    onBassBoostChange: (Int) -> Unit,
    onSelectPreset: (EqualizerPreset) -> Unit,
    onSaveCustomPreset: (String) -> Unit,
    onDeleteCustomPreset: (Long) -> Unit,
    bottomPadding: PaddingValues
) {
    var showSaveDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(VibeBackground)
            .verticalScroll(rememberScrollState())
            .padding(
                top = 8.dp,
                bottom = bottomPadding.calculateBottomPadding() + 24.dp
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = "Back",
                    tint = VibePrimaryText
                )
            }
            Text(
                text = "Equalizer",
                style = MaterialTheme.typography.headlineMedium,
                color = VibePrimaryText,
                modifier = Modifier.weight(1f)
            )
            Switch(
                checked = uiState.isEnabled,
                onCheckedChange = onSetEnabled,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = VibeBackground,
                    checkedTrackColor = VibeGreen
                )
            )
            Spacer(modifier = Modifier.width(8.dp))
        }

        if (!uiState.isAvailable) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                color = VibeSurfaceElevated,
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(
                    text = "Start playing a track to bind the audio session equalizer, or preview presets below.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = VibeSecondaryText,
                    modifier = Modifier.padding(12.dp)
                )
            }
        }

        // Presets Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Presets",
                style = MaterialTheme.typography.titleMedium,
                color = VibePrimaryText
            )
            TextButton(onClick = { showSaveDialog = true }) {
                Icon(Icons.Rounded.Add, contentDescription = null, tint = VibeGreen, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Save Custom", color = VibeGreen)
            }
        }

        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(uiState.presets, key = { "${it.id}_${it.name}" }) { preset ->
                val selected = uiState.activePresetName == preset.name
                FilterChip(
                    selected = selected,
                    onClick = { onSelectPreset(preset) },
                    label = { Text(preset.name) },
                    trailingIcon = if (!preset.isBuiltIn) {
                        {
                            IconButton(
                                onClick = { onDeleteCustomPreset(preset.id) },
                                modifier = Modifier.size(18.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Close,
                                    contentDescription = "Delete preset",
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    } else {
                        null
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = VibeGreen,
                        selectedLabelColor = VibeBackground,
                        containerColor = VibeSurfaceElevated,
                        labelColor = VibePrimaryText
                    ),
                    border = null
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 5-Band Vertical Sliders Card
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            shape = RoundedCornerShape(16.dp),
            color = VibeSurface
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "5-Band Graphic EQ",
                    style = MaterialTheme.typography.titleMedium,
                    color = VibePrimaryText
                )
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    uiState.bands.forEach { band ->
                        VerticalEqBandSlider(
                            band = band,
                            enabled = uiState.isEnabled,
                            onLevelChanged = { level -> onBandLevelChange(band.index, level) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Bass Boost Rotary Dial Card
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            shape = RoundedCornerShape(16.dp),
            color = VibeSurface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Bass Boost",
                    style = MaterialTheme.typography.titleMedium,
                    color = VibePrimaryText
                )
                Spacer(modifier = Modifier.height(12.dp))
                BassBoostDial(
                    strength = uiState.bassBoostStrength,
                    enabled = uiState.isEnabled,
                    onStrengthChange = onBassBoostChange
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "${(uiState.bassBoostStrength / 10f).roundToInt()}%",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = if (uiState.isEnabled) VibeGreen else VibeSecondaryText
                )
            }
        }
    }

    if (showSaveDialog) {
        var presetName by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showSaveDialog = false },
            containerColor = VibeSurfaceElevated,
            titleContentColor = VibePrimaryText,
            title = { Text("Save Equalizer Preset") },
            text = {
                OutlinedTextField(
                    value = presetName,
                    onValueChange = { presetName = it },
                    singleLine = true,
                    placeholder = { Text("Preset name", color = VibeSecondaryText) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = VibePrimaryText,
                        unfocusedTextColor = VibePrimaryText,
                        focusedBorderColor = VibeGreen,
                        unfocusedBorderColor = VibeSecondaryText,
                        cursorColor = VibeGreen
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onSaveCustomPreset(presetName)
                        showSaveDialog = false
                    },
                    enabled = presetName.trim().isNotEmpty()
                ) {
                    Text("Save", color = VibeGreen)
                }
            },
            dismissButton = {
                TextButton(onClick = { showSaveDialog = false }) {
                    Text("Cancel", color = VibeSecondaryText)
                }
            }
        )
    }
}

@Composable
private fun VerticalEqBandSlider(
    band: EqualizerBand,
    enabled: Boolean,
    onLevelChanged: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val minDb = band.minLevelMb.toFloat()
    val maxDb = band.maxLevelMb.toFloat()
    val range = (maxDb - minDb).coerceAtLeast(1f)
    val fraction = ((band.levelMb - minDb) / range).coerceIn(0f, 1f)
    val dbText = String.format(Locale.US, "%+.1f", band.levelMb / 100f)

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "${dbText}dB",
            style = MaterialTheme.typography.labelMedium,
            color = if (enabled) VibePrimaryText else VibeMutedText
        )
        Spacer(modifier = Modifier.height(8.dp))

        Canvas(
            modifier = Modifier
                .width(44.dp)
                .height(190.dp)
                .pointerInput(band.index, minDb, maxDb) {
                    detectTapGestures { offset ->
                        val frac = 1f - (offset.y / size.height.toFloat()).coerceIn(0f, 1f)
                        val newLevel = (minDb + frac * range).roundToInt()
                        onLevelChanged(newLevel)
                    }
                }
                .pointerInput(band.index, minDb, maxDb) {
                    detectDragGestures { change, _ ->
                        change.consume()
                        val frac = 1f - (change.position.y / size.height.toFloat()).coerceIn(0f, 1f)
                        val newLevel = (minDb + frac * range).roundToInt()
                        onLevelChanged(newLevel)
                    }
                }
        ) {
            val centerX = size.width / 2f
            val padY = 12.dp.toPx()
            val trackTop = padY
            val trackBottom = size.height - padY
            val trackHeight = (trackBottom - trackTop).coerceAtLeast(1f)
            val thumbY = trackBottom - (fraction * trackHeight)
            val zeroY = trackBottom - (((0f - minDb) / range).coerceIn(0f, 1f) * trackHeight)

            // Inactive background track
            drawLine(
                color = VibeSurfaceElevated,
                start = Offset(centerX, trackTop),
                end = Offset(centerX, trackBottom),
                strokeWidth = 6.dp.toPx(),
                cap = StrokeCap.Round
            )

            // Active fill from 0 dB center to thumb
            val activeColor = if (enabled) VibeGreen else VibeMutedText
            drawLine(
                color = activeColor,
                start = Offset(centerX, zeroY),
                end = Offset(centerX, thumbY),
                strokeWidth = 6.dp.toPx(),
                cap = StrokeCap.Round
            )

            // Thumb
            drawCircle(
                color = if (enabled) VibePrimaryText else VibeMutedText,
                radius = 10.dp.toPx(),
                center = Offset(centerX, thumbY)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = band.label,
            style = MaterialTheme.typography.labelMedium,
            color = VibeSecondaryText
        )
    }
}

@Composable
private fun BassBoostDial(
    strength: Int,
    enabled: Boolean,
    onStrengthChange: (Int) -> Unit
) {
    val fraction = (strength / 1000f).coerceIn(0f, 1f)
    val activeColor = if (enabled) VibeGreen else VibeMutedText

    Box(
        modifier = Modifier.size(144.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectDragGestures { change, _ ->
                        change.consume()
                        val cx = size.width / 2f
                        val cy = size.height / 2f
                        val dx = change.position.x - cx
                        val dy = change.position.y - cy
                        var angleDeg = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()
                        // Map 135..405 degrees (270 degree sweep from bottom-left clockwise to bottom-right)
                        angleDeg = (angleDeg - 135f + 360f) % 360f
                        val clampedSweep = angleDeg.coerceIn(0f, 270f)
                        val nextStrength = ((clampedSweep / 270f) * 1000f).roundToInt()
                        onStrengthChange(nextStrength)
                    }
                }
        ) {
            val stroke = 12.dp.toPx()
            val inset = stroke / 2f + 8.dp.toPx()
            val arcSize = Size(size.width - inset * 2f, size.height - inset * 2f)
            val topLeft = Offset(inset, inset)

            // Background arc
            drawArc(
                color = VibeSurfaceElevated,
                startAngle = 135f,
                sweepAngle = 270f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Round)
            )

            // Active arc
            drawArc(
                color = activeColor,
                startAngle = 135f,
                sweepAngle = 270f * fraction,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Round)
            )

            // Inner dial circle + indicator dot
            val center = Offset(size.width / 2f, size.height / 2f)
            val dialRadius = (size.minDimension / 2f) - 26.dp.toPx()
            drawCircle(
                color = Color(0xFF222222),
                radius = dialRadius,
                center = center
            )

            val rad = Math.toRadians((135f + 270f * fraction).toDouble())
            val dotPos = Offset(
                x = center.x + (cos(rad) * (dialRadius * 0.68f)).toFloat(),
                y = center.y + (sin(rad) * (dialRadius * 0.68f)).toFloat()
            )
            drawCircle(
                color = activeColor,
                radius = 5.dp.toPx(),
                center = dotPos
            )
        }
    }
}
