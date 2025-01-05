package com.example.meditation.ui.dashboard

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.viewModelScope
import com.example.meditation.data.MeditationDatabase
import com.example.meditation.data.MeditationRepository
import com.example.meditation.data.ProgressManager
import kotlinx.coroutines.launch

class DashboardViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: MeditationRepository
    private val progressManager: ProgressManager

    val oneMinGoal: LiveData<Int>
    val twoMinGoal: LiveData<Int>
    val fiveMinGoal: LiveData<Int>

    private val _dailyMinutesGoal = MutableLiveData<Int>()
    val dailyMinutesGoal: LiveData<Int> = _dailyMinutesGoal

    private val _todayTotalMinutes = MutableLiveData<Int>()
    val todayTotalMinutes: LiveData<Int> = _todayTotalMinutes

    private val dailyProgressObserver = Observer<ProgressManager.DailyProgress> { progress ->
        _dailyMinutesGoal.value = progress.target
        _todayTotalMinutes.value = progress.completed
    }

    init {
        val database = MeditationDatabase.getDatabase(application)
        repository = MeditationRepository(database.meditationDao())
        progressManager = ProgressManager.getInstance(repository)

        // Get goals from ProgressManager
        oneMinGoal = progressManager.oneMinGoal
        twoMinGoal = progressManager.twoMinGoal
        fiveMinGoal = progressManager.fiveMinGoal

        // Observe daily progress and update the LiveData values
        progressManager.dailyProgress.observeForever(dailyProgressObserver)
    }

    override fun onCleared() {
        super.onCleared()
        progressManager.dailyProgress.removeObserver(dailyProgressObserver)
    }

    fun updateDailyMinutesGoal(value: Int?) {
        val goalValue = value ?: 0
        viewModelScope.launch {
            repository.updateDailyMinutesGoal(goalValue)
        }
    }

    fun updateTimerGoal(minutes: Int, value: Int?) {
        val goalValue = value ?: 0
        viewModelScope.launch {
            repository.updateGoal(minutes, goalValue)
        }
    }

    // Convenience methods to maintain backward compatibility
    fun updateOneMinGoal(value: Int?) = updateTimerGoal(1, value)
    fun updateTwoMinGoal(value: Int?) = updateTimerGoal(2, value)
    fun updateFiveMinGoal(value: Int?) = updateTimerGoal(5, value)
}