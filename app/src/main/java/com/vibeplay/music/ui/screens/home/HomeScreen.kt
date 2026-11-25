package com.vibeplay.music.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Equalizer
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.vibeplay.music.domain.model.HomeQuickItem
import com.vibeplay.music.domain.model.QuickItemType
import com.vibeplay.music.domain.model.Song
import com.vibeplay.music.ui.components.ArtworkImage
import com.vibeplay.music.ui.components.SongListItem
import com.vibeplay.music.ui.navigation.CollectionType
import com.vibeplay.music.ui.theme.VibeBackground
import com.vibeplay.music.ui.theme.VibeGreen
import com.vibeplay.music.ui.theme.VibePrimaryText
import com.vibeplay.music.ui.theme.VibeSecondaryText
import com.vibeplay.music.ui.theme.VibeSurfaceElevated
import com.vibeplay.music.ui.viewmodel.HomeUiState

@Composable
fun HomeScreen(
    uiState: HomeUiState,
    currentSongId: Long?,
    onOpenSettings: () -> Unit,
    onOpenEqualizer: () -> Unit,
    onOpenCollection: (CollectionType, String, String) -> Unit,
    onPlaySong: (Song, List<Song>) -> Unit,
    onPlayNext: (Song) -> Unit,
    onAddToQueue: (Song) -> Unit,
    onAddToPlaylist: (Song) -> Unit,
    onToggleFavorite: (Song) -> Unit,
    onRescan: () -> Unit,
    bottomPadding: PaddingValues
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(VibeBackground),
        contentPadding = PaddingValues(
            top = 12.dp,
            bottom = bottomPadding.calculateBottomPadding() + 16.dp
        )
    ) {
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = uiState.greeting,
                    style = MaterialTheme.typography.headlineMedium,
                    color = VibePrimaryText,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onOpenEqualizer) {
                    Icon(
                        imageVector = Icons.Rounded.Equalizer,
                        contentDescription = "Equalizer",
                        tint = VibePrimaryText
                    )
                }
                IconButton(onClick = onOpenSettings) {
                    Icon(
                        imageVector = Icons.Rounded.Settings,
                        contentDescription = "Settings",
                        tint = VibePrimaryText
                    )
                }
            }
        }

        if (uiState.allSongsCount == 0) {
            item {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    shape = RoundedCornerShape(14.dp),
                    color = VibeSurfaceElevated
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "No music found yet",
                            style = MaterialTheme.typography.titleLarge,
                            color = VibePrimaryText
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Add MP3, FLAC, WAV, AAC, M4A, or OGG files to your device and tap Scan Library.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = VibeSecondaryText
                        )
                        Spacer(modifier = Modifier.height(16.dp))
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
                                text = if (uiState.isScanning) "Scanning..." else "Scan Library",
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        if (uiState.quickItems.isNotEmpty()) {
            item {
                val rows = uiState.quickItems.chunked(2)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    rows.forEach { pair ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            pair.forEach { item ->
                                QuickAccessCard(
                                    item = item,
                                    onClick = {
                                        val collectionType = when (item.type) {
                                            QuickItemType.LIKED_SONGS -> CollectionType.LIKED_SONGS
                                            QuickItemType.ALBUM -> CollectionType.ALBUM
                                            QuickItemType.PLAYLIST -> CollectionType.PLAYLIST
                                        }
                                        onOpenCollection(collectionType, item.targetId, item.title)
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            if (pair.size == 1) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }

        if (uiState.recentlyAdded.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Recently Added",
                    style = MaterialTheme.typography.titleLarge,
                    color = VibePrimaryText,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(uiState.recentlyAdded, key = { "recent_${it.id}" }) { song ->
                        Column(
                            modifier = Modifier
                                .width(136.dp)
                                .clickable { onPlaySong(song, uiState.recentlyAdded) }
                        ) {
                            ArtworkImage(
                                songId = song.id,
                                contentUri = song.contentUri,
                                albumArtUri = song.albumArtUri,
                                contentDescription = song.album,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.size(136.dp),
                                iconSizeDp = 36
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = song.title,
                                style = MaterialTheme.typography.labelLarge,
                                color = if (song.id == currentSongId) VibeGreen else VibePrimaryText,
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
                    }
                }
            }
        }

        if (uiState.mostPlayed.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(20.dp))
                Text(
                    text = if (uiState.mostPlayed.any { it.playCount > 0 }) "Most Played" else "Your Tracks",
                    style = MaterialTheme.typography.titleLarge,
                    color = VibePrimaryText,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                )
            }
            items(uiState.mostPlayed, key = { "most_${it.id}" }) { song ->
                SongListItem(
                    song = song,
                    isCurrentlyPlaying = song.id == currentSongId,
                    onClick = { onPlaySong(song, uiState.mostPlayed) },
                    onPlayNext = { onPlayNext(song) },
                    onAddToQueue = { onAddToQueue(song) },
                    onAddToPlaylist = { onAddToPlaylist(song) },
                    onToggleFavorite = { onToggleFavorite(song) }
                )
            }
        }
    }
}

@Composable
private fun QuickAccessCard(
    item: HomeQuickItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .height(56.dp)
            .clip(RoundedCornerShape(6.dp))
            .clickable(onClick = onClick),
        color = VibeSurfaceElevated,
        shape = RoundedCornerShape(6.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (item.type == QuickItemType.LIKED_SONGS) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
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
                        modifier = Modifier.size(22.dp)
                    )
                }
            } else {
                ArtworkImage(
                    songId = item.songId,
                    contentUri = item.contentUri,
                    albumArtUri = item.artworkUri,
                    contentDescription = item.title,
                    shape = RoundedCornerShape(topStart = 6.dp, bottomStart = 6.dp),
                    modifier = Modifier.size(56.dp),
                    iconSizeDp = 22
                )
            }

            Text(
                text = item.title,
                style = MaterialTheme.typography.labelLarge,
                color = VibePrimaryText,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 10.dp)
            )
        }
    }
}
