package com.example.meditation.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class MeditationRepository(private val meditationDao: MeditationDao) {
    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    // Helper functions
    private fun LocalDate.format() = format(dateFormatter)
    private fun getCurrentDate() = LocalDate.now().format()

    // Goal operations
    fun getAllGoals(): Flow<List<MeditationGoal>> = meditationDao.getAllCurrentGoals()

    fun getGoalForTimer(minutes: Int): Flow<MeditationGoal?> = 
        meditationDao.getGoalForTimer(minutes)

    suspend fun getGoalsActiveAtTime(timestamp: Long): List<MeditationGoal> =
        meditationDao.getGoalsActiveAtTime(timestamp)

    suspend fun updateGoal(minutes: Int, timesPerDay: Int) {
        val goal = MeditationGoal(minutes, timesPerDay)
        meditationDao.insertGoal(goal)
    }

    // Completion operations
    suspend fun recordCompletion(minutes: Int, actualDuration: Long? = null) {
        val completion = MeditationCompletion(
            timerMinutes = minutes,
            timestamp = System.currentTimeMillis(),
            date = getCurrentDate(),
            actualDuration = actualDuration
        )
        meditationDao.insertCompletion(completion)
    }

    suspend fun recordCustomCompletion(minutes: Int, actualDuration: Long) {
        recordCompletion(minutes, actualDuration)
    }

    fun getTodayCompletions(minutes: Int): Flow<List<MeditationCompletion>> =
        meditationDao.getCompletionsForDate(getCurrentDate(), minutes)

    fun getTodayCompletionCount(minutes: Int): Flow<Int> =
        meditationDao.getCompletionCountForDate(getCurrentDate(), minutes)

    suspend fun getCompletionCountForDate(date: LocalDate, minutes: Int): Int =
        meditationDao.getCompletionCountForDateSync(date.format(), minutes)

    fun getAllCompletions(): Flow<List<MeditationCompletion>> = 
        meditationDao.getAllCompletions()

    suspend fun cleanupOldCompletions(daysToKeep: Int = 30) {
        val cutoffDate = LocalDate.now().minusDays(daysToKeep.toLong()).format()
        meditationDao.deleteCompletionsBeforeDate(cutoffDate)
    }

    // Daily minutes goal operations
    fun getCurrentDailyMinutesGoal(): Flow<DailyMinutesGoal?> =
        meditationDao.getCurrentDailyMinutesGoal()

    suspend fun updateDailyMinutesGoal(targetMinutes: Int) {
        val goal = DailyMinutesGoal(targetMinutes = targetMinutes)
        meditationDao.insertDailyMinutesGoal(goal)
    }

    fun getTodayTotalMinutes(): Flow<Long> =
        meditationDao.getTotalMinutesForDate(getCurrentDate())
            .map { totalSeconds -> totalSeconds / 60 }

    fun getTotalMinutesForDate(date: LocalDate): Flow<Long> =
        meditationDao.getTotalMinutesForDate(date.format())
            .map { totalSeconds -> totalSeconds / 60 }
} 