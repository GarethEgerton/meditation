package com.example.meditation.ui.home

import android.animation.AnimatorInflater
import android.animation.ArgbEvaluator
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.animation.AnimatorSet
import android.content.res.ColorStateList
import android.graphics.Color
import android.media.MediaPlayer
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.LiveData
import com.example.meditation.R
import com.example.meditation.databinding.FragmentHomeBinding
import com.google.android.material.button.MaterialButton
import androidx.annotation.ColorRes
import androidx.core.content.ContextCompat
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import com.example.meditation.ui.timer.TimerConfig
import com.example.meditation.ui.timer.TimerConfigs

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private val viewModel: HomeViewModel by viewModels()
    private var mediaPlayer: MediaPlayer? = null
    private var currentAnimation: ObjectAnimator? = null
    private var pulseAnimations = mutableMapOf<MaterialButton, AnimatorSet>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)

        setupTimerButtons()
        setupTimerObservers()
        observeProgress()

        // Add complete button click listener
        binding.completeCustom.setOnClickListener {
            viewModel.completeCustomTimer()
        }

        return binding.root
    }

    private fun setupTimerButtons() {
        // Set up timer click listeners
        TimerConfigs.configs.forEach { (minutes, config) ->
            getTimerButton(config.buttonId).setOnClickListener { 
                viewModel.handleTimerClick(minutes)
            }
            getCancelButton(config.cancelButtonId).setOnClickListener {
                viewModel.cancelTimer(minutes)
            }
        }

        // Set up custom timer long press
        binding.timerCustom.setOnLongClickListener {
            showCustomTimerDialog()
            true
        }

        // Set up complete button for custom timer
        binding.completeCustom.setOnClickListener {
            viewModel.completeCustomTimer()
        }
    }

    private fun setupTimerObservers() {
        // Observe timer text changes
        viewModel.timerOneText.observe(viewLifecycleOwner) {
            binding.timer1min.text = it
        }
        viewModel.timerTwoText.observe(viewLifecycleOwner) {
            binding.timer2min.text = it
        }
        viewModel.timerFiveText.observe(viewLifecycleOwner) {
            binding.timer5min.text = it
        }
        viewModel.timerCustomText.observe(viewLifecycleOwner) {
            binding.timerCustom.text = it
        }

        // Observe timer state
        viewModel.timerState.observe(viewLifecycleOwner) { state ->
            updateCancelButtonsVisibility(state.activeTimer, state.isPaused)
            updateActiveTimerAppearance(state.activeTimer, state.isPaused)
        }

        // Observe timer completion
        viewModel.timerFinished.observe(viewLifecycleOwner) { finished ->
            if (finished) {
                playTimerCompletionSound()
            }
        }

        // Observe error events
        viewModel.errorEvent.observe(viewLifecycleOwner) { minutes ->
            val config = TimerConfigs.getConfigForMinutes(minutes)
            showErrorAnimation(getTimerButton(config.buttonId))
        }
    }

    private fun observeProgress() {
        // Observe daily progress
        viewModel.dailyProgress.observe(viewLifecycleOwner) { progress ->
            binding.dailyGoalProgress.max = progress.target
            binding.dailyGoalProgress.progress = progress.completed
            binding.dailyGoalText.text = getString(
                R.string.daily_progress_format,
                progress.completed,
                progress.target
            )

            // Update card and progress bar state when goal is completed
            val isGoalCompleted = progress.completed >= progress.target && progress.target > 0
            binding.dailyGoalCard.isActivated = isGoalCompleted
            binding.dailyGoalProgress.setIndicatorColor(
                ContextCompat.getColor(
                    requireContext(),
                    if (isGoalCompleted) R.color.daily_goal_completed_progress_bar else R.color.daily_goal_progress_bar
                )
            )
        }

        // Existing timer progress observations
        TimerConfigs.configs.forEach { (minutes, config) ->
            when (minutes) {
                1 -> observeTimerProgress(viewModel.oneMinCompletions, viewModel.oneMinGoal, config)
                2 -> observeTimerProgress(viewModel.twoMinCompletions, viewModel.twoMinGoal, config)
                5 -> observeTimerProgress(viewModel.fiveMinCompletions, viewModel.fiveMinGoal, config)
                TimerConfigs.CUSTOM_TIMER_ID -> {
                    viewModel.customCompletions.observe(viewLifecycleOwner) { completionState ->
                        updateProgressText(getProgressText(config.progressTextId), completionState.count, 0, completionState.isToday)
                    }
                }
            }
        }
    }

    private fun observeTimerProgress(
        completions: LiveData<HomeViewModel.CompletionState>,
        goal: LiveData<Int>,
        config: TimerConfig
    ) {
        completions.observe(viewLifecycleOwner) { completionState ->
            goal.observe(viewLifecycleOwner) { goalValue ->
                updateProgressText(
                    getProgressText(config.progressTextId),
                    completionState.count,
                    goalValue,
                    completionState.isToday
                )
            }
        }
    }

    private fun updateProgressText(textView: TextView, completions: Int, goal: Int?, isToday: Boolean) {
        textView.apply {
            if (goal == null || goal == 0) {
                visibility = View.GONE
                return
            }

            visibility = View.VISIBLE
            text = "$completions/$goal"
            
            val config = TimerConfigs.configs.values.find { it.progressTextId == id }
                ?: return

            if (completions < goal && !isToday) {
                showErrorAnimation(textView, config.activeColor)
            } else {
                setTextColor(ContextCompat.getColor(context, config.activeColor))
                alpha = if (completions >= goal) 1.0f else 0.8f
            }
        }
    }

    private fun showErrorAnimation(textView: TextView, @ColorRes baseColor: Int) {
        val errorColor = Color.parseColor("#FFFF5252")
        val baseColorValue = ContextCompat.getColor(textView.context, baseColor)
        
        ValueAnimator.ofObject(ArgbEvaluator(), baseColorValue, errorColor).apply {
            duration = 1500
            repeatMode = ValueAnimator.REVERSE
            repeatCount = ValueAnimator.INFINITE
            addUpdateListener { animator ->
                textView.setTextColor(animator.animatedValue as Int)
            }
            start()
        }
        textView.alpha = 1.0f
    }

    private fun updateCancelButtonsVisibility(activeTimer: Int, isPaused: Boolean) {
        TimerConfigs.configs.forEach { (minutes, config) ->
            getCancelButton(config.cancelButtonId).visibility = 
                if (activeTimer == minutes && isPaused) View.VISIBLE else View.GONE
        }

        // Update complete button visibility for custom timer
        binding.completeCustom.visibility = 
            if (activeTimer == -1 && isPaused) View.VISIBLE else View.GONE
    }

    private fun showErrorAnimation(button: MaterialButton) {
        val config = TimerConfigs.getConfigForButtonId(button.id)
        val colorFrom = ContextCompat.getColor(requireContext(), config.activeColor)
        val colorTo = Color.parseColor("#FFFF5252")

        ValueAnimator.ofObject(ArgbEvaluator(), colorFrom, colorTo).apply {
            duration = 100
            addUpdateListener { animator ->
                button.backgroundTintList = ColorStateList.valueOf(animator.animatedValue as Int)
            }
            start()
        }

        ValueAnimator.ofObject(ArgbEvaluator(), colorTo, colorFrom).apply {
            duration = 100
            startDelay = 100
            addUpdateListener { animator ->
                button.backgroundTintList = ColorStateList.valueOf(animator.animatedValue as Int)
            }
            start()
        }
    }

    private fun updateActiveTimerAppearance(activeTimer: Int, isPaused: Boolean) {
        TimerConfigs.configs.forEach { (_, config) ->
            setButtonAppearance(
                getTimerButton(config.buttonId),
                getProgressText(config.progressTextId),
                config
            )
        }
    }

    private fun setButtonAppearance(button: MaterialButton, progressText: TextView, config: TimerConfig) {
        val timerState = viewModel.timerState.value
        val isActiveTimer = timerState?.activeTimer == config.minutes
        val isAnyTimerActive = timerState?.activeTimer != 0
        val isInactive = isAnyTimerActive && !isActiveTimer
        val isPaused = isActiveTimer && timerState?.isPaused == true

        val colorResId = when {
            isInactive -> config.inactiveColor
            isPaused -> config.pausedColor
            else -> config.activeColor
        }

        button.backgroundTintList = ColorStateList.valueOf(
            ContextCompat.getColor(requireContext(), colorResId)
        )
        button.strokeColor = ColorStateList.valueOf(
            ContextCompat.getColor(requireContext(), config.strokeColor)
        )
    }

    private fun getTimerButton(buttonId: Int): MaterialButton = when (buttonId) {
        R.id.timer_1min -> binding.timer1min
        R.id.timer_2min -> binding.timer2min
        R.id.timer_5min -> binding.timer5min
        R.id.timer_custom -> binding.timerCustom
        else -> throw IllegalArgumentException("Unknown button ID: $buttonId")
    }

    private fun getCancelButton(buttonId: Int): View = when (buttonId) {
        R.id.cancel_1min -> binding.cancel1min
        R.id.cancel_2min -> binding.cancel2min
        R.id.cancel_5min -> binding.cancel5min
        R.id.cancel_custom -> binding.cancelCustom
        else -> throw IllegalArgumentException("Unknown cancel button ID: $buttonId")
    }

    private fun getProgressText(textId: Int): TextView = when (textId) {
        R.id.progress_1min -> binding.progress1min
        R.id.progress_2min -> binding.progress2min
        R.id.progress_5min -> binding.progress5min
        R.id.progress_custom -> binding.progressCustom
        else -> throw IllegalArgumentException("Unknown progress text ID: $textId")
    }

    private fun playTimerCompletionSound() {
        mediaPlayer?.release()
        mediaPlayer = MediaPlayer.create(requireContext(), R.raw.bell_end_timer)
        mediaPlayer?.setOnCompletionListener { mp ->
            mp.release()
            mediaPlayer = null
        }
        mediaPlayer?.start()
    }

    private fun showCustomTimerDialog() {
        CustomTimerDialog.newInstance(
            onSave = { hours, minutes, isInfinite ->
                viewModel.updateCustomTimer(hours, minutes, isInfinite)
            }
        ).show(childFragmentManager, "custom_timer_dialog")
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Cancel all animations
        pulseAnimations.values.forEach { it.cancel() }
        pulseAnimations.clear()
        _binding = null
        mediaPlayer?.release()
        mediaPlayer = null
    }
}