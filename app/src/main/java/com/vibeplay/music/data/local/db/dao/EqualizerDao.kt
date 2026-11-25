package com.vibeplay.music.data.local.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.vibeplay.music.data.local.db.entity.EqualizerPresetEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface EqualizerDao {
    @Query("SELECT * FROM equalizer_presets ORDER BY isBuiltIn DESC, id ASC")
    fun observePresets(): Flow<List<EqualizerPresetEntity>>

    @Query("SELECT COUNT(*) FROM equalizer_presets")
    suspend fun countPresets(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPreset(preset: EqualizerPresetEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPresets(presets: List<EqualizerPresetEntity>)

    @Query("DELETE FROM equalizer_presets WHERE id = :id AND isBuiltIn = 0")
    suspend fun deleteCustomPreset(id: Long)
}
