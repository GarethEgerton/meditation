package com.example.meditation.ui.home

import android.app.Application
import androidx.lifecycle.*
import com.example.meditation.data.MeditationDatabase
import com.example.meditation.data.MeditationRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class HomeViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: MeditationRepository
    private var timerJob: Job? = null

    private val _timerState = MutableLiveData(TimerState(0, false))
    val timerState: LiveData<TimerState> = _timerState

    private val _timerOneText = MutableLiveData("1:00")
    val timerOneText: LiveData<String> = _timerOneText

    private val _timerTwoText = MutableLiveData("2:00")
    val timerTwoText: LiveData<String> = _timerTwoText

    private val _timerFiveText = MutableLiveData("5:00")
    val timerFiveText: LiveData<String> = _timerFiveText

    private val _timerFinished = MutableLiveData(false)
    val timerFinished: LiveData<Boolean> = _timerFinished

    private val _errorEvent = MutableLiveData<Int>()
    val errorEvent: LiveData<Int> = _errorEvent

    // Goals and completions from database
    val oneMinGoal: LiveData<Int>
    val twoMinGoal: LiveData<Int>
    val fiveMinGoal: LiveData<Int>

    val oneMinCompletions: LiveData<Int>
    val twoMinCompletions: LiveData<Int>
    val fiveMinCompletions: LiveData<Int>

    init {
        val database = MeditationDatabase.getDatabase(application)
        repository = MeditationRepository(database.meditationDao())

        // Initialize goals from database
        oneMinGoal = repository.getGoalForTimer(1)
            .map { it?.timesPerDay ?: 0 }
            .asLiveData()

        twoMinGoal = repository.getGoalForTimer(2)
            .map { it?.timesPerDay ?: 0 }
            .asLiveData()

        fiveMinGoal = repository.getGoalForTimer(5)
            .map { it?.timesPerDay ?: 0 }
            .asLiveData()

        // Initialize completions from database
        oneMinCompletions = repository.getTodayCompletionCount(1).asLiveData()
        twoMinCompletions = repository.getTodayCompletionCount(2).asLiveData()
        fiveMinCompletions = repository.getTodayCompletionCount(5).asLiveData()
    }

    fun handleTimerClick(minutes: Int) {
        val currentState = _timerState.value!!
        
        if (currentState.activeTimer == 0) {
            startTimer(minutes)
        } else if (currentState.activeTimer == minutes) {
            if (currentState.isPaused) {
                resumeTimer(minutes)
            } else {
                pauseTimer()
            }
        } else {
            _errorEvent.value = minutes
        }
    }

    private fun startTimer(minutes: Int) {
        _timerState.value = TimerState(minutes, false)
        startCountdown(minutes)
    }

    private fun resumeTimer(minutes: Int) {
        _timerState.value = TimerState(minutes, false)
        startCountdown(minutes)
    }

    private fun pauseTimer() {
        timerJob?.cancel()
        _timerState.value = _timerState.value?.copy(isPaused = true)
    }

    fun cancelTimer(minutes: Int) {
        if (_timerState.value?.activeTimer == minutes) {
            timerJob?.cancel()
            _timerState.value = TimerState(0, false)
            resetTimerText(minutes)
        }
    }

    private fun startCountdown(minutes: Int) {
        timerJob?.cancel()
        
        val totalSeconds = minutes * 60
        var remainingSeconds = totalSeconds

        timerJob = viewModelScope.launch {
            while (remainingSeconds > 0) {
                updateTimerText(minutes, remainingSeconds)
                delay(1000)
                remainingSeconds--
            }
            
            // Timer completed
            _timerState.value = TimerState(0, false)
            resetTimerText(minutes)
            _timerFinished.value = true
            _timerFinished.value = false

            // Record completion in database
            repository.recordCompletion(minutes)
        }
    }

    private fun updateTimerText(minutes: Int, remainingSeconds: Int) {
        val text = String.format("%d:%02d", remainingSeconds / 60, remainingSeconds % 60)
        when (minutes) {
            1 -> _timerOneText.value = text
            2 -> _timerTwoText.value = text
            5 -> _timerFiveText.value = text
        }
    }

    private fun resetTimerText(minutes: Int) {
        when (minutes) {
            1 -> _timerOneText.value = "1:00"
            2 -> _timerTwoText.value = "2:00"
            5 -> _timerFiveText.value = "5:00"
        }
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
    }

    data class TimerState(
        val activeTimer: Int,  // 0 for no timer, 1 for 1min, 2 for 2min, 5 for 5min
        val isPaused: Boolean
    )
}