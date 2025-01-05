package com.example.meditation.data

import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class MeditationRepository(private val meditationDao: MeditationDao) {
    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    // Goal operations
    fun getAllGoals(): Flow<List<MeditationGoal>> = meditationDao.getAllCurrentGoals()

    fun getGoalForTimer(minutes: Int): Flow<MeditationGoal?> = 
        meditationDao.getGoalForTimer(minutes)

    suspend fun getGoalsActiveAtTime(timestamp: Long): List<MeditationGoal> =
        meditationDao.getGoalsActiveAtTime(timestamp)

    suspend fun updateGoal(minutes: Int, timesPerDay: Int) {
        val goal = MeditationGoal(minutes, timesPerDay)
        println("Updating goal: $goal") // Debug log
        meditationDao.insertGoal(goal)
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

    suspend fun recordCustomCompletion(minutes: Int, actualDuration: Long) {
        val currentDate = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
        val completion = MeditationCompletion(
            timerMinutes = minutes,
            timestamp = System.currentTimeMillis(),
            date = currentDate,
            actualDuration = actualDuration
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

    suspend fun getCompletionCountForDate(date: LocalDate, minutes: Int): Int {
        val formattedDate = date.format(dateFormatter)
        return meditationDao.getCompletionCountForDateSync(formattedDate, minutes)
    }

    fun getAllCompletions(): Flow<List<MeditationCompletion>> = 
        meditationDao.getAllCompletions()

    suspend fun cleanupOldCompletions(daysToKeep: Int = 30) {
        val cutoffDate = LocalDate.now().minusDays(daysToKeep.toLong()).format(dateFormatter)
        meditationDao.deleteCompletionsBeforeDate(cutoffDate)
    }

    // Daily minutes goal operations
    fun getCurrentDailyMinutesGoal(): Flow<DailyMinutesGoal?> =
        meditationDao.getCurrentDailyMinutesGoal()

    suspend fun updateDailyMinutesGoal(targetMinutes: Int) {
        val goal = DailyMinutesGoal(targetMinutes = targetMinutes)
        meditationDao.insertDailyMinutesGoal(goal)
    }

    fun getTodayTotalMinutes(): Flow<Long> {
        val today = LocalDate.now().format(dateFormatter)
        return meditationDao.getTotalMinutesForDate(today)
    }

    fun getTotalMinutesForDate(date: LocalDate): Flow<Long> {
        val formattedDate = date.format(dateFormatter)
        return meditationDao.getTotalMinutesForDate(formattedDate)
    }
} 