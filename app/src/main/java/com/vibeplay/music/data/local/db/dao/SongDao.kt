package com.vibeplay.music.data.local.db.dao

import androidx.room.Dao
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.vibeplay.music.data.local.db.entity.SongEntity
import com.vibeplay.music.data.local.db.entity.SongStatsEntity
import com.vibeplay.music.domain.model.Song
import kotlinx.coroutines.flow.Flow

data class SongWithStats(
    @Embedded val song: SongEntity,
    val isFavorite: Boolean?,
    val playCount: Int?,
    val lastPlayedAt: Long?
) {
    fun toDomain(): Song = Song(
        id = song.id,
        title = song.title,
        artist = song.artist,
        album = song.album,
        albumId = song.albumId,
        durationMs = song.durationMs,
        contentUri = song.contentUri,
        filePath = song.filePath,
        folderPath = song.folderPath,
        folderName = song.folderName,
        dateAdded = song.dateAdded,
        trackNumber = song.trackNumber,
        year = song.year,
        mimeType = song.mimeType,
        sizeBytes = song.sizeBytes,
        albumArtUri = song.albumArtUri,
        isFavorite = isFavorite ?: false,
        playCount = playCount ?: 0,
        lastPlayedAt = lastPlayedAt ?: 0L
    )
}

@Dao
interface SongDao {

    @Query(
        """
        SELECT s.*, st.isFavorite, st.playCount, st.lastPlayedAt
        FROM songs s
        LEFT JOIN song_stats st ON s.id = st.songId
        ORDER BY s.title COLLATE NOCASE ASC
        """
    )
    fun observeAllSongs(): Flow<List<SongWithStats>>

    @Query(
        """
        SELECT s.*, st.isFavorite, st.playCount, st.lastPlayedAt
        FROM songs s
        LEFT JOIN song_stats st ON s.id = st.songId
        ORDER BY s.title COLLATE NOCASE ASC
        """
    )
    suspend fun getAllSongsOnce(): List<SongWithStats>

    @Query(
        """
        SELECT s.*, st.isFavorite, st.playCount, st.lastPlayedAt
        FROM songs s
        INNER JOIN song_stats st ON s.id = st.songId
        WHERE st.isFavorite = 1
        ORDER BY st.favoritedAt DESC
        """
    )
    fun observeLikedSongs(): Flow<List<SongWithStats>>

    @Query(
        """
        SELECT s.*, st.isFavorite, st.playCount, st.lastPlayedAt
        FROM songs s
        LEFT JOIN song_stats st ON s.id = st.songId
        ORDER BY s.dateAdded DESC
        LIMIT :limit
        """
    )
    fun observeRecentlyAdded(limit: Int): Flow<List<SongWithStats>>

    @Query(
        """
        SELECT s.*, st.isFavorite, st.playCount, st.lastPlayedAt
        FROM songs s
        INNER JOIN song_stats st ON s.id = st.songId
        WHERE st.playCount > 0
        ORDER BY st.playCount DESC, st.lastPlayedAt DESC
        LIMIT :limit
        """
    )
    fun observeMostPlayed(limit: Int): Flow<List<SongWithStats>>

    @Query(
        """
        SELECT s.*, st.isFavorite, st.playCount, st.lastPlayedAt
        FROM songs s
        INNER JOIN song_stats st ON s.id = st.songId
        WHERE st.lastPlayedAt > 0
        ORDER BY st.lastPlayedAt DESC
        LIMIT :limit
        """
    )
    fun observeRecentlyPlayed(limit: Int): Flow<List<SongWithStats>>

    @Query(
        """
        SELECT s.*, st.isFavorite, st.playCount, st.lastPlayedAt
        FROM songs s
        LEFT JOIN song_stats st ON s.id = st.songId
        WHERE s.albumId = :albumId
        ORDER BY s.trackNumber ASC, s.title COLLATE NOCASE ASC
        """
    )
    fun observeSongsByAlbum(albumId: Long): Flow<List<SongWithStats>>

    @Query(
        """
        SELECT s.*, st.isFavorite, st.playCount, st.lastPlayedAt
        FROM songs s
        LEFT JOIN song_stats st ON s.id = st.songId
        WHERE s.artist = :artistName
        ORDER BY s.album COLLATE NOCASE ASC, s.trackNumber ASC, s.title COLLATE NOCASE ASC
        """
    )
    fun observeSongsByArtist(artistName: String): Flow<List<SongWithStats>>

    @Query(
        """
        SELECT s.*, st.isFavorite, st.playCount, st.lastPlayedAt
        FROM songs s
        LEFT JOIN song_stats st ON s.id = st.songId
        WHERE s.folderPath = :folderPath
        ORDER BY s.title COLLATE NOCASE ASC
        """
    )
    fun observeSongsByFolder(folderPath: String): Flow<List<SongWithStats>>

    @Query(
        """
        SELECT s.*, st.isFavorite, st.playCount, st.lastPlayedAt
        FROM songs s
        LEFT JOIN song_stats st ON s.id = st.songId
        WHERE s.title LIKE '%' || :query || '%'
           OR s.artist LIKE '%' || :query || '%'
           OR s.album LIKE '%' || :query || '%'
           OR s.folderName LIKE '%' || :query || '%'
        ORDER BY s.title COLLATE NOCASE ASC
        """
    )
    fun searchSongs(query: String): Flow<List<SongWithStats>>

    @Query(
        """
        SELECT s.*, st.isFavorite, st.playCount, st.lastPlayedAt
        FROM songs s
        LEFT JOIN song_stats st ON s.id = st.songId
        WHERE s.id = :songId
        LIMIT 1
        """
    )
    suspend fun getSongById(songId: Long): SongWithStats?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSongs(songs: List<SongEntity>)

    @Query("DELETE FROM songs")
    suspend fun clearSongs()

    @Transaction
    suspend fun replaceAllSongs(songs: List<SongEntity>) {
        clearSongs()
        if (songs.isNotEmpty()) {
            upsertSongs(songs)
        }
    }

    @Query("SELECT * FROM song_stats WHERE songId = :songId LIMIT 1")
    suspend fun getStats(songId: Long): SongStatsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertStats(stats: SongStatsEntity)

    @Transaction
    suspend fun toggleFavorite(songId: Long) {
        val current = getStats(songId)
        val newFav = !(current?.isFavorite ?: false)
        upsertStats(
            SongStatsEntity(
                songId = songId,
                isFavorite = newFav,
                favoritedAt = if (newFav) System.currentTimeMillis() else 0L,
                playCount = current?.playCount ?: 0,
                lastPlayedAt = current?.lastPlayedAt ?: 0L
            )
        )
    }

    @Transaction
    suspend fun recordLastPlayed(songId: Long, timestamp: Long = System.currentTimeMillis()) {
        val current = getStats(songId)
        upsertStats(
            SongStatsEntity(
                songId = songId,
                isFavorite = current?.isFavorite ?: false,
                favoritedAt = current?.favoritedAt ?: 0L,
                playCount = current?.playCount ?: 0,
                lastPlayedAt = timestamp
            )
        )
    }

    @Transaction
    suspend fun incrementPlayCount(songId: Long) {
        val current = getStats(songId)
        upsertStats(
            SongStatsEntity(
                songId = songId,
                isFavorite = current?.isFavorite ?: false,
                favoritedAt = current?.favoritedAt ?: 0L,
                playCount = (current?.playCount ?: 0) + 1,
                lastPlayedAt = System.currentTimeMillis()
            )
        )
    }
}
