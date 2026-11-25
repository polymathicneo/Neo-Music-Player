package com.vibeplay.music.domain.repository

import com.vibeplay.music.domain.model.Album
import com.vibeplay.music.domain.model.Artist
import com.vibeplay.music.domain.model.DiscoveredFolder
import com.vibeplay.music.domain.model.LibraryScanResult
import com.vibeplay.music.domain.model.MusicFolder
import com.vibeplay.music.domain.model.Song
import kotlinx.coroutines.flow.Flow

interface MusicRepository {
    val discoveredFolders: Flow<List<DiscoveredFolder>>
    val isScanning: Flow<Boolean>

    fun observeAllSongs(): Flow<List<Song>>
    fun observeLikedSongs(): Flow<List<Song>>
    fun observeRecentlyAdded(limit: Int = 20): Flow<List<Song>>
    fun observeMostPlayed(limit: Int = 20): Flow<List<Song>>
    fun observeRecentlyPlayed(limit: Int = 20): Flow<List<Song>>
    fun observeAlbums(): Flow<List<Album>>
    fun observeArtists(): Flow<List<Artist>>
    fun observeFolders(): Flow<List<MusicFolder>>
    fun observeSongsByAlbum(albumId: Long): Flow<List<Song>>
    fun observeSongsByArtist(artistName: String): Flow<List<Song>>
    fun observeSongsByFolder(folderPath: String): Flow<List<Song>>
    fun searchSongs(query: String): Flow<List<Song>>
    fun observeSearchHistory(): Flow<List<String>>

    suspend fun getSongById(songId: Long): Song?
    suspend fun getAllSongsOnce(): List<Song>
    suspend fun rescanLibrary(): LibraryScanResult
    suspend fun toggleFavorite(songId: Long)
    suspend fun recordLastPlayed(songId: Long)
    suspend fun incrementPlayCount(songId: Long)
    suspend fun saveSearchQuery(query: String)
    suspend fun deleteSearchQuery(query: String)
    suspend fun clearSearchHistory()
}
