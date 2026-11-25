package com.vibeplay.music.domain.model

data class Song(
    val id: Long,
    val title: String,
    val artist: String,
    val album: String,
    val albumId: Long,
    val durationMs: Long,
    val contentUri: String,
    val filePath: String,
    val folderPath: String,
    val folderName: String,
    val dateAdded: Long,
    val trackNumber: Int,
    val year: Int,
    val mimeType: String,
    val sizeBytes: Long,
    val albumArtUri: String?,
    val isFavorite: Boolean = false,
    val playCount: Int = 0,
    val lastPlayedAt: Long = 0L
)
