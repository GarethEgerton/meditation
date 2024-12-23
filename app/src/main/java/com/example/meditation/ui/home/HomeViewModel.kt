package com.example.meditation.ui.home

import android.os.CountDownTimer
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class HomeViewModel : ViewModel() {
    private var currentTimer: CountDownTimer? = null
    private var activeTimer: Int = 0
    private var remainingTimeMillis: Long = 0
    private var isPaused: Boolean = false
    
    private val _timerOneText = MutableLiveData<String>()
    private val _timerTwoText = MutableLiveData<String>()
    private val _timerFiveText = MutableLiveData<String>()
    private val _timerFinished = MutableLiveData<Boolean>()
    private val _timerState = MutableLiveData<TimerState>()
    private val _errorEvent = MutableLiveData<Int>()
    
    val timerOneText: LiveData<String> = _timerOneText
    val timerTwoText: LiveData<String> = _timerTwoText
    val timerFiveText: LiveData<String> = _timerFiveText
    val timerFinished: LiveData<Boolean> = _timerFinished
    val timerState: LiveData<TimerState> = _timerState
    val errorEvent: LiveData<Int> = _errorEvent

    init {
        resetTimers()
    }

    fun handleTimerClick(minutes: Int) {
        when {
            activeTimer != 0 && activeTimer != minutes && !isPaused -> {
                _errorEvent.value = minutes
            }
            activeTimer == 0 -> startTimer(minutes)
            activeTimer == minutes && !isPaused -> pauseTimer()
            activeTimer == minutes && isPaused -> resumeTimer()
            else -> startTimer(minutes)
        }
    }

    private fun startTimer(minutes: Int) {
        currentTimer?.cancel()
        activeTimer = minutes
        isPaused = false
        
        val milliseconds = minutes * 60 * 1000L
        remainingTimeMillis = milliseconds
        startCountdown(milliseconds)
        _timerState.value = TimerState(activeTimer, isPaused)
    }

    private fun pauseTimer() {
        currentTimer?.cancel()
        isPaused = true
        _timerState.value = TimerState(activeTimer, isPaused)
    }

    private fun resumeTimer() {
        isPaused = false
        startCountdown(remainingTimeMillis)
        _timerState.value = TimerState(activeTimer, isPaused)
    }

    private fun startCountdown(milliseconds: Long) {
        currentTimer = object : CountDownTimer(milliseconds, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                remainingTimeMillis = millisUntilFinished
                val minutes = millisUntilFinished / 1000 / 60
                val seconds = (millisUntilFinished / 1000) % 60
                val timeText = String.format("%d:%02d", minutes, seconds)
                
                when (activeTimer) {
                    1 -> _timerOneText.value = timeText
                    2 -> _timerTwoText.value = timeText
                    5 -> _timerFiveText.value = timeText
                }
            }

            override fun onFinish() {
                _timerFinished.value = true
                resetTimers()
            }
        }.start()
    }

    fun cancelTimer(minutes: Int) {
        if (activeTimer == minutes) {
            currentTimer?.cancel()
            resetTimers()
        }
    }

    private fun resetTimers() {
        _timerOneText.value = "1:00"
        _timerTwoText.value = "2:00"
        _timerFiveText.value = "5:00"
        _timerFinished.value = false
        _timerState.value = TimerState(0, false)
        activeTimer = 0
        isPaused = false
        remainingTimeMillis = 0
    }

    override fun onCleared() {
        super.onCleared()
        currentTimer?.cancel()
    }

    data class TimerState(val activeTimer: Int, val isPaused: Boolean)
}