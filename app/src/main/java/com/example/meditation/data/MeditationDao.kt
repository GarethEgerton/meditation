package com.example.meditation.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface MeditationDao {
    // Goal operations
    @Query("""
        SELECT mg.* FROM meditation_goals mg
        INNER JOIN (
            SELECT timerMinutes, MAX(timestamp) as max_timestamp
            FROM meditation_goals
            GROUP BY timerMinutes
        ) latest ON mg.timerMinutes = latest.timerMinutes 
        AND mg.timestamp = latest.max_timestamp
    """)
    fun getAllCurrentGoals(): Flow<List<MeditationGoal>>

    @Query("SELECT * FROM meditation_goals")
    fun getAllGoals(): Flow<List<MeditationGoal>>

    @Query("""
        SELECT * FROM meditation_goals 
        WHERE timerMinutes = :minutes 
        ORDER BY timestamp DESC 
        LIMIT 1
    """)
    fun getGoalForTimer(minutes: Int): Flow<MeditationGoal?>

    @Query("""
        SELECT mg.* FROM meditation_goals mg
        INNER JOIN (
            SELECT timerMinutes, MAX(timestamp) as max_timestamp
            FROM meditation_goals
            WHERE timestamp <= :timestamp
            GROUP BY timerMinutes
        ) latest ON mg.timerMinutes = latest.timerMinutes 
        AND mg.timestamp = latest.max_timestamp
    """)
    suspend fun getGoalsActiveAtTime(timestamp: Long): List<MeditationGoal>

    @Insert
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

    @Query("SELECT COUNT(*) FROM meditation_completions WHERE date = :date AND timerMinutes = :minutes")
    suspend fun getCompletionCountForDateSync(date: String, minutes: Int): Int

    // Daily minutes goal operations
    @Query("""
        SELECT * FROM daily_minutes_goals 
        ORDER BY timestamp DESC 
        LIMIT 1
    """)
    fun getCurrentDailyMinutesGoal(): Flow<DailyMinutesGoal?>

    @Insert
    suspend fun insertDailyMinutesGoal(goal: DailyMinutesGoal)

    @Query("""
        SELECT COALESCE(SUM(
            CASE 
                WHEN actualDuration IS NOT NULL THEN actualDuration 
                ELSE timerMinutes * 60 
            END
        ), 0) 
        FROM meditation_completions 
        WHERE date = :date
    """)
    fun getTotalMinutesForDate(date: String): Flow<Long>
} 