package com.vibeplay.music.domain.repository

import com.vibeplay.music.domain.model.Playlist
import com.vibeplay.music.domain.model.PlaylistWithSongs
import kotlinx.coroutines.flow.Flow

interface PlaylistRepository {
    fun observePlaylists(): Flow<List<Playlist>>
    fun observePlaylistWithSongs(playlistId: Long): Flow<PlaylistWithSongs?>
    suspend fun createPlaylist(name: String): Long
    suspend fun renamePlaylist(playlistId: Long, newName: String)
    suspend fun deletePlaylist(playlistId: Long)
    suspend fun addSongToPlaylist(playlistId: Long, songId: Long)
    suspend fun removeSongFromPlaylist(playlistId: Long, songId: Long)
    suspend fun reorderPlaylistSongs(playlistId: Long, orderedSongIds: List<Long>)
}
