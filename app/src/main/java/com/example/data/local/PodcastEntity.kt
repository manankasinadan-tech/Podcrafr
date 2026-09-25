package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "podcasts")
data class PodcastEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val scriptText: String,
    val createdAt: Long = System.currentTimeMillis(),
    val totalTurns: Int = 0,
    val durationSec: Int = 0,
    val fullAudioPath: String? = null,
    val speakerCount: Int = 0,
    val category: String = "Podcast"
)
