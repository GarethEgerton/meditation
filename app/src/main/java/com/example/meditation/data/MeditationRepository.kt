package com.example.meditation.data

import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class MeditationRepository(private val meditationDao: MeditationDao) {
    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    // Goal operations
    fun getAllGoals(): Flow<List<MeditationGoal>> = meditationDao.getAllGoals()

    fun getGoalForTimer(minutes: Int): Flow<MeditationGoal?> = 
        meditationDao.getGoalForTimer(minutes)

    suspend fun getGoalsActiveAtTime(timestamp: Long): List<MeditationGoal> =
        meditationDao.getGoalsActiveAtTime(timestamp)

    suspend fun updateGoal(minutes: Int, timesPerDay: Int) {
        meditationDao.insertGoal(MeditationGoal(minutes, timesPerDay))
    }

    // Completion operations
    suspend fun recordCompletion(minutes: Int) {
        val now = System.currentTimeMillis()
        val today = LocalDate.now().format(dateFormatter)
        val completion = MeditationCompletion(
            timerMinutes = minutes,
            timestamp = now,
            date = today
        )
        meditationDao.insertCompletion(completion)
    }

    fun getTodayCompletions(minutes: Int): Flow<List<MeditationCompletion>> {
        val today = LocalDate.now().format(dateFormatter)
        return meditationDao.getCompletionsForDate(today, minutes)
    }

    fun getTodayCompletionCount(minutes: Int): Flow<Int> {
        val today = LocalDate.now().format(dateFormatter)
        return meditationDao.getCompletionCountForDate(today, minutes)
    }

    fun getAllCompletions(): Flow<List<MeditationCompletion>> = 
        meditationDao.getAllCompletions()

    suspend fun cleanupOldCompletions(daysToKeep: Int = 30) {
        val cutoffDate = LocalDate.now().minusDays(daysToKeep.toLong()).format(dateFormatter)
        meditationDao.deleteCompletionsBeforeDate(cutoffDate)
    }
} 