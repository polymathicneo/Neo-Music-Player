package com.vibeplay.music.data.local.lyrics

import android.content.ContentUris
import android.content.Context
import android.os.Build
import android.provider.MediaStore
import com.vibeplay.music.domain.model.LyricsData
import com.vibeplay.music.domain.model.LyricsSource
import com.vibeplay.music.domain.model.Song
import com.vibeplay.music.domain.repository.LyricsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LyricsRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val embeddedLyricsReader: EmbeddedLyricsReader
) : LyricsRepository {

    override suspend fun loadLyricsForSong(song: Song): LyricsData = withContext(Dispatchers.IO) {
        // 1. Check sibling .lrc file on filesystem
        if (song.filePath.isNotBlank()) {
            val audioFile = File(song.filePath)
            val baseName = audioFile.nameWithoutExtension
            val parentDir = audioFile.parentFile
            if (parentDir != null && baseName.isNotBlank()) {
                val lrcFile = File(parentDir, "$baseName.lrc")
                if (lrcFile.exists() && lrcFile.canRead()) {
                    val content = runCatching { lrcFile.readText(Charsets.UTF_8) }.getOrNull()
                    if (!content.isNullOrBlank()) {
                        val parsed = LrcParser.parseLrc(content, LyricsSource.LRC_FILE)
                        if (!parsed.isEmpty) return@withContext parsed
                    }
                }
            }
        }

        // 2. Scoped storage fallback via MediaStore.Files for matching .lrc filename
        val mediaStoreLrc = findLrcInMediaStore(song)
        if (!mediaStoreLrc.isNullOrBlank()) {
            val parsed = LrcParser.parseLrc(mediaStoreLrc, LyricsSource.LRC_FILE)
            if (!parsed.isEmpty) return@withContext parsed
        }

        // 3. Fallback to embedded lyrics inside audio metadata
        val embedded = embeddedLyricsReader.readEmbeddedLyrics(
            contentUri = song.contentUri,
            mimeType = song.mimeType,
            filePath = song.filePath
        )
        if (!embedded.isNullOrBlank()) {
            return@withContext LrcParser.parseEmbeddedText(embedded)
        }

        LyricsData()
    }

    private fun findLrcInMediaStore(song: Song): String? {
        if (song.filePath.isBlank()) return null
        val expectedBaseName = File(song.filePath).nameWithoutExtension
        if (expectedBaseName.isBlank()) return null
        val expectedLrcName = "$expectedBaseName.lrc"

        val filesUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Files.getContentUri("external")
        }

        return runCatching {
            context.contentResolver.query(
                filesUri,
                arrayOf(MediaStore.Files.FileColumns._ID, MediaStore.Files.FileColumns.DISPLAY_NAME),
                "${MediaStore.Files.FileColumns.DISPLAY_NAME} = ?",
                arrayOf(expectedLrcName),
                null
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val id = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID))
                    val uri = ContentUris.withAppendedId(filesUri, id)
                    context.contentResolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)?.use {
                        it.readText()
                    }
                } else {
                    null
                }
            }
        }.getOrNull()
    }
}
