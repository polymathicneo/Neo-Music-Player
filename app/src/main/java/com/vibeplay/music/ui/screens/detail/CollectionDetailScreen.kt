package com.vibeplay.music.ui.screens.detail

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.vibeplay.music.domain.model.Song
import com.vibeplay.music.domain.model.SongSortOption
import com.vibeplay.music.ui.components.ArtworkImage
import com.vibeplay.music.ui.components.CreatePlaylistDialog
import com.vibeplay.music.ui.components.SongListItem
import com.vibeplay.music.ui.components.SortBar
import com.vibeplay.music.ui.components.SortBottomSheet
import com.vibeplay.music.ui.navigation.CollectionType
import com.vibeplay.music.ui.theme.VibeBackground
import com.vibeplay.music.ui.theme.VibeGreen
import com.vibeplay.music.ui.theme.VibePrimaryText
import com.vibeplay.music.ui.theme.VibeSecondaryText
import com.vibeplay.music.ui.theme.VibeSurfaceElevated
import com.vibeplay.music.ui.viewmodel.CollectionDetailUiState

@Composable
fun CollectionDetailScreen(
    uiState: CollectionDetailUiState,
    currentSongId: Long?,
    onBack: () -> Unit,
    onSelectSortOption: (SongSortOption) -> Unit,
    onToggleSortDirection: () -> Unit,
    onPlayAll: (List<Song>, Boolean) -> Unit,
    onPlaySong: (Song, List<Song>) -> Unit,
    onPlayNext: (Song) -> Unit,
    onAddToQueue: (Song) -> Unit,
    onAddToPlaylist: (Song) -> Unit,
    onToggleFavorite: (Song) -> Unit,
    onRenamePlaylist: (String) -> Unit,
    onDeletePlaylist: () -> Unit,
    onRemoveFromPlaylist: (Long) -> Unit,
    bottomPadding: PaddingValues
) {
    var menuExpanded by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var showSortSheet by remember { mutableStateOf(false) }
    val firstSong = uiState.songs.firstOrNull()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(VibeBackground),
        contentPadding = PaddingValues(bottom = bottomPadding.calculateBottomPadding() + 16.dp)
    ) {
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(VibeSurfaceElevated, VibeBackground)
                        )
                    )
                    .padding(bottom = 16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Back",
                            tint = VibePrimaryText
                        )
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    if (uiState.type == CollectionType.PLAYLIST) {
                        Box {
                            IconButton(onClick = { menuExpanded = true }) {
                                Icon(
                                    imageVector = Icons.Rounded.MoreVert,
                                    contentDescription = "Playlist options",
                                    tint = VibePrimaryText
                                )
                            }
                            DropdownMenu(
                                expanded = menuExpanded,
                                onDismissRequest = { menuExpanded = false },
                                containerColor = VibeSurfaceElevated
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Rename Playlist", color = VibePrimaryText) },
                                    onClick = {
                                        menuExpanded = false
                                        showRenameDialog = true
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Delete Playlist", color = VibePrimaryText) },
                                    onClick = {
                                        menuExpanded = false
                                        onDeletePlaylist()
                                    }
                                )
                            }
                        }
                    }
                }

                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    if (uiState.type == CollectionType.LIKED_SONGS) {
                        Box(
                            modifier = Modifier
                                .size(176.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    Brush.linearGradient(
                                        listOf(Color(0xFF450AF5), Color(0xFF1DB954))
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Favorite,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(72.dp)
                            )
                        }
                    } else {
                        ArtworkImage(
                            songId = firstSong?.id,
                            contentUri = firstSong?.contentUri,
                            albumArtUri = firstSong?.albumArtUri,
                            contentDescription = uiState.title,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.size(176.dp),
                            iconSizeDp = 56
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = uiState.title,
                            style = MaterialTheme.typography.headlineMedium,
                            color = VibePrimaryText,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = uiState.subtitle,
                            style = MaterialTheme.typography.bodyMedium,
                            color = VibeSecondaryText,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    if (uiState.songs.isNotEmpty()) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = { onPlayAll(uiState.songs, true) }) {
                                Icon(
                                    imageVector = Icons.Rounded.Shuffle,
                                    contentDescription = "Shuffle play",
                                    tint = VibeGreen
                                )
                            }
                            Surface(
                                shape = CircleShape,
                                color = VibeGreen,
                                modifier = Modifier.size(52.dp),
                                onClick = { onPlayAll(uiState.songs, false) }
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Rounded.PlayArrow,
                                        contentDescription = "Play all",
                                        tint = VibeBackground,
                                        modifier = Modifier.size(30.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        if (uiState.songs.isEmpty()) {
            item {
                Text(
                    text = "No songs in this collection yet.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = VibeSecondaryText,
                    modifier = Modifier.padding(24.dp)
                )
            }
        } else {
            item {
                SortBar(
                    sortConfig = uiState.sortConfig,
                    trackCount = uiState.songs.size,
                    onOpenSortMenu = { showSortSheet = true },
                    onToggleDirection = onToggleSortDirection
                )
            }
            items(uiState.songs, key = { "det_${it.id}" }) { song ->
                SongListItem(
                    song = song,
                    isCurrentlyPlaying = song.id == currentSongId,
                    onClick = { onPlaySong(song, uiState.songs) },
                    onPlayNext = { onPlayNext(song) },
                    onAddToQueue = { onAddToQueue(song) },
                    onAddToPlaylist = { onAddToPlaylist(song) },
                    onToggleFavorite = { onToggleFavorite(song) },
                    onRemoveFromPlaylist = if (uiState.type == CollectionType.PLAYLIST) {
                        { onRemoveFromPlaylist(song.id) }
                    } else {
                        null
                    }
                )
            }
        }
    }

    if (showSortSheet) {
        SortBottomSheet(
            currentConfig = uiState.sortConfig,
            includeDefaultOption = uiState.type == CollectionType.PLAYLIST || uiState.type == CollectionType.ALBUM,
            onSelectOption = onSelectSortOption,
            onDismiss = { showSortSheet = false }
        )
    }

    if (showRenameDialog) {
        CreatePlaylistDialog(
            title = "Rename Playlist",
            initialName = uiState.title,
            confirmLabel = "Save",
            onConfirm = { newName ->
                onRenamePlaylist(newName)
                showRenameDialog = false
            },
            onDismiss = { showRenameDialog = false }
        )
    }
}
