package com.vibeplay.music.ui.screens.nowplaying

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.vibeplay.music.domain.model.SleepTimerMode
import com.vibeplay.music.domain.model.SleepTimerState
import com.vibeplay.music.ui.components.formatDuration
import com.vibeplay.music.ui.theme.VibeBackground
import com.vibeplay.music.ui.theme.VibeDivider
import com.vibeplay.music.ui.theme.VibeError
import com.vibeplay.music.ui.theme.VibeGreen
import com.vibeplay.music.ui.theme.VibePrimaryText
import com.vibeplay.music.ui.theme.VibeSecondaryText
import com.vibeplay.music.ui.theme.VibeSurface
import com.vibeplay.music.ui.theme.VibeSurfaceElevated
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SleepTimerSheet(
    sleepTimerState: SleepTimerState,
    onSelectMinutes: (Int) -> Unit,
    onSelectEndOfTrack: () -> Unit,
    onCancelTimer: () -> Unit,
    onDismiss: () -> Unit
) {
    var showCustomPicker by remember { mutableStateOf(false) }
    var customMinutes by remember { mutableFloatStateOf(20f) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = VibeSurface,
        contentColor = VibePrimaryText
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 28.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Sleep Timer",
                        style = MaterialTheme.typography.titleLarge,
                        color = VibePrimaryText
                    )
                    val subtitle = when (sleepTimerState.mode) {
                        SleepTimerMode.DURATION -> "Stopping in ${formatDuration(sleepTimerState.remainingMs)} (5s fade-out)"
                        SleepTimerMode.END_OF_TRACK -> "Stopping at end of current track"
                        SleepTimerMode.OFF -> "Smooth 5-second fade-out before pausing"
                    }
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (sleepTimerState.isActive) VibeGreen else VibeSecondaryText
                    )
                }
            }

            HorizontalDivider(color = VibeDivider)
            Spacer(modifier = Modifier.height(6.dp))

            val presets = listOf(15, 30, 45, 60)
            presets.forEach { minutes ->
                TimerOptionRow(
                    label = "$minutes minutes",
                    onClick = {
                        onSelectMinutes(minutes)
                        onDismiss()
                    }
                )
            }

            TimerOptionRow(
                label = "End of current track",
                onClick = {
                    onSelectEndOfTrack()
                    onDismiss()
                }
            )

            TimerOptionRow(
                label = "Custom duration...",
                onClick = { showCustomPicker = !showCustomPicker }
            )

            if (showCustomPicker) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = "${customMinutes.roundToInt()} minutes",
                        style = MaterialTheme.typography.titleMedium,
                        color = VibeGreen
                    )
                    Slider(
                        value = customMinutes,
                        onValueChange = { customMinutes = it },
                        valueRange = 1f..180f,
                        colors = SliderDefaults.colors(
                            thumbColor = VibeGreen,
                            activeTrackColor = VibeGreen,
                            inactiveTrackColor = VibeSurfaceElevated
                        )
                    )
                    Button(
                        onClick = {
                            onSelectMinutes(customMinutes.roundToInt())
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = VibeGreen,
                            contentColor = VibeBackground
                        ),
                        shape = RoundedCornerShape(50),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Start ${customMinutes.roundToInt()}m Timer", fontWeight = FontWeight.Bold)
                    }
                }
            }

            if (sleepTimerState.isActive) {
                Spacer(modifier = Modifier.height(8.dp))
                TextButton(
                    onClick = {
                        onCancelTimer()
                        onDismiss()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) {
                    Text("Turn off timer", color = VibeError, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun TimerOptionRow(
    label: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = VibePrimaryText
        )
    }
}
