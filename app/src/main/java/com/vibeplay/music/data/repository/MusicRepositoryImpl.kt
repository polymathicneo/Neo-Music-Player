package com.vibeplay.music.data.repository

import com.vibeplay.music.data.local.db.dao.SearchHistoryDao
import com.vibeplay.music.data.local.db.dao.SongDao
import com.vibeplay.music.data.local.db.entity.SearchHistoryEntity
import com.vibeplay.music.data.local.prefs.UserPreferencesDataStore
import com.vibeplay.music.data.local.scanner.MediaStoreScanner
import com.vibeplay.music.domain.model.Album
import com.vibeplay.music.domain.model.Artist
import com.vibeplay.music.domain.model.DiscoveredFolder
import com.vibeplay.music.domain.model.LibraryScanResult
import com.vibeplay.music.domain.model.MusicFolder
import com.vibeplay.music.domain.model.Song
import com.vibeplay.music.domain.repository.MusicRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MusicRepositoryImpl @Inject constructor(
    private val songDao: SongDao,
    private val searchHistoryDao: SearchHistoryDao,
    private val scanner: MediaStoreScanner,
    private val prefs: UserPreferencesDataStore
) : MusicRepository {

    private val scanMutex = Mutex()
    private val _discoveredFolders = MutableStateFlow<List<DiscoveredFolder>>(emptyList())
    override val discoveredFolders: Flow<List<DiscoveredFolder>> = _discoveredFolders.asStateFlow()

    private val _isScanning = MutableStateFlow(false)
    override val isScanning: Flow<Boolean> = _isScanning.asStateFlow()

    override fun observeAllSongs(): Flow<List<Song>> =
        songDao.observeAllSongs().map { list -> list.map { it.toDomain() } }

    override fun observeLikedSongs(): Flow<List<Song>> =
        songDao.observeLikedSongs().map { list -> list.map { it.toDomain() } }

    override fun observeRecentlyAdded(limit: Int): Flow<List<Song>> =
        songDao.observeRecentlyAdded(limit).map { list -> list.map { it.toDomain() } }

    override fun observeMostPlayed(limit: Int): Flow<List<Song>> =
        songDao.observeMostPlayed(limit).map { list -> list.map { it.toDomain() } }

    override fun observeRecentlyPlayed(limit: Int): Flow<List<Song>> =
        songDao.observeRecentlyPlayed(limit).map { list -> list.map { it.toDomain() } }

    override fun observeAlbums(): Flow<List<Album>> =
        observeAllSongs().map { songs ->
            songs.groupBy { it.albumId to it.album }
                .values
                .map { albumSongs ->
                    val first = albumSongs.first()
                    Album(
                        id = first.albumId,
                        name = first.album,
                        artist = first.artist,
                        songCount = albumSongs.size,
                        artworkUri = albumSongs.firstNotNullOfOrNull { it.albumArtUri },
                        firstSongId = first.id,
                        firstSongContentUri = first.contentUri
                    )
                }
                .sortedBy { it.name.lowercase() }
        }

    override fun observeArtists(): Flow<List<Artist>> =
        observeAllSongs().map { songs ->
            songs.groupBy { it.artist }
                .values
                .map { artistSongs ->
                    val first = artistSongs.first()
                    Artist(
                        name = first.artist,
                        songCount = artistSongs.size,
                        albumCount = artistSongs.map { it.albumId }.distinct().size,
                        artworkUri = artistSongs.firstNotNullOfOrNull { it.albumArtUri },
                        firstSongId = first.id,
                        firstSongContentUri = first.contentUri
                    )
                }
                .sortedBy { it.name.lowercase() }
        }

    override fun observeFolders(): Flow<List<MusicFolder>> =
        observeAllSongs().map { songs ->
            songs.groupBy { it.folderPath }
                .values
                .map { folderSongs ->
                    val first = folderSongs.first()
                    MusicFolder(
                        path = first.folderPath,
                        name = first.folderName,
                        songCount = folderSongs.size,
                        firstSongId = first.id,
                        firstSongContentUri = first.contentUri,
                        artworkUri = folderSongs.firstNotNullOfOrNull { it.albumArtUri }
                    )
                }
                .sortedBy { it.name.lowercase() }
        }

    override fun observeSongsByAlbum(albumId: Long): Flow<List<Song>> =
        songDao.observeSongsByAlbum(albumId).map { list -> list.map { it.toDomain() } }

    override fun observeSongsByArtist(artistName: String): Flow<List<Song>> =
        songDao.observeSongsByArtist(artistName).map { list -> list.map { it.toDomain() } }

    override fun observeSongsByFolder(folderPath: String): Flow<List<Song>> =
        songDao.observeSongsByFolder(folderPath).map { list -> list.map { it.toDomain() } }

    override fun searchSongs(query: String): Flow<List<Song>> =
        songDao.searchSongs(query).map { list -> list.map { it.toDomain() } }

    override fun observeSearchHistory(): Flow<List<String>> =
        searchHistoryDao.observeRecentQueries()

    override suspend fun getSongById(songId: Long): Song? =
        songDao.getSongById(songId)?.toDomain()

    override suspend fun getAllSongsOnce(): List<Song> =
        songDao.getAllSongsOnce().map { it.toDomain() }

    override suspend fun rescanLibrary(): LibraryScanResult = scanMutex.withLock {
        _isScanning.value = true
        try {
            val settings = prefs.getScannerSettingsOnce()
            val scanOutput = scanner.scanDeviceAudio(settings)
            _discoveredFolders.value = scanOutput.discoveredFolders
            songDao.replaceAllSongs(scanOutput.songs)
            LibraryScanResult(
                importedCount = scanOutput.songs.size,
                discoveredFolders = scanOutput.discoveredFolders
            )
        } finally {
            _isScanning.value = false
        }
    }

    override suspend fun toggleFavorite(songId: Long) {
        songDao.toggleFavorite(songId)
    }

    override suspend fun recordLastPlayed(songId: Long) {
        songDao.recordLastPlayed(songId)
    }

    override suspend fun incrementPlayCount(songId: Long) {
        songDao.incrementPlayCount(songId)
    }

    override suspend fun saveSearchQuery(query: String) {
        val trimmed = query.trim()
        if (trimmed.isNotEmpty()) {
            searchHistoryDao.upsertQuery(SearchHistoryEntity(query = trimmed))
        }
    }

    override suspend fun deleteSearchQuery(query: String) {
        searchHistoryDao.deleteQuery(query)
    }

    override suspend fun clearSearchHistory() {
        searchHistoryDao.clearAll()
    }
}
