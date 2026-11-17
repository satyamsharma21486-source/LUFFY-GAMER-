package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "scripts")
data class ScriptEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val title: String,
    val scriptContent: String,
    val imagePath: String? = null,
    val sampleDrawableName: String? = null,
    val createdAtMillis: Long = System.currentTimeMillis()
)
