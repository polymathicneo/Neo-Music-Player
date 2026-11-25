package com.vibeplay.music.ui.screens.queue

import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DragHandle
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.vibeplay.music.domain.model.PlaybackState
import com.vibeplay.music.ui.components.ArtworkImage
import com.vibeplay.music.ui.theme.VibeDivider
import com.vibeplay.music.ui.theme.VibeGreen
import com.vibeplay.music.ui.theme.VibePrimaryText
import com.vibeplay.music.ui.theme.VibeSecondaryText
import com.vibeplay.music.ui.theme.VibeSurface
import com.vibeplay.music.ui.theme.VibeSurfaceElevated

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QueueSheet(
    playbackState: PlaybackState,
    onPlayIndex: (Int) -> Unit,
    onMoveItem: (Int, Int) -> Unit,
    onRemoveItem: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val rowHeightPx = with(LocalDensity.current) { 64.dp.toPx() }
    var draggingIndex by remember { mutableIntStateOf(-1) }
    var dragOffsetY by remember { mutableFloatStateOf(0f) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = VibeSurface,
        contentColor = VibePrimaryText
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp)
        ) {
            Text(
                text = "Play Queue",
                style = MaterialTheme.typography.titleLarge,
                color = VibePrimaryText,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
            )
            HorizontalDivider(color = VibeDivider)

            val currentSong = playbackState.currentSong
            if (currentSong != null) {
                Text(
                    text = "Now Playing",
                    style = MaterialTheme.typography.labelLarge,
                    color = VibeSecondaryText,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp)
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ArtworkImage(
                        songId = currentSong.id,
                        contentUri = currentSong.contentUri,
                        albumArtUri = currentSong.albumArtUri,
                        contentDescription = currentSong.album,
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = currentSong.title,
                            style = MaterialTheme.typography.titleMedium,
                            color = VibeGreen,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = currentSong.artist,
                            style = MaterialTheme.typography.bodyMedium,
                            color = VibeSecondaryText,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Queue (${playbackState.queue.size} tracks) • Drag handle to reorder",
                style = MaterialTheme.typography.labelLarge,
                color = VibeSecondaryText,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
            )

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(420.dp)
            ) {
                itemsIndexed(
                    items = playbackState.queue,
                    key = { index, song -> "${song.id}_$index" }
                ) { index, song ->
                    val isDragging = draggingIndex == index
                    val isCurrent = index == playbackState.currentIndex

                    Surface(
                        color = if (isDragging) VibeSurfaceElevated else VibeSurface,
                        shadowElevation = if (isDragging) 8.dp else 0.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .zIndex(if (isDragging) 1f else 0f)
                            .graphicsLayer {
                                translationY = if (isDragging) dragOffsetY else 0f
                            }
                            .clickable { onPlayIndex(index) }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(64.dp)
                                .padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.DragHandle,
                                contentDescription = "Reorder",
                                tint = VibeSecondaryText,
                                modifier = Modifier
                                    .size(32.dp)
                                    .padding(4.dp)
                                    .pointerInput(playbackState.queue.size) {
                                        detectDragGestures(
                                            onDragStart = {
                                                draggingIndex = index
                                                dragOffsetY = 0f
                                            },
                                            onDrag = { change, dragAmount ->
                                                change.consume()
                                                dragOffsetY += dragAmount.y
                                                if (dragOffsetY > rowHeightPx * 0.75f &&
                                                    draggingIndex in 0 until playbackState.queue.lastIndex
                                                ) {
                                                    val next = draggingIndex + 1
                                                    onMoveItem(draggingIndex, next)
                                                    draggingIndex = next
                                                    dragOffsetY -= rowHeightPx
                                                } else if (dragOffsetY < -rowHeightPx * 0.75f &&
                                                    draggingIndex > 0
                                                ) {
                                                    val prev = draggingIndex - 1
                                                    onMoveItem(draggingIndex, prev)
                                                    draggingIndex = prev
                                                    dragOffsetY += rowHeightPx
                                                }
                                            },
                                            onDragEnd = {
                                                draggingIndex = -1
                                                dragOffsetY = 0f
                                            },
                                            onDragCancel = {
                                                draggingIndex = -1
                                                dragOffsetY = 0f
                                            }
                                        )
                                    }
                            )

                            Spacer(modifier = Modifier.width(8.dp))

                            ArtworkImage(
                                songId = song.id,
                                contentUri = song.contentUri,
                                albumArtUri = song.albumArtUri,
                                contentDescription = song.album,
                                shape = RoundedCornerShape(4.dp),
                                modifier = Modifier.size(42.dp)
                            )

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = song.title,
                                    style = MaterialTheme.typography.bodyLarge.copy(
                                        fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium
                                    ),
                                    color = if (isCurrent) VibeGreen else VibePrimaryText,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = song.artist,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = VibeSecondaryText,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            if (!isCurrent) {
                                IconButton(onClick = { onRemoveItem(index) }) {
                                    Icon(
                                        imageVector = Icons.Rounded.Close,
                                        contentDescription = "Remove from queue",
                                        tint = VibeSecondaryText,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
