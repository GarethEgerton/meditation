package com.example.meditation.ui.dashboard

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class DashboardViewModel : ViewModel() {
    private val _oneMinGoal = MutableLiveData<Int>(0)
    val oneMinGoal: LiveData<Int> = _oneMinGoal

    private val _twoMinGoal = MutableLiveData<Int>(0)
    val twoMinGoal: LiveData<Int> = _twoMinGoal

    private val _fiveMinGoal = MutableLiveData<Int>(0)
    val fiveMinGoal: LiveData<Int> = _fiveMinGoal

    fun updateOneMinGoal(value: Int?) {
        _oneMinGoal.value = value ?: 0
    }

    fun updateTwoMinGoal(value: Int?) {
        _twoMinGoal.value = value ?: 0
    }

    fun updateFiveMinGoal(value: Int?) {
        _fiveMinGoal.value = value ?: 0
    }
}