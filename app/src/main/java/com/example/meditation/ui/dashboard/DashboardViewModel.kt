package com.example.meditation.ui.dashboard

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.meditation.data.MeditationDatabase
import com.example.meditation.data.MeditationRepository
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

class DashboardViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: MeditationRepository
    
    private val _timerGoals = mutableMapOf(
        1 to MutableLiveData<Int>(),
        2 to MutableLiveData<Int>(),
        5 to MutableLiveData<Int>()
    )
    val oneMinGoal: LiveData<Int> = _timerGoals[1]!!
    val twoMinGoal: LiveData<Int> = _timerGoals[2]!!
    val fiveMinGoal: LiveData<Int> = _timerGoals[5]!!

    private val _dailyMinutesGoal = MutableLiveData<Int>()
    val dailyMinutesGoal: LiveData<Int> = _dailyMinutesGoal

    private val _todayTotalMinutes = MutableLiveData<Int>()
    val todayTotalMinutes: LiveData<Int> = _todayTotalMinutes

    init {
        val database = MeditationDatabase.getDatabase(application)
        repository = MeditationRepository(database.meditationDao())
        
        loadGoalsFromDatabase()
        loadDailyMinutesProgress()
    }

    private fun loadGoalsFromDatabase() {
        // Load daily minutes goal
        viewModelScope.launch {
            repository.getCurrentDailyMinutesGoal().collect { goal ->
                _dailyMinutesGoal.value = goal?.targetMinutes ?: 0
            }
        }

        // Load timer goals
        _timerGoals.keys.forEach { minutes ->
            viewModelScope.launch {
                repository.getGoalForTimer(minutes).collect { goal ->
                    _timerGoals[minutes]?.value = goal?.timesPerDay ?: 0
                }
            }
        }
    }

    private fun loadDailyMinutesProgress() {
        viewModelScope.launch {
            repository.getTodayTotalMinutes().collect { minutes ->
                _todayTotalMinutes.value = minutes.toInt()
            }
        }
    }

    fun updateDailyMinutesGoal(value: Int?) {
        val goalValue = value ?: 0
        _dailyMinutesGoal.value = goalValue
        viewModelScope.launch {
            repository.updateDailyMinutesGoal(goalValue)
        }
    }

    fun updateTimerGoal(minutes: Int, value: Int?) {
        val goalValue = value ?: 0
        _timerGoals[minutes]?.value = goalValue
        viewModelScope.launch {
            repository.updateGoal(minutes, goalValue)
        }
    }

    // Convenience methods to maintain backward compatibility
    fun updateOneMinGoal(value: Int?) = updateTimerGoal(1, value)
    fun updateTwoMinGoal(value: Int?) = updateTimerGoal(2, value)
    fun updateFiveMinGoal(value: Int?) = updateTimerGoal(5, value)
}