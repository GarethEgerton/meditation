package com.example.meditation.data

import com.example.meditation.core.result.MeditationError
import com.example.meditation.core.result.Result
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class MeditationRepository(private val meditationDao: MeditationDao) : IMeditationRepository {
    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    // Helper functions
    private fun LocalDate.format() = format(dateFormatter)
    private fun getCurrentDate() = LocalDate.now().format()

    private suspend fun <T> wrapDatabaseCall(
        operation: String,
        block: suspend () -> T
    ): Result<T> = try {
        Result.success(block())
    } catch (e: Exception) {
        Result.error(MeditationError.DatabaseError(operation, e))
    }
    
    private fun <T> Flow<T>.wrapWithResult(operation: String): Flow<Result<T>> = 
        this.map { Result.success(it) }
            .catch { e -> 
                emit(Result.error(MeditationError.DatabaseError(operation, e)))
            }

    // Goal operations
    override fun getAllGoals(): Flow<Result<List<MeditationGoal>>> =
        meditationDao.getAllCurrentGoals().wrapWithResult("getAllGoals")

    override fun getGoalForTimer(minutes: Int): Flow<Result<MeditationGoal?>> =
        meditationDao.getGoalForTimer(minutes).wrapWithResult("getGoalForTimer")

    override suspend fun getGoalsActiveAtTime(timestamp: Long): Result<List<MeditationGoal>> =
        wrapDatabaseCall("getGoalsActiveAtTime") {
            meditationDao.getGoalsActiveAtTime(timestamp)
        }

    override suspend fun updateGoal(minutes: Int, timesPerDay: Int): Result<Unit> =
        wrapDatabaseCall("updateGoal") {
            val goal = MeditationGoal(minutes, timesPerDay)
            meditationDao.insertGoal(goal)
        }

    // Completion operations
    override suspend fun recordCompletion(minutes: Int, actualDuration: Long?): Result<Unit> =
        wrapDatabaseCall("recordCompletion") {
            val completion = MeditationCompletion(
                timerMinutes = minutes,
                timestamp = System.currentTimeMillis(),
                date = getCurrentDate(),
                actualDuration = actualDuration
            )
            meditationDao.insertCompletion(completion)
        }

    override fun getTodayCompletions(minutes: Int): Flow<Result<List<MeditationCompletion>>> =
        meditationDao.getCompletionsForDate(getCurrentDate(), minutes)
            .wrapWithResult("getTodayCompletions")

    override fun getTodayCompletionCount(minutes: Int): Flow<Result<Int>> =
        meditationDao.getCompletionCountForDate(getCurrentDate(), minutes)
            .wrapWithResult("getTodayCompletionCount")

    override suspend fun cleanupOldCompletions(daysToKeep: Int): Result<Unit> =
        wrapDatabaseCall("cleanupOldCompletions") {
            val cutoffDate = LocalDate.now().minusDays(daysToKeep.toLong()).format()
            meditationDao.deleteCompletionsBeforeDate(cutoffDate)
        }

    // Daily minutes goal operations
    override fun getCurrentDailyMinutesGoal(): Flow<Result<DailyMinutesGoal?>> =
        meditationDao.getCurrentDailyMinutesGoal().wrapWithResult("getCurrentDailyMinutesGoal")

    override suspend fun updateDailyMinutesGoal(targetMinutes: Int): Result<Unit> =
        wrapDatabaseCall("updateDailyMinutesGoal") {
            val goal = DailyMinutesGoal(targetMinutes = targetMinutes)
            meditationDao.insertDailyMinutesGoal(goal)
        }

    override fun getTodayTotalMinutes(): Flow<Result<Long>> =
        meditationDao.getTotalMinutesForDate(getCurrentDate())
            .map { totalSeconds -> totalSeconds / 60 }
            .wrapWithResult("getTodayTotalMinutes")

    override fun getTotalMinutesForDate(date: LocalDate): Flow<Result<Long>> =
        meditationDao.getTotalMinutesForDate(date.format())
            .map { totalSeconds -> totalSeconds / 60 }
            .wrapWithResult("getTotalMinutesForDate")
} 