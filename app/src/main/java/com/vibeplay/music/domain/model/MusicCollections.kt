package com.vibeplay.music.domain.model

data class Album(
    val id: Long,
    val name: String,
    val artist: String,
    val songCount: Int,
    val artworkUri: String?,
    val firstSongId: Long,
    val firstSongContentUri: String
)

data class Artist(
    val name: String,
    val songCount: Int,
    val albumCount: Int,
    val artworkUri: String?,
    val firstSongId: Long,
    val firstSongContentUri: String
)

data class MusicFolder(
    val path: String,
    val name: String,
    val songCount: Int,
    val firstSongId: Long = 0L,
    val firstSongContentUri: String = "",
    val artworkUri: String? = null
)

data class Playlist(
    val id: Long,
    val name: String,
    val createdAt: Long,
    val songCount: Int,
    val coverSongId: Long? = null,
    val coverContentUri: String? = null,
    val coverArtUri: String? = null
)

data class PlaylistWithSongs(
    val playlist: Playlist,
    val songs: List<Song>
)

data class HomeQuickItem(
    val id: String,
    val title: String,
    val subtitle: String,
    val type: QuickItemType,
    val targetId: String,
    val songId: Long?,
    val contentUri: String?,
    val artworkUri: String?
)

enum class QuickItemType {
    LIKED_SONGS,
    ALBUM,
    PLAYLIST
}
