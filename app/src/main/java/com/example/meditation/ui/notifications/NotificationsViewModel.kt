package com.example.meditation.ui.notifications

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.meditation.core.result.getOrDefault
import com.example.meditation.core.result.unwrapOrDefault
import com.example.meditation.data.*
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.time.LocalDate

class NotificationsViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: MeditationRepository
    private val _statsRows = MutableLiveData<List<StatsRow>>()
    val statsRows: LiveData<List<StatsRow>> = _statsRows
    
    private val _isShowingMinutes = MutableLiveData(true)
    val isShowingMinutes: LiveData<Boolean> = _isShowingMinutes

    init {
        val database = MeditationDatabase.getDatabase(application)
        repository = MeditationRepository(database.meditationDao())
        loadStats()
    }

    fun setDisplayMode(showMinutes: Boolean) {
        if (_isShowingMinutes.value != showMinutes) {
            _isShowingMinutes.value = showMinutes
        }
    }

    private fun loadStats() {
        viewModelScope.launch {
            // Combine goals and completions flows to update whenever either changes
            repository.getAllGoals().combine(repository.getAllCompletions()) { goalsResult, completionsResult ->
                val goals = goalsResult.getOrDefault(emptyList())
                val completions = completionsResult.getOrDefault(emptyList())
                
                val today = LocalDate.now()
                val twoWeeksAgo = today.minusDays(13) // 14 days including today

                val statsMap = mutableMapOf<LocalDate, MutableList<MeditationCompletion>>()
                
                // Initialize the map with all dates
                for (i in 0..13) {
                    val date = today.minusDays(i.toLong())
                    statsMap[date] = mutableListOf()
                }

                // Group completions by date
                completions.forEach { completion ->
                    try {
                        val date = LocalDate.parse(completion.date)
                        if (date >= twoWeeksAgo && date <= today) {
                            statsMap[date]?.add(completion)
                            android.util.Log.d("NotificationsViewModel", 
                                "Found completion: date=${completion.date}, minutes=${completion.timerMinutes}, " +
                                "actualDuration=${completion.actualDuration}")
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("NotificationsViewModel", 
                            "Error parsing date ${completion.date}", e)
                    }
                }

                // Convert to StatsRow objects
                statsMap.entries
                    .sortedByDescending { it.key }
                    .map { (date, dayCompletions) ->
                        // For today, use current goals instead of historical ones
                        val historicalGoals = if (date == today) {
                            goals
                        } else {
                            repository.getGoalsActiveAtTime(date.atStartOfDay().toEpochSecond(java.time.ZoneOffset.UTC) * 1000)
                                .getOrDefault(emptyList())
                        }

                        // Calculate completions for each timer
                        val oneMinCompletions = dayCompletions.count { it.timerMinutes == 1 }
                        val twoMinCompletions = dayCompletions.count { it.timerMinutes == 2 }
                        val fiveMinCompletions = dayCompletions.count { it.timerMinutes == 5 }
                        val infiniteCompletions = dayCompletions.count { it.timerMinutes == -1 }
                        val totalSessions = oneMinCompletions + twoMinCompletions + fiveMinCompletions + infiniteCompletions

                        // Get goals for each timer
                        val oneMinGoal = historicalGoals.find { it.timerMinutes == 1 }?.timesPerDay ?: 0
                        val twoMinGoal = historicalGoals.find { it.timerMinutes == 2 }?.timesPerDay ?: 0
                        val fiveMinGoal = historicalGoals.find { it.timerMinutes == 5 }?.timesPerDay ?: 0

                        // Calculate total minutes including infinite sessions
                        val totalMinutes = dayCompletions.sumOf { completion ->
                            if (completion.timerMinutes == -1) {
                                // For infinite sessions, use actual duration
                                (completion.actualDuration ?: 0L) / 60L
                            } else {
                                completion.timerMinutes.toLong()
                            }
                        }

                        // Get daily minutes goal for this date
                        val dailyMinutesGoal = if (date == today) {
                            repository.getCurrentDailyMinutesGoal().first().getOrDefault(null)?.targetMinutes ?: 0
                        } else {
                            0 // For now, we don't track historical daily minutes goals
                        }

                        // Calculate total minutes goal (use the larger of timer goals sum and daily minutes goal)
                        val timerMinutesGoal = historicalGoals.sumOf { goal ->
                            goal.timerMinutes * goal.timesPerDay
                        }
                        val totalMinutesGoal = maxOf(timerMinutesGoal, dailyMinutesGoal)

                        // Calculate total sessions goal
                        val totalSessionsGoal = historicalGoals.sumOf { it.timesPerDay }

                        // Check if all goals were met for this day
                        val isGoalCompleted = historicalGoals.all { goal ->
                            val completionsForTimer = dayCompletions.count { it.timerMinutes == goal.timerMinutes }
                            completionsForTimer >= goal.timesPerDay
                        } && (dailyMinutesGoal == 0 || totalMinutes >= dailyMinutesGoal)

                        StatsRow(
                            date = date,
                            totalMinutes = totalMinutes.toInt(),
                            totalMinutesGoal = totalMinutesGoal,
                            totalSessions = totalSessions,
                            totalSessionsGoal = totalSessionsGoal,
                            isGoalCompleted = isGoalCompleted && historicalGoals.isNotEmpty(),
                            oneMinGoal = oneMinGoal,
                            twoMinGoal = twoMinGoal,
                            fiveMinGoal = fiveMinGoal,
                            oneMinCompletions = oneMinCompletions,
                            twoMinCompletions = twoMinCompletions,
                            fiveMinCompletions = fiveMinCompletions
                        )
                    }
            }.collect { stats ->
                _statsRows.value = stats
            }
        }
    }
}