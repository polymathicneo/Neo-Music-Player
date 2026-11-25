package com.vibeplay.music.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Sort
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.vibeplay.music.domain.model.SongSortConfig
import com.vibeplay.music.domain.model.SongSortOption
import com.vibeplay.music.domain.model.SortDirection
import com.vibeplay.music.ui.theme.VibeDivider
import com.vibeplay.music.ui.theme.VibeGreen
import com.vibeplay.music.ui.theme.VibePrimaryText
import com.vibeplay.music.ui.theme.VibeSecondaryText
import com.vibeplay.music.ui.theme.VibeSurface
import com.vibeplay.music.ui.theme.VibeSurfaceElevated

@Composable
fun SortBar(
    sortConfig: SongSortConfig,
    trackCount: Int,
    onOpenSortMenu: () -> Unit,
    onToggleDirection: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = "$trackCount ${if (trackCount == 1) "song" else "songs"}",
            style = MaterialTheme.typography.bodyMedium,
            color = VibeSecondaryText
        )

        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(
                onClick = onOpenSortMenu,
                shape = RoundedCornerShape(50),
                color = VibeSurfaceElevated
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.Sort,
                        contentDescription = "Sort songs",
                        tint = VibeGreen,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = sortConfig.option.label,
                        style = MaterialTheme.typography.labelLarge,
                        color = VibePrimaryText
                    )
                }
            }

            IconButton(
                onClick = onToggleDirection,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = if (sortConfig.direction == SortDirection.ASCENDING) {
                        Icons.Rounded.ArrowUpward
                    } else {
                        Icons.Rounded.ArrowDownward
                    },
                    contentDescription = if (sortConfig.direction == SortDirection.ASCENDING) {
                        "Ascending order"
                    } else {
                        "Descending order"
                    },
                    tint = VibeGreen,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SortBottomSheet(
    currentConfig: SongSortConfig,
    includeDefaultOption: Boolean,
    onSelectOption: (SongSortOption) -> Unit,
    onDismiss: () -> Unit
) {
    val options = if (includeDefaultOption) {
        SongSortOption.entries
    } else {
        SongSortOption.entries.filter { it != SongSortOption.DEFAULT }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = VibeSurface,
        contentColor = VibePrimaryText
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 28.dp)
        ) {
            Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
                Text(
                    text = "Sort by",
                    style = MaterialTheme.typography.titleLarge,
                    color = VibePrimaryText
                )
                Text(
                    text = "Tap the active option again to reverse order",
                    style = MaterialTheme.typography.bodyMedium,
                    color = VibeSecondaryText
                )
            }

            HorizontalDivider(color = VibeDivider)
            Spacer(modifier = Modifier.height(6.dp))

            options.forEach { option ->
                val isSelected = currentConfig.option == option
                val activeDirection = if (isSelected) currentConfig.direction else option.defaultDirection
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            onSelectOption(option)
                            onDismiss()
                        }
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = option.label,
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            ),
                            color = if (isSelected) VibeGreen else VibePrimaryText
                        )
                        Text(
                            text = option.hintFor(activeDirection),
                            style = MaterialTheme.typography.bodyMedium,
                            color = VibeSecondaryText
                        )
                    }
                    if (isSelected) {
                        Icon(
                            imageVector = if (currentConfig.direction == SortDirection.ASCENDING) {
                                Icons.Rounded.ArrowUpward
                            } else {
                                Icons.Rounded.ArrowDownward
                            },
                            contentDescription = "Selected sort order",
                            tint = VibeGreen,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }
        }
    }
}
