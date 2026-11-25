package com.vibeplay.music.data.local.scanner

import com.vibeplay.music.domain.model.ScannerSettings

object AudioFilterRules {

    val SUPPORTED_EXTENSIONS = setOf("mp3", "flac", "wav", "aac", "m4a", "ogg")

    val SUPPORTED_MIME_TYPES = setOf(
        "audio/mpeg",
        "audio/mp3",
        "audio/flac",
        "audio/x-flac",
        "audio/wav",
        "audio/x-wav",
        "audio/vnd.wave",
        "audio/aac",
        "audio/aac-adts",
        "audio/mp4",
        "audio/m4a",
        "audio/x-m4a",
        "audio/ogg",
        "application/ogg",
        "audio/vorbis"
    )

    fun isSupportedAudioFormat(fileNameOrPath: String, mimeType: String?): Boolean {
        val extension = fileNameOrPath.substringAfterLast('.', "").lowercase()
        if (extension in SUPPORTED_EXTENSIONS) return true
        val normalizedMime = mimeType?.lowercase()?.trim().orEmpty()
        return normalizedMime in SUPPORTED_MIME_TYPES
    }

    fun shouldIncludeTrack(
        fileNameOrPath: String,
        mimeType: String?,
        durationMs: Long,
        folderPath: String,
        settings: ScannerSettings
    ): Boolean {
        if (!isSupportedAudioFormat(fileNameOrPath, mimeType)) return false
        if (durationMs < settings.minDurationMs) return false
        if (folderPath in settings.excludedFolders) return false
        if (settings.includedFolders.isNotEmpty() && folderPath !in settings.includedFolders) {
            return false
        }
        return true
    }

    fun extractFolderPathAndName(fullPath: String?, relativePath: String?): Pair<String, String> {
        val rawFolder = when {
            !fullPath.isNullOrBlank() -> fullPath.substringBeforeLast('/', "").trimEnd('/')
            !relativePath.isNullOrBlank() -> relativePath.trimEnd('/')
            else -> ""
        }
        val normalizedFolder = rawFolder.ifBlank { "Internal Storage" }
        val folderName = normalizedFolder.substringAfterLast('/').ifBlank { normalizedFolder }
        return normalizedFolder to folderName
    }
}
