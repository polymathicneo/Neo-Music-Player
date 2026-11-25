package com.vibeplay.music.data.repository

import com.vibeplay.music.data.local.db.dao.PlaylistDao
import com.vibeplay.music.data.local.db.entity.PlaylistEntity
import com.vibeplay.music.domain.model.Playlist
import com.vibeplay.music.domain.model.PlaylistWithSongs
import com.vibeplay.music.domain.repository.PlaylistRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlaylistRepositoryImpl @Inject constructor(
    private val playlistDao: PlaylistDao
) : PlaylistRepository {

    override fun observePlaylists(): Flow<List<Playlist>> =
        playlistDao.observePlaylists().map { rows ->
            rows.map { row ->
                Playlist(
                    id = row.id,
                    name = row.name,
                    createdAt = row.createdAt,
                    songCount = row.songCount,
                    coverSongId = row.coverSongId,
                    coverContentUri = row.coverContentUri,
                    coverArtUri = row.coverArtUri
                )
            }
        }

    override fun observePlaylistWithSongs(playlistId: Long): Flow<PlaylistWithSongs?> =
        combine(
            playlistDao.observePlaylistById(playlistId),
            playlistDao.observeSongsInPlaylist(playlistId)
        ) { entity, songRows ->
            entity ?: return@combine null
            val songs = songRows.map { it.toDomain() }
            val first = songs.firstOrNull()
            PlaylistWithSongs(
                playlist = Playlist(
                    id = entity.id,
                    name = entity.name,
                    createdAt = entity.createdAt,
                    songCount = songs.size,
                    coverSongId = first?.id,
                    coverContentUri = first?.contentUri,
                    coverArtUri = first?.albumArtUri
                ),
                songs = songs
            )
        }

    override suspend fun createPlaylist(name: String): Long {
        val clean = name.trim().ifEmpty { "My Playlist" }
        return playlistDao.insertPlaylist(PlaylistEntity(name = clean))
    }

    override suspend fun renamePlaylist(playlistId: Long, newName: String) {
        val clean = newName.trim()
        if (clean.isNotEmpty()) {
            playlistDao.renamePlaylist(playlistId, clean)
        }
    }

    override suspend fun deletePlaylist(playlistId: Long) {
        playlistDao.deletePlaylist(playlistId)
    }

    override suspend fun addSongToPlaylist(playlistId: Long, songId: Long) {
        playlistDao.addSongToPlaylist(playlistId, songId)
    }

    override suspend fun removeSongFromPlaylist(playlistId: Long, songId: Long) {
        playlistDao.removeSongFromPlaylist(playlistId, songId)
    }

    override suspend fun reorderPlaylistSongs(playlistId: Long, orderedSongIds: List<Long>) {
        playlistDao.reorderPlaylistSongs(playlistId, orderedSongIds)
    }
}
