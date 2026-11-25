package com.vibeplay.music.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Equalizer
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.vibeplay.music.ui.theme.VibeBackground
import com.vibeplay.music.ui.theme.VibeGreen
import com.vibeplay.music.ui.theme.VibePrimaryText
import com.vibeplay.music.ui.theme.VibeSecondaryText
import com.vibeplay.music.ui.theme.VibeSurface
import com.vibeplay.music.ui.theme.VibeSurfaceElevated
import com.vibeplay.music.ui.viewmodel.SettingsUiState
import kotlin.math.roundToInt

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    uiState: SettingsUiState,
    onBack: () -> Unit,
    onRescan: () -> Unit,
    onSetMinDurationSeconds: (Int) -> Unit,
    onToggleExcludedFolder: (String) -> Unit,
    onToggleIncludedFolder: (String) -> Unit,
    onClearIncludedFolders: () -> Unit,
    onSetCrossfadeSeconds: (Int) -> Unit,
    onOpenEqualizer: () -> Unit,
    bottomPadding: PaddingValues
) {
    var showFolderDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(VibeBackground),
        contentPadding = PaddingValues(
            top = 8.dp,
            bottom = bottomPadding.calculateBottomPadding() + 24.dp
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
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
                    text = "Settings",
                    style = MaterialTheme.typography.headlineMedium,
                    color = VibePrimaryText
                )
            }
        }

        // Library Scan Card
        item {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(12.dp),
                color = VibeSurface
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Music Library",
                        style = MaterialTheme.typography.titleMedium,
                        color = VibePrimaryText
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${uiState.totalSongsCount} tracks indexed locally",
                        style = MaterialTheme.typography.bodyMedium,
                        color = VibeSecondaryText
                    )
                    if (!uiState.lastScanMessage.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = uiState.lastScanMessage,
                            style = MaterialTheme.typography.bodyMedium,
                            color = VibeGreen
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = onRescan,
                        enabled = !uiState.isScanning,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = VibeGreen,
                            contentColor = VibeBackground
                        ),
                        shape = RoundedCornerShape(50)
                    ) {
                        if (uiState.isScanning) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = VibeBackground
                            )
                        } else {
                            Icon(Icons.Rounded.Refresh, contentDescription = null)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (uiState.isScanning) "Scanning Storage..." else "Rescan Library",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Crossfade & Gapless Card
        item {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(12.dp),
                color = VibeSurface
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Crossfade",
                            style = MaterialTheme.typography.titleMedium,
                            color = VibePrimaryText
                        )
                        Text(
                            text = if (uiState.settings.crossfadeDurationSec == 0) {
                                "Off (Gapless)"
                            } else {
                                "${uiState.settings.crossfadeDurationSec}s"
                            },
                            style = MaterialTheme.typography.labelLarge,
                            color = VibeGreen
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Gapless playback is enabled automatically when crossfade is 0s.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = VibeSecondaryText
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Slider(
                        value = uiState.settings.crossfadeDurationSec.toFloat(),
                        onValueChange = { onSetCrossfadeSeconds(it.roundToInt()) },
                        valueRange = 0f..12f,
                        steps = 11,
                        colors = SliderDefaults.colors(
                            thumbColor = VibeGreen,
                            activeTrackColor = VibeGreen,
                            inactiveTrackColor = VibeSurfaceElevated
                        )
                    )
                }
            }
        }

        // Equalizer Link Card
        item {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .clickable(onClick = onOpenEqualizer),
                shape = RoundedCornerShape(12.dp),
                color = VibeSurface
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Equalizer,
                        contentDescription = null,
                        tint = VibeGreen
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "5-Band Equalizer & Bass Boost",
                            style = MaterialTheme.typography.titleMedium,
                            color = VibePrimaryText
                        )
                        Text(
                            text = "Customize audio frequencies and presets",
                            style = MaterialTheme.typography.bodyMedium,
                            color = VibeSecondaryText
                        )
                    }
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                        contentDescription = null,
                        tint = VibeSecondaryText
                    )
                }
            }
        }

        // Scanner Filtering Card
        item {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(12.dp),
                color = VibeSurface
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Minimum Track Duration",
                        style = MaterialTheme.typography.titleMedium,
                        color = VibePrimaryText
                    )
                    Text(
                        text = "Filter out short voice notes and ringtones.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = VibeSecondaryText
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    val currentSec = (uiState.settings.minDurationMs / 1000L).toInt()
                    val options = listOf(0, 15, 30, 60, 120)
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        options.forEach { sec ->
                            val selected = currentSec == sec
                            FilterChip(
                                selected = selected,
                                onClick = { onSetMinDurationSeconds(sec) },
                                label = { Text(if (sec == 0) "All" else "${sec}s+") },
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

                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showFolderDialog = true }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Rounded.Folder, contentDescription = null, tint = VibeGreen)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Include / Exclude Folders",
                                style = MaterialTheme.typography.titleMedium,
                                color = VibePrimaryText
                            )
                            val excludedCount = uiState.settings.excludedFolders.size
                            val includedCount = uiState.settings.includedFolders.size
                            Text(
                                text = when {
                                    includedCount > 0 -> "$includedCount folder(s) whitelisted"
                                    excludedCount > 0 -> "$excludedCount folder(s) excluded"
                                    else -> "Scanning all detected music folders"
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = VibeSecondaryText
                            )
                        }
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                            contentDescription = null,
                            tint = VibeSecondaryText
                        )
                    }
                }
            }
        }

        // Offline Privacy Badge
        item {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(12.dp),
                color = VibeSurface
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Rounded.Lock, contentDescription = null, tint = VibeGreen)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "100% Offline & Private",
                            style = MaterialTheme.typography.titleMedium,
                            color = VibePrimaryText
                        )
                        Text(
                            text = "No INTERNET permission, zero ads, zero analytics, zero tracking.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = VibeSecondaryText
                        )
                    }
                }
            }
        }
    }

    if (showFolderDialog) {
        FolderFilterDialog(
            uiState = uiState,
            onToggleExcluded = onToggleExcludedFolder,
            onToggleIncluded = onToggleIncludedFolder,
            onClearIncluded = onClearIncludedFolders,
            onDismiss = { showFolderDialog = false }
        )
    }
}

@Composable
private fun FolderFilterDialog(
    uiState: SettingsUiState,
    onToggleExcluded: (String) -> Unit,
    onToggleIncluded: (String) -> Unit,
    onClearIncluded: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = VibeSurfaceElevated,
        titleContentColor = VibePrimaryText,
        title = { Text("Manage Music Folders") },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Toggle folders off to exclude them, or tap \"Only\" to restrict scanning to selected folders.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = VibeSecondaryText
                )
                if (uiState.settings.includedFolders.isNotEmpty()) {
                    TextButton(onClick = onClearIncluded) {
                        Text("Reset whitelist (scan all non-excluded)", color = VibeGreen)
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                if (uiState.discoveredFolders.isEmpty()) {
                    Text(
                        text = "Tap Rescan Library first to discover device folders.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = VibeSecondaryText
                    )
                } else {
                    LazyColumn(modifier = Modifier.height(280.dp)) {
                        items(uiState.discoveredFolders, key = { it.path }) { folder ->
                            val isExcluded = folder.path in uiState.settings.excludedFolders
                            val isIncluded = folder.path in uiState.settings.includedFolders
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "${folder.name} (${folder.totalFiles})",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = VibePrimaryText,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = folder.path,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = VibeSecondaryText,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                TextButton(onClick = { onToggleIncluded(folder.path) }) {
                                    Text(
                                        text = if (isIncluded) "Only ✓" else "Only",
                                        color = if (isIncluded) VibeGreen else VibeSecondaryText
                                    )
                                }
                                Switch(
                                    checked = !isExcluded,
                                    onCheckedChange = { onToggleExcluded(folder.path) },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = VibeBackground,
                                        checkedTrackColor = VibeGreen
                                    )
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Done", color = VibeGreen)
            }
        }
    )
}
