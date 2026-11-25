package com.vibeplay.music.data.local.scanner

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import com.vibeplay.music.data.local.db.entity.SongEntity
import com.vibeplay.music.domain.model.DiscoveredFolder
import com.vibeplay.music.domain.model.ScannerSettings
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

data class MediaScanOutput(
    val songs: List<SongEntity>,
    val discoveredFolders: List<DiscoveredFolder>
)

@Singleton
class MediaStoreScanner @Inject constructor(
    @ApplicationContext private val context: Context
) {
    suspend fun scanDeviceAudio(settings: ScannerSettings): MediaScanOutput = withContext(Dispatchers.IO) {
        val songs = mutableListOf<SongEntity>()
        val folderCounts = linkedMapOf<String, Pair<String, Int>>()

        val collectionUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        }

        val projection = buildList {
            add(MediaStore.Audio.Media._ID)
            add(MediaStore.Audio.Media.TITLE)
            add(MediaStore.Audio.Media.ARTIST)
            add(MediaStore.Audio.Media.ALBUM)
            add(MediaStore.Audio.Media.ALBUM_ID)
            add(MediaStore.Audio.Media.DURATION)
            add(MediaStore.Audio.Media.DATE_ADDED)
            add(MediaStore.Audio.Media.TRACK)
            add(MediaStore.Audio.Media.YEAR)
            add(MediaStore.Audio.Media.MIME_TYPE)
            add(MediaStore.Audio.Media.SIZE)
            add(MediaStore.Audio.Media.DISPLAY_NAME)
            @Suppress("DEPRECATION")
            add(MediaStore.Audio.Media.DATA)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                add(MediaStore.Audio.Media.RELATIVE_PATH)
            }
        }.toTypedArray()

        val albumArtBaseUri = Uri.parse("content://media/external/audio/albumart")

        context.contentResolver.query(
            collectionUri,
            projection,
            null,
            null,
            "${MediaStore.Audio.Media.DATE_ADDED} DESC"
        )?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val albumCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
            val albumIdCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
            val durationCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            val dateAddedCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED)
            val trackCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TRACK)
            val yearCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.YEAR)
            val mimeCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.MIME_TYPE)
            val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE)
            val displayNameCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME)
            @Suppress("DEPRECATION")
            val dataCol = cursor.getColumnIndex(MediaStore.Audio.Media.DATA)
            val relPathCol = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                cursor.getColumnIndex(MediaStore.Audio.Media.RELATIVE_PATH)
            } else {
                -1
            }

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idCol)
                val displayName = cursor.getString(displayNameCol).orEmpty()
                val mimeType = cursor.getString(mimeCol).orEmpty()
                val filePath = if (dataCol >= 0) cursor.getString(dataCol).orEmpty() else ""
                val relativePath = if (relPathCol >= 0) cursor.getString(relPathCol) else null
                val durationMs = cursor.getLong(durationCol).coerceAtLeast(0L)

                if (!AudioFilterRules.isSupportedAudioFormat(filePath.ifBlank { displayName }, mimeType)) {
                    continue
                }

                val (folderPath, folderName) = AudioFilterRules.extractFolderPathAndName(
                    fullPath = filePath.takeIf { it.isNotBlank() },
                    relativePath = relativePath
                )

                // Track all audio folders with supported files so Settings can show excluded folders too
                if (durationMs >= settings.minDurationMs) {
                    val existing = folderCounts[folderPath]
                    folderCounts[folderPath] = folderName to ((existing?.second ?: 0) + 1)
                }

                if (!AudioFilterRules.shouldIncludeTrack(
                        fileNameOrPath = filePath.ifBlank { displayName },
                        mimeType = mimeType,
                        durationMs = durationMs,
                        folderPath = folderPath,
                        settings = settings
                    )
                ) {
                    continue
                }

                val contentUri = ContentUris.withAppendedId(
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    id
                ).toString()
                val albumId = cursor.getLong(albumIdCol)
                val albumArtUri = if (albumId > 0L) {
                    ContentUris.withAppendedId(albumArtBaseUri, albumId).toString()
                } else {
                    null
                }

                val rawTitle = cursor.getString(titleCol)?.trim().orEmpty()
                val cleanTitle = rawTitle.ifBlank {
                    displayName.substringBeforeLast('.').ifBlank { "Unknown Track" }
                }
                val rawArtist = cursor.getString(artistCol)?.trim().orEmpty()
                val cleanArtist = if (rawArtist.isBlank() || rawArtist == "<unknown>") {
                    "Unknown Artist"
                } else {
                    rawArtist
                }
                val rawAlbum = cursor.getString(albumCol)?.trim().orEmpty()
                val cleanAlbum = if (rawAlbum.isBlank() || rawAlbum == "<unknown>") {
                    "Unknown Album"
                } else {
                    rawAlbum
                }

                songs += SongEntity(
                    id = id,
                    title = cleanTitle,
                    artist = cleanArtist,
                    album = cleanAlbum,
                    albumId = albumId,
                    durationMs = durationMs,
                    contentUri = contentUri,
                    filePath = filePath,
                    folderPath = folderPath,
                    folderName = folderName,
                    dateAdded = cursor.getLong(dateAddedCol),
                    trackNumber = cursor.getInt(trackCol),
                    year = cursor.getInt(yearCol),
                    mimeType = mimeType,
                    sizeBytes = cursor.getLong(sizeCol),
                    albumArtUri = albumArtUri
                )
            }
        }

        val discovered = folderCounts.entries
            .map { (path, pair) ->
                DiscoveredFolder(
                    path = path,
                    name = pair.first,
                    totalFiles = pair.second
                )
            }
            .sortedBy { it.name.lowercase() }

        MediaScanOutput(songs = songs, discoveredFolders = discovered)
    }
}
