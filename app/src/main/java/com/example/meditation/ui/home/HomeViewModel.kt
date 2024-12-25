package com.example.meditation.ui.home

import android.app.Application
import androidx.lifecycle.*
import com.example.meditation.data.MeditationDatabase
import com.example.meditation.data.MeditationRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.time.LocalDate

class HomeViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: MeditationRepository
    private var timerJob: Job? = null
    private var remainingSeconds: Int = 0

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

    data class CompletionState(
        val count: Int,
        val isToday: Boolean
    )

    private val _oneMinCompletions = MutableLiveData<CompletionState>()
    private val _twoMinCompletions = MutableLiveData<CompletionState>()
    private val _fiveMinCompletions = MutableLiveData<CompletionState>()

    val oneMinCompletions: LiveData<CompletionState> = _oneMinCompletions
    val twoMinCompletions: LiveData<CompletionState> = _twoMinCompletions
    val fiveMinCompletions: LiveData<CompletionState> = _fiveMinCompletions

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

        // Initialize completions from database with today flag
        viewModelScope.launch {
            // Get today's date
            val today = LocalDate.now()
            val yesterday = today.minusDays(1)

            // First check yesterday's completions
            val yesterdayOneMin = repository.getCompletionCountForDate(yesterday, 1)
            val yesterdayTwoMin = repository.getCompletionCountForDate(yesterday, 2)
            val yesterdayFiveMin = repository.getCompletionCountForDate(yesterday, 5)

            // If there are any completions from yesterday, show those first
            if (yesterdayOneMin > 0 || yesterdayTwoMin > 0 || yesterdayFiveMin > 0) {
                _oneMinCompletions.value = CompletionState(yesterdayOneMin, false)
                _twoMinCompletions.value = CompletionState(yesterdayTwoMin, false)
                _fiveMinCompletions.value = CompletionState(yesterdayFiveMin, false)
            }

            // Then start observing today's completions
            repository.getTodayCompletionCount(1).collect { count ->
                _oneMinCompletions.value = CompletionState(count, true)
            }
        }

        viewModelScope.launch {
            repository.getTodayCompletionCount(2).collect { count ->
                _twoMinCompletions.value = CompletionState(count, true)
            }
        }

        viewModelScope.launch {
            repository.getTodayCompletionCount(5).collect { count ->
                _fiveMinCompletions.value = CompletionState(count, true)
            }
        }
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
        remainingSeconds = minutes * 60
        startCountdown(minutes)
    }

    private fun resumeTimer(minutes: Int) {
        _timerState.value = TimerState(minutes, false)
        startCountdown(minutes, remainingSeconds)
    }

    private fun pauseTimer() {
        timerJob?.cancel()
        _timerState.value = _timerState.value?.copy(isPaused = true)
    }

    fun cancelTimer(minutes: Int) {
        if (_timerState.value?.activeTimer == minutes) {
            timerJob?.cancel()
            _timerState.value = TimerState(0, false)
            remainingSeconds = 0
            resetTimerText(minutes)
        }
    }

    private fun startCountdown(minutes: Int, startFromSeconds: Int = minutes * 60) {
        timerJob?.cancel()
        
        remainingSeconds = startFromSeconds

        timerJob = viewModelScope.launch {
            while (remainingSeconds > 0) {
                updateTimerText(minutes, remainingSeconds)
                delay(1000)
                remainingSeconds--
            }
            
            // Timer completed
            _timerState.value = TimerState(0, false)
            remainingSeconds = 0
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