package com.example.meditation.data

import com.example.meditation.core.result.Result
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

interface IMeditationRepository {
    // Goal operations
    fun getAllGoals(): Flow<Result<List<MeditationGoal>>>
    fun getGoalForTimer(minutes: Int): Flow<Result<MeditationGoal?>>
    suspend fun getGoalsActiveAtTime(timestamp: Long): Result<List<MeditationGoal>>
    suspend fun updateGoal(minutes: Int, timesPerDay: Int): Result<Unit>
    
    // Completion operations
    suspend fun recordCompletion(minutes: Int, actualDuration: Long? = null): Result<Unit>
    fun getTodayCompletions(minutes: Int): Flow<Result<List<MeditationCompletion>>>
    fun getTodayCompletionCount(minutes: Int): Flow<Result<Int>>
    suspend fun cleanupOldCompletions(daysToKeep: Int): Result<Unit>
    
    // Daily minutes operations
    fun getCurrentDailyMinutesGoal(): Flow<Result<DailyMinutesGoal?>>
    suspend fun updateDailyMinutesGoal(targetMinutes: Int): Result<Unit>
    fun getTodayTotalMinutes(): Flow<Result<Long>>
    fun getTotalMinutesForDate(date: LocalDate): Flow<Result<Long>>
} 