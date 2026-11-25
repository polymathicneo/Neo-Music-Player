package com.vibeplay.music.data.local.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.vibeplay.music.domain.model.ScannerSettings
import com.vibeplay.music.domain.model.SongSortConfig
import com.vibeplay.music.domain.model.SongSortOption
import com.vibeplay.music.domain.model.SortDirection
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "vibeplay_prefs")

data class EqualizerPrefsSnapshot(
    val enabled: Boolean,
    val presetName: String,
    val bandLevelsMb: List<Int>,
    val bassBoostStrength: Int
)

data class WidgetPlaybackSnapshot(
    val songId: Long = -1L,
    val title: String = "Nothing playing",
    val artist: String = "NeoMusic Offline",
    val contentUri: String = "",
    val albumArtUri: String = "",
    val isPlaying: Boolean = false
)

@Singleton
class UserPreferencesDataStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private object Keys {
        val MIN_DURATION_MS = longPreferencesKey("min_duration_ms")
        val INCLUDED_FOLDERS = stringSetPreferencesKey("included_folders")
        val EXCLUDED_FOLDERS = stringSetPreferencesKey("excluded_folders")
        val CROSSFADE_SEC = intPreferencesKey("crossfade_duration_sec")

        val EQ_ENABLED = booleanPreferencesKey("eq_enabled")
        val EQ_PRESET_NAME = stringPreferencesKey("eq_preset_name")
        val EQ_BAND_LEVELS = stringPreferencesKey("eq_band_levels")
        val EQ_BASS_BOOST = intPreferencesKey("eq_bass_boost")

        val WIDGET_SONG_ID = longPreferencesKey("widget_song_id")
        val WIDGET_TITLE = stringPreferencesKey("widget_title")
        val WIDGET_ARTIST = stringPreferencesKey("widget_artist")
        val WIDGET_CONTENT_URI = stringPreferencesKey("widget_content_uri")
        val WIDGET_ALBUM_ART_URI = stringPreferencesKey("widget_album_art_uri")
        val WIDGET_IS_PLAYING = booleanPreferencesKey("widget_is_playing")

        val SONG_SORT_OPTION = stringPreferencesKey("song_sort_option")
        val SONG_SORT_DIRECTION = stringPreferencesKey("song_sort_direction")
    }

    val scannerSettings: Flow<ScannerSettings> = context.dataStore.data.map { prefs ->
        ScannerSettings(
            minDurationMs = prefs[Keys.MIN_DURATION_MS] ?: 15_000L,
            includedFolders = prefs[Keys.INCLUDED_FOLDERS] ?: emptySet(),
            excludedFolders = prefs[Keys.EXCLUDED_FOLDERS] ?: emptySet(),
            crossfadeDurationSec = (prefs[Keys.CROSSFADE_SEC] ?: 0).coerceIn(0, 12)
        )
    }

    val equalizerPrefs: Flow<EqualizerPrefsSnapshot> = context.dataStore.data.map { prefs ->
        val rawBands = prefs[Keys.EQ_BAND_LEVELS].orEmpty()
        val parsedBands = if (rawBands.isBlank()) {
            emptyList()
        } else {
            rawBands.split(",").mapNotNull { it.trim().toIntOrNull() }
        }
        EqualizerPrefsSnapshot(
            enabled = prefs[Keys.EQ_ENABLED] ?: false,
            presetName = prefs[Keys.EQ_PRESET_NAME] ?: "Flat",
            bandLevelsMb = parsedBands,
            bassBoostStrength = (prefs[Keys.EQ_BASS_BOOST] ?: 0).coerceIn(0, 1000)
        )
    }

    val widgetSnapshot: Flow<WidgetPlaybackSnapshot> = context.dataStore.data.map { prefs ->
        WidgetPlaybackSnapshot(
            songId = prefs[Keys.WIDGET_SONG_ID] ?: -1L,
            title = prefs[Keys.WIDGET_TITLE] ?: "Nothing playing",
            artist = prefs[Keys.WIDGET_ARTIST] ?: "Tap to open NeoMusic",
            contentUri = prefs[Keys.WIDGET_CONTENT_URI] ?: "",
            albumArtUri = prefs[Keys.WIDGET_ALBUM_ART_URI] ?: "",
            isPlaying = prefs[Keys.WIDGET_IS_PLAYING] ?: false
        )
    }

    val songSortConfig: Flow<SongSortConfig> = context.dataStore.data.map { prefs ->
        val option = prefs[Keys.SONG_SORT_OPTION]?.let { name ->
            runCatching { SongSortOption.valueOf(name) }.getOrNull()
        } ?: SongSortOption.DATE_ADDED
        val direction = prefs[Keys.SONG_SORT_DIRECTION]?.let { name ->
            runCatching { SortDirection.valueOf(name) }.getOrNull()
        } ?: option.defaultDirection
        SongSortConfig(option = option, direction = direction)
    }

    suspend fun getScannerSettingsOnce(): ScannerSettings = scannerSettings.first()

    suspend fun getEqualizerPrefsOnce(): EqualizerPrefsSnapshot = equalizerPrefs.first()

    suspend fun getWidgetSnapshotOnce(): WidgetPlaybackSnapshot = widgetSnapshot.first()

    suspend fun setMinDurationMs(minDurationMs: Long) {
        context.dataStore.edit { it[Keys.MIN_DURATION_MS] = minDurationMs.coerceAtLeast(0L) }
    }

    suspend fun toggleIncludedFolder(folderPath: String) {
        context.dataStore.edit { prefs ->
            val current = prefs[Keys.INCLUDED_FOLDERS]?.toMutableSet() ?: mutableSetOf()
            if (!current.add(folderPath)) {
                current.remove(folderPath)
            }
            prefs[Keys.INCLUDED_FOLDERS] = current
            if (folderPath in current) {
                val excluded = prefs[Keys.EXCLUDED_FOLDERS]?.toMutableSet() ?: mutableSetOf()
                excluded.remove(folderPath)
                prefs[Keys.EXCLUDED_FOLDERS] = excluded
            }
        }
    }

    suspend fun clearIncludedFolders() {
        context.dataStore.edit { it[Keys.INCLUDED_FOLDERS] = emptySet() }
    }

    suspend fun toggleExcludedFolder(folderPath: String) {
        context.dataStore.edit { prefs ->
            val current = prefs[Keys.EXCLUDED_FOLDERS]?.toMutableSet() ?: mutableSetOf()
            if (!current.add(folderPath)) {
                current.remove(folderPath)
            }
            prefs[Keys.EXCLUDED_FOLDERS] = current
            if (folderPath in current) {
                val included = prefs[Keys.INCLUDED_FOLDERS]?.toMutableSet() ?: mutableSetOf()
                included.remove(folderPath)
                prefs[Keys.INCLUDED_FOLDERS] = included
            }
        }
    }

    suspend fun setCrossfadeDurationSec(seconds: Int) {
        context.dataStore.edit { it[Keys.CROSSFADE_SEC] = seconds.coerceIn(0, 12) }
    }

    suspend fun setSongSort(option: SongSortOption, direction: SortDirection) {
        context.dataStore.edit { prefs ->
            prefs[Keys.SONG_SORT_OPTION] = option.name
            prefs[Keys.SONG_SORT_DIRECTION] = direction.name
        }
    }

    suspend fun saveEqualizerState(
        enabled: Boolean,
        presetName: String,
        bandLevelsMb: List<Int>,
        bassBoostStrength: Int
    ) {
        context.dataStore.edit { prefs ->
            prefs[Keys.EQ_ENABLED] = enabled
            prefs[Keys.EQ_PRESET_NAME] = presetName
            prefs[Keys.EQ_BAND_LEVELS] = bandLevelsMb.joinToString(",")
            prefs[Keys.EQ_BASS_BOOST] = bassBoostStrength.coerceIn(0, 1000)
        }
    }

    suspend fun updateWidgetSnapshot(snapshot: WidgetPlaybackSnapshot) {
        context.dataStore.edit { prefs ->
            prefs[Keys.WIDGET_SONG_ID] = snapshot.songId
            prefs[Keys.WIDGET_TITLE] = snapshot.title
            prefs[Keys.WIDGET_ARTIST] = snapshot.artist
            prefs[Keys.WIDGET_CONTENT_URI] = snapshot.contentUri
            prefs[Keys.WIDGET_ALBUM_ART_URI] = snapshot.albumArtUri
            prefs[Keys.WIDGET_IS_PLAYING] = snapshot.isPlaying
        }
    }
}
