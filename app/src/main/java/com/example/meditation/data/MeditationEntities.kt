package com.example.meditation.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "meditation_goals")
data class MeditationGoal(
    @PrimaryKey
    val timerMinutes: Int,  // 1, 2, or 5 minutes
    val timesPerDay: Int,  // Daily goal for this timer
    val timestamp: Long = System.currentTimeMillis()  // When this goal was set
)

@Entity(tableName = "meditation_completions")
data class MeditationCompletion(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val timerMinutes: Int,  // Duration of the meditation (1, 2, or 5)
    val timestamp: Long,  // Unix timestamp of when the meditation was completed
    val date: String  // YYYY-MM-DD format for easier querying by day
) 