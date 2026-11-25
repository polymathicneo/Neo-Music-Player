package com.vibeplay.music.data.local.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "songs")
data class SongEntity(
    @PrimaryKey val id: Long,
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
    val albumArtUri: String?
)
