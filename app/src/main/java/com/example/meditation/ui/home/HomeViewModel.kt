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
import java.time.format.DateTimeFormatter

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

    private val _timerCustomText = MutableLiveData("∞")
    val timerCustomText: LiveData<String> = _timerCustomText

    private val _timerFinished = MutableLiveData(false)
    val timerFinished: LiveData<Boolean> = _timerFinished

    private val _errorEvent = MutableLiveData<Int>()
    val errorEvent: LiveData<Int> = _errorEvent

    private var customTimerMinutes: Int = 0
    private var isCustomTimerInfinite: Boolean = true
    private var customTimerStartTime: Long = 0

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

    private var customTimerPausedAt: Long = 0

    private val _customCompletions = MutableLiveData<CompletionState>()
    val customCompletions: LiveData<CompletionState> = _customCompletions

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
            val yesterdayCustom = repository.getCompletionCountForDate(yesterday, -1)

            // If there are any completions from yesterday, show those first
            if (yesterdayOneMin > 0 || yesterdayTwoMin > 0 || yesterdayFiveMin > 0 || yesterdayCustom > 0) {
                _oneMinCompletions.value = CompletionState(yesterdayOneMin, false)
                _twoMinCompletions.value = CompletionState(yesterdayTwoMin, false)
                _fiveMinCompletions.value = CompletionState(yesterdayFiveMin, false)
                _customCompletions.value = CompletionState(yesterdayCustom, false)
            }

            // Start observing today's completions for all timers
            launch {
                repository.getTodayCompletionCount(1).collect { count ->
                    _oneMinCompletions.value = CompletionState(count, true)
                }
            }

            launch {
                repository.getTodayCompletionCount(2).collect { count ->
                    _twoMinCompletions.value = CompletionState(count, true)
                }
            }

            launch {
                repository.getTodayCompletionCount(5).collect { count ->
                    _fiveMinCompletions.value = CompletionState(count, true)
                }
            }

            launch {
                repository.getTodayCompletionCount(-1).collect { count ->
                    _customCompletions.value = CompletionState(count, true)
                }
            }
        }
    }

    data class TimerState(
        val activeTimer: Int,  // 0 for no timer, 1 for 1min, 2 for 2min, 5 for 5min, -1 for custom
        val isPaused: Boolean,
        val isInfinite: Boolean = false
    )

    fun updateCustomTimer(hours: Int, minutes: Int, isInfinite: Boolean) {
        isCustomTimerInfinite = isInfinite
        customTimerMinutes = hours * 60 + minutes

        if (isInfinite) {
            _timerCustomText.value = "∞"
        } else if (customTimerMinutes > 0) {
            val text = if (hours > 0) {
                String.format("%d:%02d:00", hours, minutes)
            } else {
                String.format("%d:00", minutes)
            }
            _timerCustomText.value = text
        }
    }

    fun handleTimerClick(minutes: Int) {
        val currentState = _timerState.value!!
        
        if (currentState.activeTimer == 0) {
            if (minutes == -1) {  // Custom timer
                if (isCustomTimerInfinite || customTimerMinutes > 0) {
                    startTimer(minutes)
                }
            } else {
                startTimer(minutes)
            }
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
        if (minutes == -1) {  // Custom timer
            _timerState.value = TimerState(-1, false, isCustomTimerInfinite)
            if (isCustomTimerInfinite) {
                customTimerStartTime = System.currentTimeMillis()
                startInfiniteTimer()
            } else {
                remainingSeconds = customTimerMinutes * 60
                startCountdown(-1)
            }
        } else {
            _timerState.value = TimerState(minutes, false)
            remainingSeconds = minutes * 60
            startCountdown(minutes)
        }
    }

    private fun startInfiniteTimer() {
        timerJob?.cancel()
        
        timerJob = viewModelScope.launch {
            while (true) {
                val elapsedSeconds = (System.currentTimeMillis() - customTimerStartTime) / 1000
                val hours = elapsedSeconds / 3600
                val minutes = (elapsedSeconds % 3600) / 60
                val seconds = elapsedSeconds % 60
                
                _timerCustomText.value = if (hours > 0) {
                    String.format("%d:%02d:%02d", hours, minutes, seconds)
                } else {
                    String.format("%d:%02d", minutes, seconds)
                }
                
                delay(1000)
            }
        }
    }

    private fun resumeTimer(minutes: Int) {
        if (minutes == -1) {  // Custom timer
            _timerState.value = TimerState(-1, false, isCustomTimerInfinite)
            if (isCustomTimerInfinite) {
                customTimerStartTime = System.currentTimeMillis() - ((System.currentTimeMillis() - customTimerStartTime) / 1000) * 1000
                startInfiniteTimer()
            } else {
                startCountdown(-1, remainingSeconds)
            }
        } else {
            _timerState.value = TimerState(minutes, false)
            startCountdown(minutes, remainingSeconds)
        }
    }

    private fun pauseTimer() {
        timerJob?.cancel()
        if (_timerState.value?.activeTimer == -1) {
            customTimerPausedAt = System.currentTimeMillis()
        }
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

    private fun startCountdown(minutes: Int, startFromSeconds: Int = if (minutes == -1) customTimerMinutes * 60 else minutes * 60) {
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
            if (minutes != -1) {
                repository.recordCompletion(minutes)
            } else if (!isCustomTimerInfinite && customTimerMinutes > 0) {
                repository.recordCompletion(customTimerMinutes)
                // Reset timer text to show the custom duration again
                val hours = customTimerMinutes / 60
                val mins = customTimerMinutes % 60
                _timerCustomText.value = if (hours > 0) {
                    String.format("%d:%02d:00", hours, mins)
                } else {
                    String.format("%d:00", mins)
                }
            }
        }
    }

    private fun updateTimerText(minutes: Int, remainingSeconds: Int) {
        if (minutes == -1) {  // Custom timer
            val hours = remainingSeconds / 3600
            val mins = (remainingSeconds % 3600) / 60
            val secs = remainingSeconds % 60
            
            _timerCustomText.value = if (hours > 0) {
                String.format("%d:%02d:%02d", hours, mins, secs)
            } else {
                String.format("%d:%02d", mins, secs)
            }
        } else {
            val text = String.format("%d:%02d", remainingSeconds / 60, remainingSeconds % 60)
            when (minutes) {
                1 -> _timerOneText.value = text
                2 -> _timerTwoText.value = text
                5 -> _timerFiveText.value = text
            }
        }
    }

    private fun resetTimerText(minutes: Int) {
        when (minutes) {
            1 -> _timerOneText.value = "1:00"
            2 -> _timerTwoText.value = "2:00"
            5 -> _timerFiveText.value = "5:00"
            -1 -> _timerCustomText.value = if (isCustomTimerInfinite) "∞" else {
                val hours = customTimerMinutes / 60
                val mins = customTimerMinutes % 60
                if (hours > 0) {
                    String.format("%d:%02d:00", hours, mins)
                } else {
                    String.format("%d:00", mins)
                }
            }
        }
    }

    fun completeCustomTimer() {
        val currentState = _timerState.value!!
        if (currentState.activeTimer == -1 && currentState.isPaused) {
            // Calculate actual duration based on elapsed time for both infinite and fixed timers
            val actualDuration = (customTimerPausedAt - customTimerStartTime) / 1000L

            viewModelScope.launch {
                repository.recordCustomCompletion(
                    minutes = if (isCustomTimerInfinite) -1 else customTimerMinutes,
                    actualDuration = actualDuration
                )
            }

            // Reset timer state
            _timerState.value = TimerState(0, false)
            remainingSeconds = 0
            resetTimerText(-1)
            
            // Trigger completion bell
            _timerFinished.value = true
            _timerFinished.value = false
        }
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
    }
}