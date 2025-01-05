package com.example.meditation.data

import androidx.lifecycle.LiveData
import androidx.lifecycle.asLiveData
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

class ProgressManager(private val repository: MeditationRepository) {
    
    data class DailyProgress(
        val completed: Int = 0,
        val target: Int = 30  // Default target of 30 minutes
    )

    // Get daily progress as LiveData
    val dailyProgress: LiveData<DailyProgress> = repository.getCurrentDailyMinutesGoal()
        .combine(repository.getTodayTotalMinutes()) { goal, totalMinutes ->
            DailyProgress(
                completed = totalMinutes.toInt(),
                target = goal?.targetMinutes ?: 30
            )
        }
        .asLiveData()

    // Get individual timer goals
    val oneMinGoal: LiveData<Int> = repository.getGoalForTimer(1)
        .map { it?.timesPerDay ?: 0 }
        .asLiveData()

    val twoMinGoal: LiveData<Int> = repository.getGoalForTimer(2)
        .map { it?.timesPerDay ?: 0 }
        .asLiveData()

    val fiveMinGoal: LiveData<Int> = repository.getGoalForTimer(5)
        .map { it?.timesPerDay ?: 0 }
        .asLiveData()

    companion object {
        @Volatile
        private var INSTANCE: ProgressManager? = null

        fun getInstance(repository: MeditationRepository): ProgressManager {
            return INSTANCE ?: synchronized(this) {
                val instance = ProgressManager(repository)
                INSTANCE = instance
                instance
            }
        }
    }
} 