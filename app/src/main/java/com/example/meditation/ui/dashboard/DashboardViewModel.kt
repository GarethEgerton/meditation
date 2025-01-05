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
    
    private val _oneMinGoal = MutableLiveData<Int>()
    val oneMinGoal: LiveData<Int> = _oneMinGoal

    private val _twoMinGoal = MutableLiveData<Int>()
    val twoMinGoal: LiveData<Int> = _twoMinGoal

    private val _fiveMinGoal = MutableLiveData<Int>()
    val fiveMinGoal: LiveData<Int> = _fiveMinGoal

    private val _dailyMinutesGoal = MutableLiveData<Int>()
    val dailyMinutesGoal: LiveData<Int> = _dailyMinutesGoal

    private val _todayTotalMinutes = MutableLiveData<Int>()
    val todayTotalMinutes: LiveData<Int> = _todayTotalMinutes

    init {
        val database = MeditationDatabase.getDatabase(application)
        repository = MeditationRepository(database.meditationDao())
        
        // Load all goals from database
        loadGoalsFromDatabase()
        loadDailyMinutesProgress()
    }

    private fun loadGoalsFromDatabase() {
        viewModelScope.launch {
            // Load daily minutes goal
            repository.getCurrentDailyMinutesGoal().collect { goal ->
                _dailyMinutesGoal.value = goal?.targetMinutes ?: 0
            }
        }

        viewModelScope.launch {
            // Load 1-minute goal
            repository.getGoalForTimer(1).collect { goal ->
                _oneMinGoal.value = goal?.timesPerDay ?: 0
            }
        }
        
        viewModelScope.launch {
            // Load 2-minute goal
            repository.getGoalForTimer(2).collect { goal ->
                _twoMinGoal.value = goal?.timesPerDay ?: 0
            }
        }
        
        viewModelScope.launch {
            // Load 5-minute goal
            repository.getGoalForTimer(5).collect { goal ->
                _fiveMinGoal.value = goal?.timesPerDay ?: 0
            }
        }
    }

    private fun loadDailyMinutesProgress() {
        viewModelScope.launch {
            repository.getTodayTotalMinutes().collect { totalSeconds ->
                _todayTotalMinutes.value = (totalSeconds / 60).toInt()
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

    fun updateOneMinGoal(value: Int?) {
        val goalValue = value ?: 0
        _oneMinGoal.value = goalValue
        viewModelScope.launch {
            repository.updateGoal(1, goalValue)
        }
    }

    fun updateTwoMinGoal(value: Int?) {
        val goalValue = value ?: 0
        _twoMinGoal.value = goalValue
        viewModelScope.launch {
            repository.updateGoal(2, goalValue)
        }
    }

    fun updateFiveMinGoal(value: Int?) {
        val goalValue = value ?: 0
        _fiveMinGoal.value = goalValue
        viewModelScope.launch {
            repository.updateGoal(5, goalValue)
        }
    }
}