package com.vibeplay.music.ui.screens.library

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.text.font.FontWeight
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
import com.vibeplay.music.ui.viewmodel.LibraryTab
import com.vibeplay.music.ui.viewmodel.LibraryUiState

@Composable
fun LibraryScreen(
    uiState: LibraryUiState,
    currentSongId: Long?,
    onSelectTab: (LibraryTab) -> Unit,
    onSelectSortOption: (SongSortOption) -> Unit,
    onToggleSortDirection: () -> Unit,
    onCreatePlaylist: (String) -> Unit,
    onDeletePlaylist: (Long) -> Unit,
    onOpenCollection: (CollectionType, String, String) -> Unit,
    onPlaySong: (Song, List<Song>) -> Unit,
    onPlayQueue: (List<Song>, Boolean) -> Unit,
    onPlayNext: (Song) -> Unit,
    onAddToQueue: (Song) -> Unit,
    onAddToPlaylist: (Song) -> Unit,
    onToggleFavorite: (Song) -> Unit,
    bottomPadding: PaddingValues
) {
    var showCreateDialog by remember { mutableStateOf(false) }
    var showSortSheet by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(VibeBackground)
            .padding(top = 12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Your Library",
                style = MaterialTheme.typography.headlineMedium,
                color = VibePrimaryText,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = { showCreateDialog = true }) {
                Icon(
                    imageVector = Icons.Rounded.Add,
                    contentDescription = "Create playlist",
                    tint = VibePrimaryText,
                    modifier = Modifier.size(28.dp)
                )
            }
        }

        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(LibraryTab.entries) { tab ->
                val selected = uiState.selectedTab == tab
                FilterChip(
                    selected = selected,
                    onClick = { onSelectTab(tab) },
                    label = { Text(tab.label) },
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

        val listBottomPadding = PaddingValues(bottom = bottomPadding.calculateBottomPadding() + 16.dp)

        when (uiState.selectedTab) {
            LibraryTab.SONGS -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = listBottomPadding
                ) {
                    if (uiState.songs.isNotEmpty()) {
                        item {
                            SortBar(
                                sortConfig = uiState.sortConfig,
                                trackCount = uiState.songs.size,
                                onOpenSortMenu = { showSortSheet = true },
                                onToggleDirection = onToggleSortDirection
                            )
                        }
                        item {
                            PlayShuffleRow(
                                onPlay = { onPlayQueue(uiState.songs, false) },
                                onShuffle = { onPlayQueue(uiState.songs, true) }
                            )
                        }
                    } else {
                        item {
                            Text(
                                text = "No local songs found. Open Settings to rescan your storage.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = VibeSecondaryText,
                                modifier = Modifier.padding(24.dp)
                            )
                        }
                    }

                    items(uiState.songs, key = { "all_${it.id}" }) { song ->
                        SongListItem(
                            song = song,
                            isCurrentlyPlaying = song.id == currentSongId,
                            onClick = { onPlaySong(song, uiState.songs) },
                            onPlayNext = { onPlayNext(song) },
                            onAddToQueue = { onAddToQueue(song) },
                            onAddToPlaylist = { onAddToPlaylist(song) },
                            onToggleFavorite = { onToggleFavorite(song) }
                        )
                    }
                }
            }

            LibraryTab.PLAYLISTS -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = listBottomPadding
                ) {
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onOpenCollection(CollectionType.LIKED_SONGS, "liked", "Liked Songs")
                                }
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(RoundedCornerShape(6.dp))
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
                                    tint = Color.White
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Liked Songs",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = VibePrimaryText
                                )
                                Text(
                                    text = "Playlist • ${uiState.likedSongs.size} songs",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = VibeSecondaryText
                                )
                            }
                        }
                    }

                    items(uiState.playlists, key = { "pl_${it.id}" }) { playlist ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onOpenCollection(
                                        CollectionType.PLAYLIST,
                                        playlist.id.toString(),
                                        playlist.name
                                    )
                                }
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            ArtworkImage(
                                songId = playlist.coverSongId,
                                contentUri = playlist.coverContentUri,
                                albumArtUri = playlist.coverArtUri,
                                contentDescription = playlist.name,
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier.size(56.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = playlist.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = VibePrimaryText,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "Playlist • ${playlist.songCount} songs",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = VibeSecondaryText
                                )
                            }
                            IconButton(onClick = { onDeletePlaylist(playlist.id) }) {
                                Icon(
                                    imageVector = Icons.Rounded.DeleteOutline,
                                    contentDescription = "Delete playlist",
                                    tint = VibeSecondaryText
                                )
                            }
                        }
                    }
                }
            }

            LibraryTab.ARTISTS -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = listBottomPadding
                ) {
                    items(uiState.artists, key = { "lib_art_${it.name}" }) { artist ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onOpenCollection(CollectionType.ARTIST, artist.name, artist.name)
                                }
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            ArtworkImage(
                                songId = artist.firstSongId,
                                contentUri = artist.firstSongContentUri,
                                albumArtUri = artist.artworkUri,
                                contentDescription = artist.name,
                                shape = CircleShape,
                                modifier = Modifier.size(56.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = artist.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = VibePrimaryText,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${artist.songCount} songs • ${artist.albumCount} albums",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = VibeSecondaryText
                                )
                            }
                        }
                    }
                }
            }

            LibraryTab.ALBUMS -> {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = 16.dp,
                        end = 16.dp,
                        top = 8.dp,
                        bottom = bottomPadding.calculateBottomPadding() + 16.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(uiState.albums, key = { "lib_alb_${it.id}_${it.name}" }) { album ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onOpenCollection(CollectionType.ALBUM, album.id.toString(), album.name)
                                }
                        ) {
                            ArtworkImage(
                                songId = album.firstSongId,
                                contentUri = album.firstSongContentUri,
                                albumArtUri = album.artworkUri,
                                contentDescription = album.name,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(1f),
                                iconSizeDp = 40
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = album.name,
                                style = MaterialTheme.typography.labelLarge,
                                color = VibePrimaryText,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = album.artist,
                                style = MaterialTheme.typography.bodyMedium,
                                color = VibeSecondaryText,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            LibraryTab.FOLDERS -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = listBottomPadding
                ) {
                    items(uiState.folders, key = { "lib_fld_${it.path}" }) { folder ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onOpenCollection(CollectionType.FOLDER, folder.path, folder.name)
                                }
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = VibeSurfaceElevated,
                                modifier = Modifier.size(52.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Folder,
                                    contentDescription = null,
                                    tint = VibeGreen,
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = folder.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = VibePrimaryText
                                )
                                Text(
                                    text = "${folder.songCount} songs • ${folder.path}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = VibeSecondaryText,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }

            LibraryTab.LIKED_SONGS -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = listBottomPadding
                ) {
                    if (uiState.likedSongs.isNotEmpty()) {
                        item {
                            SortBar(
                                sortConfig = uiState.sortConfig,
                                trackCount = uiState.likedSongs.size,
                                onOpenSortMenu = { showSortSheet = true },
                                onToggleDirection = onToggleSortDirection
                            )
                        }
                        item {
                            PlayShuffleRow(
                                onPlay = { onPlayQueue(uiState.likedSongs, false) },
                                onShuffle = { onPlayQueue(uiState.likedSongs, true) }
                            )
                        }
                    } else {
                        item {
                            Text(
                                text = "Tap the heart icon on any track to save it to Liked Songs.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = VibeSecondaryText,
                                modifier = Modifier.padding(24.dp)
                            )
                        }
                    }

                    items(uiState.likedSongs, key = { "liked_${it.id}" }) { song ->
                        SongListItem(
                            song = song,
                            isCurrentlyPlaying = song.id == currentSongId,
                            onClick = { onPlaySong(song, uiState.likedSongs) },
                            onPlayNext = { onPlayNext(song) },
                            onAddToQueue = { onAddToQueue(song) },
                            onAddToPlaylist = { onAddToPlaylist(song) },
                            onToggleFavorite = { onToggleFavorite(song) }
                        )
                    }
                }
            }
        }
    }

    if (showSortSheet) {
        SortBottomSheet(
            currentConfig = uiState.sortConfig,
            includeDefaultOption = false,
            onSelectOption = onSelectSortOption,
            onDismiss = { showSortSheet = false }
        )
    }

    if (showCreateDialog) {
        CreatePlaylistDialog(
            onConfirm = { name ->
                onCreatePlaylist(name)
                showCreateDialog = false
            },
            onDismiss = { showCreateDialog = false }
        )
    }
}

@Composable
private fun PlayShuffleRow(
    onPlay: () -> Unit,
    onShuffle: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Button(
            onClick = onPlay,
            colors = ButtonDefaults.buttonColors(
                containerColor = VibeGreen,
                contentColor = VibeBackground
            ),
            shape = RoundedCornerShape(50),
            modifier = Modifier.weight(1f)
        ) {
            Icon(Icons.Rounded.PlayArrow, contentDescription = null)
            Spacer(modifier = Modifier.width(4.dp))
            Text("Play", fontWeight = FontWeight.Bold)
        }
        OutlinedButton(
            onClick = onShuffle,
            shape = RoundedCornerShape(50),
            modifier = Modifier.weight(1f)
        ) {
            Icon(Icons.Rounded.Shuffle, contentDescription = null, tint = VibePrimaryText)
            Spacer(modifier = Modifier.width(4.dp))
            Text("Shuffle", color = VibePrimaryText)
        }
    }
}
