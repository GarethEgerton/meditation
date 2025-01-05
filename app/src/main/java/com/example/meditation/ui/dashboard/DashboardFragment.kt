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
import androidx.lifecycle.ViewModelProvider
import com.example.meditation.databinding.FragmentDashboardBinding
import com.google.android.material.textfield.TextInputEditText
import com.example.meditation.ui.home.HomeViewModel

class DashboardFragment : Fragment() {

    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!
    private lateinit var dashboardViewModel: DashboardViewModel
    private var textWatchers = mutableListOf<TextWatcher>()

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

        // Initialize EditText fields with default values
        binding.dailyMinutesGoal.setText("0")
        binding.oneMinGoal.setText("0")
        binding.twoMinGoal.setText("0")
        binding.fiveMinGoal.setText("0")

        // Set up text change listeners
        setupGoalInput(binding.dailyMinutesGoal, dashboardViewModel::updateDailyMinutesGoal)
        setupGoalInput(binding.oneMinGoal, dashboardViewModel::updateOneMinGoal)
        setupGoalInput(binding.twoMinGoal, dashboardViewModel::updateTwoMinGoal)
        setupGoalInput(binding.fiveMinGoal, dashboardViewModel::updateFiveMinGoal)

        // Observe daily minutes goal and progress
        dashboardViewModel.dailyMinutesGoal.observe(viewLifecycleOwner) { goal ->
            if (binding.dailyMinutesGoal.text.toString() != goal.toString()) {
                binding.dailyMinutesGoal.setText(goal.toString())
            }
        }

        dashboardViewModel.todayTotalMinutes.observe(viewLifecycleOwner) { minutes ->
            val goal = dashboardViewModel.dailyMinutesGoal.value ?: 0
            updateDailyMinutesProgress(minutes, goal)
        }

        // Observe other goal values
        dashboardViewModel.oneMinGoal.observe(viewLifecycleOwner) { goal ->
            if (binding.oneMinGoal.text.toString() != goal.toString()) {
                binding.oneMinGoal.setText(goal.toString())
            }
        }

        dashboardViewModel.twoMinGoal.observe(viewLifecycleOwner) { goal ->
            if (binding.twoMinGoal.text.toString() != goal.toString()) {
                binding.twoMinGoal.setText(goal.toString())
            }
        }

        dashboardViewModel.fiveMinGoal.observe(viewLifecycleOwner) { goal ->
            if (binding.fiveMinGoal.text.toString() != goal.toString()) {
                binding.fiveMinGoal.setText(goal.toString())
            }
        }

        return binding.root
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
        
        textWatchers.add(textWatcher)
        input.addTextChangedListener(textWatcher)

        // Clear focus when done editing
        input.setOnFocusChangeListener { v, hasFocus ->
            if (!hasFocus) {
                hideKeyboard(v)
            }
        }
    }

    private fun hideKeyboard(view: View) {
        try {
            val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(view.windowToken, 0)
        } catch (e: Exception) {
            // Ignore any exceptions that might occur during keyboard hiding
        }
    }

    override fun onPause() {
        super.onPause()
        // Remove focus from EditTexts
        binding.dailyMinutesGoal.clearFocus()
        binding.oneMinGoal.clearFocus()
        binding.twoMinGoal.clearFocus()
        binding.fiveMinGoal.clearFocus()
        
        // Hide keyboard
        view?.let { hideKeyboard(it) }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Remove text watchers
        textWatchers.forEach { watcher ->
            binding.dailyMinutesGoal.removeTextChangedListener(watcher)
            binding.oneMinGoal.removeTextChangedListener(watcher)
            binding.twoMinGoal.removeTextChangedListener(watcher)
            binding.fiveMinGoal.removeTextChangedListener(watcher)
        }
        textWatchers.clear()
        _binding = null
    }
}