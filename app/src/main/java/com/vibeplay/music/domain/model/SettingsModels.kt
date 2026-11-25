package com.vibeplay.music.domain.model

data class ScannerSettings(
    val minDurationMs: Long = 15_000L,
    val includedFolders: Set<String> = emptySet(),
    val excludedFolders: Set<String> = emptySet(),
    val crossfadeDurationSec: Int = 0
)

data class DiscoveredFolder(
    val path: String,
    val name: String,
    val totalFiles: Int
)

data class LibraryScanResult(
    val importedCount: Int,
    val discoveredFolders: List<DiscoveredFolder>
)
