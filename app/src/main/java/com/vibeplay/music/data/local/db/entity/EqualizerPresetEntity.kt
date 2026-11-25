package com.vibeplay.music.data.local.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "equalizer_presets")
data class EqualizerPresetEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val name: String,
    val bandLevelsMb: List<Int>,
    val bassBoostStrength: Int,
    val isBuiltIn: Boolean = false
)
