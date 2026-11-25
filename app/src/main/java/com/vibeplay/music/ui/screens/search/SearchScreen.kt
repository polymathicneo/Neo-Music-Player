package com.vibeplay.music.ui.screens.search

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.vibeplay.music.domain.model.Song
import com.vibeplay.music.ui.components.ArtworkImage
import com.vibeplay.music.ui.components.SongListItem
import com.vibeplay.music.ui.navigation.CollectionType
import com.vibeplay.music.ui.theme.VibeBackground
import com.vibeplay.music.ui.theme.VibeGreen
import com.vibeplay.music.ui.theme.VibePrimaryText
import com.vibeplay.music.ui.theme.VibeSecondaryText
import com.vibeplay.music.ui.theme.VibeSurfaceElevated
import com.vibeplay.music.ui.viewmodel.SearchFilter
import com.vibeplay.music.ui.viewmodel.SearchUiState

@Composable
fun SearchScreen(
    uiState: SearchUiState,
    currentSongId: Long?,
    onQueryChange: (String) -> Unit,
    onCommitQuery: (String) -> Unit,
    onSelectFilter: (SearchFilter) -> Unit,
    onDeleteHistoryItem: (String) -> Unit,
    onClearHistory: () -> Unit,
    onPlaySong: (Song, List<Song>) -> Unit,
    onPlayNext: (Song) -> Unit,
    onAddToQueue: (Song) -> Unit,
    onAddToPlaylist: (Song) -> Unit,
    onToggleFavorite: (Song) -> Unit,
    onOpenCollection: (CollectionType, String, String) -> Unit,
    bottomPadding: PaddingValues
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(VibeBackground)
            .padding(top = 12.dp)
    ) {
        Text(
            text = "Search",
            style = MaterialTheme.typography.headlineMedium,
            color = VibePrimaryText,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )

        TextField(
            value = uiState.query,
            onValueChange = onQueryChange,
            placeholder = {
                Text(
                    text = "Songs, artists, albums, or folders",
                    color = VibeSecondaryText
                )
            },
            leadingIcon = {
                Icon(Icons.Rounded.Search, contentDescription = null, tint = VibePrimaryText)
            },
            trailingIcon = {
                if (uiState.query.isNotEmpty()) {
                    IconButton(onClick = { onQueryChange("") }) {
                        Icon(Icons.Rounded.Close, contentDescription = "Clear", tint = VibeSecondaryText)
                    }
                }
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { onCommitQuery(uiState.query) }),
            shape = RoundedCornerShape(10.dp),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = VibeSurfaceElevated,
                unfocusedContainerColor = VibeSurfaceElevated,
                focusedTextColor = VibePrimaryText,
                unfocusedTextColor = VibePrimaryText,
                cursorColor = VibeGreen,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        )

        if (uiState.query.isBlank()) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    top = 16.dp,
                    bottom = bottomPadding.calculateBottomPadding() + 16.dp
                )
            ) {
                if (uiState.searchHistory.isNotEmpty()) {
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Recent searches",
                                style = MaterialTheme.typography.titleMedium,
                                color = VibePrimaryText,
                                modifier = Modifier.weight(1f)
                            )
                            TextButton(onClick = onClearHistory) {
                                Text("Clear all", color = VibeSecondaryText)
                            }
                        }
                    }

                    items(uiState.searchHistory, key = { "hist_$it" }) { item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onQueryChange(item) }
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.History,
                                contentDescription = null,
                                tint = VibeSecondaryText
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = item,
                                style = MaterialTheme.typography.bodyLarge,
                                color = VibePrimaryText,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(onClick = { onDeleteHistoryItem(item) }) {
                                Icon(
                                    imageVector = Icons.Rounded.Close,
                                    contentDescription = "Remove",
                                    tint = VibeSecondaryText,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                } else {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Find your local music",
                                style = MaterialTheme.typography.titleMedium,
                                color = VibePrimaryText
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Instant offline search across songs, artists, albums, and storage folders.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = VibeSecondaryText
                            )
                        }
                    }
                }
            }
        } else {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(SearchFilter.entries) { filter ->
                    val selected = uiState.selectedFilter == filter
                    FilterChip(
                        selected = selected,
                        onClick = { onSelectFilter(filter) },
                        label = { Text(filter.label) },
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

            val showArtists = uiState.selectedFilter in listOf(SearchFilter.ALL, SearchFilter.ARTISTS)
            val showAlbums = uiState.selectedFilter in listOf(SearchFilter.ALL, SearchFilter.ALBUMS)
            val showFolders = uiState.selectedFilter in listOf(SearchFilter.ALL, SearchFilter.FOLDERS)
            val showSongs = uiState.selectedFilter in listOf(SearchFilter.ALL, SearchFilter.SONGS)

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = bottomPadding.calculateBottomPadding() + 16.dp)
            ) {
                if (showArtists && uiState.artists.isNotEmpty()) {
                    item {
                        SectionTitle("Artists")
                    }
                    items(uiState.artists, key = { "art_${it.name}" }) { artist ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onCommitQuery(uiState.query)
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
                                modifier = Modifier.size(48.dp)
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
                                    text = "Artist • ${artist.songCount} songs",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = VibeSecondaryText
                                )
                            }
                        }
                    }
                }

                if (showAlbums && uiState.albums.isNotEmpty()) {
                    item {
                        SectionTitle("Albums")
                    }
                    items(uiState.albums, key = { "alb_${it.id}_${it.name}" }) { album ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onCommitQuery(uiState.query)
                                    onOpenCollection(CollectionType.ALBUM, album.id.toString(), album.name)
                                }
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            ArtworkImage(
                                songId = album.firstSongId,
                                contentUri = album.firstSongContentUri,
                                albumArtUri = album.artworkUri,
                                contentDescription = album.name,
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = album.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = VibePrimaryText,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "Album • ${album.artist}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = VibeSecondaryText,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }

                if (showFolders && uiState.folders.isNotEmpty()) {
                    item {
                        SectionTitle("Folders")
                    }
                    items(uiState.folders, key = { "fld_${it.path}" }) { folder ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onCommitQuery(uiState.query)
                                    onOpenCollection(CollectionType.FOLDER, folder.path, folder.name)
                                }
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = VibeSurfaceElevated,
                                modifier = Modifier.size(48.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Folder,
                                    contentDescription = null,
                                    tint = VibeGreen,
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
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

                if (showSongs && uiState.songs.isNotEmpty()) {
                    item {
                        SectionTitle("Songs")
                    }
                    items(uiState.songs, key = { "sng_${it.id}" }) { song ->
                        SongListItem(
                            song = song,
                            isCurrentlyPlaying = song.id == currentSongId,
                            onClick = {
                                onCommitQuery(uiState.query)
                                onPlaySong(song, uiState.songs)
                            },
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
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
        color = VibePrimaryText,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
    )
}
