package com.vibeplay.music.data.local.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.vibeplay.music.data.local.db.entity.PlaylistEntity
import com.vibeplay.music.data.local.db.entity.PlaylistSongCrossRef
import kotlinx.coroutines.flow.Flow

data class PlaylistSummaryRow(
    val id: Long,
    val name: String,
    val createdAt: Long,
    val songCount: Int,
    val coverSongId: Long?,
    val coverContentUri: String?,
    val coverArtUri: String?
)

@Dao
interface PlaylistDao {

    @Query(
        """
        SELECT p.id, p.name, p.createdAt,
               COUNT(s.id) AS songCount,
               (
                   SELECT s2.id FROM playlist_songs ps2
                   INNER JOIN songs s2 ON ps2.songId = s2.id
                   WHERE ps2.playlistId = p.id
                   ORDER BY ps2.position ASC
                   LIMIT 1
               ) AS coverSongId,
               (
                   SELECT s2.contentUri FROM playlist_songs ps2
                   INNER JOIN songs s2 ON ps2.songId = s2.id
                   WHERE ps2.playlistId = p.id
                   ORDER BY ps2.position ASC
                   LIMIT 1
               ) AS coverContentUri,
               (
                   SELECT s2.albumArtUri FROM playlist_songs ps2
                   INNER JOIN songs s2 ON ps2.songId = s2.id
                   WHERE ps2.playlistId = p.id
                   ORDER BY ps2.position ASC
                   LIMIT 1
               ) AS coverArtUri
        FROM playlists p
        LEFT JOIN playlist_songs ps ON p.id = ps.playlistId
        LEFT JOIN songs s ON ps.songId = s.id
        GROUP BY p.id
        ORDER BY p.createdAt DESC
        """
    )
    fun observePlaylists(): Flow<List<PlaylistSummaryRow>>

    @Query("SELECT * FROM playlists WHERE id = :playlistId LIMIT 1")
    fun observePlaylistById(playlistId: Long): Flow<PlaylistEntity?>

    @Query(
        """
        SELECT s.*, st.isFavorite, st.playCount, st.lastPlayedAt
        FROM playlist_songs ps
        INNER JOIN songs s ON ps.songId = s.id
        LEFT JOIN song_stats st ON s.id = st.songId
        WHERE ps.playlistId = :playlistId
        ORDER BY ps.position ASC, ps.addedAt ASC
        """
    )
    fun observeSongsInPlaylist(playlistId: Long): Flow<List<SongWithStats>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylist(playlist: PlaylistEntity): Long

    @Query("UPDATE playlists SET name = :newName WHERE id = :playlistId")
    suspend fun renamePlaylist(playlistId: Long, newName: String)

    @Query("DELETE FROM playlists WHERE id = :playlistId")
    suspend fun deletePlaylist(playlistId: Long)

    @Query("SELECT COALESCE(MAX(position), -1) FROM playlist_songs WHERE playlistId = :playlistId")
    suspend fun getMaxPosition(playlistId: Long): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertCrossRef(crossRef: PlaylistSongCrossRef)

    @Transaction
    suspend fun addSongToPlaylist(playlistId: Long, songId: Long) {
        val nextPos = getMaxPosition(playlistId) + 1
        insertCrossRef(
            PlaylistSongCrossRef(
                playlistId = playlistId,
                songId = songId,
                position = nextPos
            )
        )
    }

    @Query("DELETE FROM playlist_songs WHERE playlistId = :playlistId AND songId = :songId")
    suspend fun removeSongFromPlaylist(playlistId: Long, songId: Long)

    @Query("UPDATE playlist_songs SET position = :position WHERE playlistId = :playlistId AND songId = :songId")
    suspend fun updateSongPosition(playlistId: Long, songId: Long, position: Int)

    @Transaction
    suspend fun reorderPlaylistSongs(playlistId: Long, orderedSongIds: List<Long>) {
        orderedSongIds.forEachIndexed { index, songId ->
            updateSongPosition(playlistId, songId, index)
        }
    }
}
