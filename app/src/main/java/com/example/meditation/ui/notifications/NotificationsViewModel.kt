package com.example.meditation.ui.notifications

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.meditation.data.MeditationCompletion
import com.example.meditation.data.MeditationDatabase
import com.example.meditation.data.MeditationRepository
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class NotificationsViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: MeditationRepository
    private val _statsRows = MutableLiveData<List<StatsRow>>()
    val statsRows: LiveData<List<StatsRow>> = _statsRows

    init {
        val database = MeditationDatabase.getDatabase(application)
        repository = MeditationRepository(database.meditationDao())
        loadStats()
    }

    private fun loadStats() {
        viewModelScope.launch {
            // Combine goals and completions flows to update whenever either changes
            repository.getAllGoals().combine(repository.getAllCompletions()) { goals, completions ->
                println("Current goals: $goals") // Debug log
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
                    val date = LocalDate.parse(completion.date)
                    if (date >= twoWeeksAgo && date <= today) {
                        statsMap[date]?.add(completion)
                    }
                }

                // Convert to StatsRow objects
                statsMap.entries
                    .sortedByDescending { it.key }
                    .map { (date, dayCompletions) ->
                        // Get goals that were active at the end of this day
                        val endOfDay = date.plusDays(1).atStartOfDay().minusNanos(1)
                        val timestamp = endOfDay.toInstant(java.time.ZoneOffset.UTC).toEpochMilli()
                        println("Processing date: $date, timestamp: $timestamp") // Debug log
                        
                        // For today, use current goals instead of historical ones
                        val historicalGoals = if (date == today) {
                            println("Using current goals for today: $goals") // Debug log
                            goals
                        } else {
                            val activeGoals = repository.getGoalsActiveAtTime(timestamp)
                            println("Historical goals for $date: $activeGoals") // Debug log
                            activeGoals
                        }

                        // Calculate completions for each timer
                        val oneMinCompletions = dayCompletions.count { it.timerMinutes == 1 }
                        val twoMinCompletions = dayCompletions.count { it.timerMinutes == 2 }
                        val fiveMinCompletions = dayCompletions.count { it.timerMinutes == 5 }

                        // Get goals for each timer
                        val oneMinGoal = historicalGoals.find { it.timerMinutes == 1 }?.timesPerDay ?: 0
                        val twoMinGoal = historicalGoals.find { it.timerMinutes == 2 }?.timesPerDay ?: 0
                        val fiveMinGoal = historicalGoals.find { it.timerMinutes == 5 }?.timesPerDay ?: 0

                        println("Goals for $date: 1min=$oneMinGoal, 2min=$twoMinGoal, 5min=$fiveMinGoal") // Debug log

                        // Check if all goals were met for this day
                        val isGoalCompleted = historicalGoals.all { goal ->
                            val completionsForTimer = dayCompletions.count { it.timerMinutes == goal.timerMinutes }
                            completionsForTimer >= goal.timesPerDay
                        }

                        StatsRow(
                            date = date,
                            totalMinutes = dayCompletions.sumOf { it.timerMinutes },
                            totalSessions = dayCompletions.size,
                            isGoalCompleted = isGoalCompleted && historicalGoals.isNotEmpty(),
                            oneMinGoal = oneMinGoal,
                            twoMinGoal = twoMinGoal,
                            fiveMinGoal = fiveMinGoal,
                            oneMinCompletions = oneMinCompletions,
                            twoMinCompletions = twoMinCompletions,
                            fiveMinCompletions = fiveMinCompletions
                        )
                    }
            }.collectLatest { stats ->
                _statsRows.value = stats
            }
        }
    }
}