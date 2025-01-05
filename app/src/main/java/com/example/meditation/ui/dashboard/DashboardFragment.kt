package com.example.meditation.ui.dashboard

import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import androidx.fragment.app.Fragment
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModelProvider
import com.example.meditation.R
import com.example.meditation.databinding.FragmentDashboardBinding
import com.google.android.material.textfield.TextInputEditText
import com.example.meditation.ui.timer.TimerConfigs

class DashboardFragment : Fragment() {

    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!
    private lateinit var dashboardViewModel: DashboardViewModel
    private var textWatchers = mutableMapOf<Int, TextWatcher>()

    private val timerToGoalInputMap = mapOf(
        R.id.timer_1min to R.id.one_min_goal,
        R.id.timer_2min to R.id.two_min_goal,
        R.id.timer_5min to R.id.five_min_goal
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        dashboardViewModel = ViewModelProvider(
            this,
            ViewModelProvider.AndroidViewModelFactory(requireActivity().application)
        ).get(DashboardViewModel::class.java)
        
        _binding = FragmentDashboardBinding.inflate(inflater, container, false)

        setupGoalInputs()
        observeGoals()
        observeDailyProgress()

        return binding.root
    }

    private fun setupGoalInputs() {
        // Initialize EditText fields with default values
        getGoalInput(R.id.daily_minutes_goal).setText("0")
        TimerConfigs.configs.forEach { (_, config) ->
            val goalInputId = timerToGoalInputMap[config.buttonId] ?: return@forEach
            getGoalInput(goalInputId).setText("0")
        }

        // Set up text change listeners
        setupGoalInput(
            getGoalInput(R.id.daily_minutes_goal),
            dashboardViewModel::updateDailyMinutesGoal
        )

        TimerConfigs.configs.forEach { (minutes, config) ->
            val goalInputId = timerToGoalInputMap[config.buttonId] ?: return@forEach
            val updateFunction = when (minutes) {
                1 -> dashboardViewModel::updateOneMinGoal
                2 -> dashboardViewModel::updateTwoMinGoal
                5 -> dashboardViewModel::updateFiveMinGoal
                else -> return@forEach // Skip custom timer
            }
            setupGoalInput(getGoalInput(goalInputId), updateFunction)
        }
    }

    private fun observeGoals() {
        // Observe timer goals
        TimerConfigs.configs.forEach { (minutes, config) ->
            val goalInputId = timerToGoalInputMap[config.buttonId] ?: return@forEach
            when (minutes) {
                1 -> observeGoal(dashboardViewModel.oneMinGoal, goalInputId)
                2 -> observeGoal(dashboardViewModel.twoMinGoal, goalInputId)
                5 -> observeGoal(dashboardViewModel.fiveMinGoal, goalInputId)
                // Skip custom timer
            }
        }
    }

    private fun observeGoal(goalLiveData: LiveData<Int>, inputId: Int) {
        goalLiveData.observe(viewLifecycleOwner) { goal ->
            val input = getGoalInput(inputId)
            if (input.text.toString() != goal.toString()) {
                input.setText(goal.toString())
            }
        }
    }

    private fun observeDailyProgress() {
        // Observe daily minutes goal and progress
        dashboardViewModel.dailyMinutesGoal.observe(viewLifecycleOwner) { goal ->
            val input = getGoalInput(R.id.daily_minutes_goal)
            if (input.text.toString() != goal.toString()) {
                input.setText(goal.toString())
            }
        }

        dashboardViewModel.todayTotalMinutes.observe(viewLifecycleOwner) { minutes ->
            val goal = dashboardViewModel.dailyMinutesGoal.value ?: 0
            updateDailyMinutesProgress(minutes, goal)
        }
    }

    private fun updateDailyMinutesProgress(minutes: Int, goal: Int) {
        binding.dailyMinutesProgress.text = "$minutes/$goal minutes today"
        binding.dailyMinutesProgressBar.max = maxOf(goal, minutes)
        binding.dailyMinutesProgressBar.progress = minutes
    }

    private fun setupGoalInput(input: TextInputEditText, updateFunction: (Int?) -> Unit) {
        val textWatcher = object : TextWatcher {
            private var isUpdating = false

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (!isUpdating) {
                    val text = s?.toString() ?: ""
                    val value = if (text.isEmpty()) 0 else text.toIntOrNull() ?: 0
                    updateFunction(value)
                }
            }
            
            override fun afterTextChanged(s: Editable?) {
                if (isUpdating) return
                
                isUpdating = true
                try {
                    val text = s?.toString() ?: ""
                    if (text.isEmpty()) {
                        input.setText("0")
                    }
                    input.text?.length?.let { length ->
                        input.setSelection(length)
                    }
                } finally {
                    isUpdating = false
                }
            }
        }
        
        textWatchers[input.id] = textWatcher
        input.addTextChangedListener(textWatcher)
    }

    private fun getGoalInput(inputId: Int): TextInputEditText = when (inputId) {
        R.id.daily_minutes_goal -> binding.dailyMinutesGoal
        R.id.one_min_goal -> binding.oneMinGoal
        R.id.two_min_goal -> binding.twoMinGoal
        R.id.five_min_goal -> binding.fiveMinGoal
        else -> throw IllegalArgumentException("Unknown input ID: $inputId")
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Remove text watchers
        textWatchers.forEach { (inputId, watcher) ->
            getGoalInput(inputId).removeTextChangedListener(watcher)
        }
        textWatchers.clear()
        _binding = null
    }
}