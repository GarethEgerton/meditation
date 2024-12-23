package com.example.meditation.ui.home

import android.os.CountDownTimer
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class HomeViewModel : ViewModel() {
    private var currentTimer: CountDownTimer? = null
    private var activeTimer: Int = 0
    
    private val _timerOneText = MutableLiveData<String>()
    private val _timerTwoText = MutableLiveData<String>()
    private val _timerFiveText = MutableLiveData<String>()
    private val _timerFinished = MutableLiveData<Boolean>()
    
    val timerOneText: LiveData<String> = _timerOneText
    val timerTwoText: LiveData<String> = _timerTwoText
    val timerFiveText: LiveData<String> = _timerFiveText
    val timerFinished: LiveData<Boolean> = _timerFinished

    init {
        resetTimers()
    }

    fun startTimer(minutes: Int) {
        currentTimer?.cancel()
        activeTimer = minutes
        
        val milliseconds = minutes * 60 * 1000L
        currentTimer = object : CountDownTimer(milliseconds, 1000) {
            override fun onTick(millisUntilFinished: Long) {
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

    private fun resetTimers() {
        _timerOneText.value = "1:00"
        _timerTwoText.value = "2:00"
        _timerFiveText.value = "5:00"
        _timerFinished.value = false
        activeTimer = 0
    }

    override fun onCleared() {
        super.onCleared()
        currentTimer?.cancel()
    }
}