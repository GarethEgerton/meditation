package com.example.meditation.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface MeditationDao {
    // Goal operations
    @Query("SELECT * FROM meditation_goals")
    fun getAllGoals(): Flow<List<MeditationGoal>>

    @Query("SELECT * FROM meditation_goals WHERE timerMinutes = :minutes")
    fun getGoalForTimer(minutes: Int): Flow<MeditationGoal?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGoal(goal: MeditationGoal)

    // Completion operations
    @Insert
    suspend fun insertCompletion(completion: MeditationCompletion)

    @Query("SELECT * FROM meditation_completions WHERE date = :date AND timerMinutes = :minutes")
    fun getCompletionsForDate(date: String, minutes: Int): Flow<List<MeditationCompletion>>

    @Query("SELECT COUNT(*) FROM meditation_completions WHERE date = :date AND timerMinutes = :minutes")
    fun getCompletionCountForDate(date: String, minutes: Int): Flow<Int>

    @Query("SELECT * FROM meditation_completions ORDER BY timestamp DESC")
    fun getAllCompletions(): Flow<List<MeditationCompletion>>

    @Query("DELETE FROM meditation_completions WHERE date < :date")
    suspend fun deleteCompletionsBeforeDate(date: String)
} 