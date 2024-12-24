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
import com.example.meditation.R
import com.example.meditation.databinding.FragmentHomeBinding
import com.google.android.material.button.MaterialButton
import androidx.annotation.ColorRes
import androidx.core.content.ContextCompat

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

        // Set up button click listeners
        binding.timer1min.setOnClickListener { viewModel.handleTimerClick(1) }
        binding.timer2min.setOnClickListener { viewModel.handleTimerClick(2) }
        binding.timer5min.setOnClickListener { viewModel.handleTimerClick(5) }

        // Set up cancel button click listeners
        binding.cancel1min.setOnClickListener { viewModel.cancelTimer(1) }
        binding.cancel2min.setOnClickListener { viewModel.cancelTimer(2) }
        binding.cancel5min.setOnClickListener { viewModel.cancelTimer(5) }

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
            when (minutes) {
                1 -> showErrorAnimation(binding.timer1min)
                2 -> showErrorAnimation(binding.timer2min)
                5 -> showErrorAnimation(binding.timer5min)
            }
        }

        // Observe completion progress
        observeProgress()

        return binding.root
    }

    private fun observeProgress() {
        // Observe 1-minute timer progress
        viewModel.oneMinCompletions.observe(viewLifecycleOwner) { completionState ->
            viewModel.oneMinGoal.observe(viewLifecycleOwner) { goal ->
                updateProgressText(binding.progress1min, completionState.count, goal, completionState.isToday)
            }
        }

        // Observe 2-minute timer progress
        viewModel.twoMinCompletions.observe(viewLifecycleOwner) { completionState ->
            viewModel.twoMinGoal.observe(viewLifecycleOwner) { goal ->
                updateProgressText(binding.progress2min, completionState.count, goal, completionState.isToday)
            }
        }

        // Observe 5-minute timer progress
        viewModel.fiveMinCompletions.observe(viewLifecycleOwner) { completionState ->
            viewModel.fiveMinGoal.observe(viewLifecycleOwner) { goal ->
                updateProgressText(binding.progress5min, completionState.count, goal, completionState.isToday)
            }
        }
    }

    private fun updateProgressText(textView: TextView, completions: Int, goal: Int?, isToday: Boolean) {
        textView.apply {
            if (goal == null || goal == 0) {
                visibility = View.GONE
            } else {
                visibility = View.VISIBLE
                text = "$completions/$goal"
                
                // Get the base color for the timer
                val baseColor = when (id) {
                    R.id.progress_1min -> R.color.timer_1min
                    R.id.progress_2min -> R.color.timer_2min
                    R.id.progress_5min -> R.color.timer_5min
                    else -> R.color.black
                }

                // If goal is not met for a previous day, show in red with a subtle animation
                if (completions < goal && !isToday) {
                    val errorColor = Color.parseColor("#FFFF5252")
                    val baseColorValue = ContextCompat.getColor(context, baseColor)
                    
                    ValueAnimator.ofObject(ArgbEvaluator(), baseColorValue, errorColor).apply {
                        duration = 1500
                        repeatMode = ValueAnimator.REVERSE
                        repeatCount = ValueAnimator.INFINITE
                        addUpdateListener { animator ->
                            setTextColor(animator.animatedValue as Int)
                        }
                        start()
                    }
                    alpha = 1.0f
                } else {
                    // Goal is met or it's today, use normal color
                    setTextColor(ContextCompat.getColor(context, baseColor))
                    alpha = if (completions >= goal) 1.0f else 0.8f
                }
            }
        }
    }

    private fun updateCancelButtonsVisibility(activeTimer: Int, isPaused: Boolean) {
        binding.cancel1min.visibility = if (activeTimer == 1 && isPaused) View.VISIBLE else View.GONE
        binding.cancel2min.visibility = if (activeTimer == 2 && isPaused) View.VISIBLE else View.GONE
        binding.cancel5min.visibility = if (activeTimer == 5 && isPaused) View.VISIBLE else View.GONE
    }

    private fun showErrorAnimation(button: MaterialButton) {
        val colorFrom = when (button.id) {
            R.id.timer_1min -> resources.getColor(R.color.timer_1min, null)
            R.id.timer_2min -> resources.getColor(R.color.timer_2min, null)
            else -> resources.getColor(R.color.timer_5min, null)
        }
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
        // Update all buttons based on state
        setButtonAppearance(binding.timer1min, binding.progress1min, 1)
        setButtonAppearance(binding.timer2min, binding.progress2min, 2)
        setButtonAppearance(binding.timer5min, binding.progress5min, 5)
    }

    private fun setButtonAppearance(button: MaterialButton, progressText: TextView, minutes: Int) {
        val timerState = viewModel.timerState.value
        val isActiveTimer = timerState?.activeTimer == minutes
        val isAnyTimerActive = timerState?.activeTimer != 0
        val isInactive = isAnyTimerActive && !isActiveTimer
        val isPaused = isActiveTimer && timerState?.isPaused == true

        val colorResId = when {
            isInactive -> when (minutes) {
                1 -> R.color.timer_1min_inactive
                2 -> R.color.timer_2min_inactive
                5 -> R.color.timer_5min_inactive
                else -> R.color.black
            }
            isPaused -> when (minutes) {
                1 -> R.color.timer_1min_paused
                2 -> R.color.timer_2min_paused
                5 -> R.color.timer_5min_paused
                else -> R.color.black
            }
            else -> when (minutes) {
                1 -> R.color.timer_1min
                2 -> R.color.timer_2min
                5 -> R.color.timer_5min
                else -> R.color.black
            }
        }

        if (isInactive) {
            animateButtonState(button, progressText, colorResId, 0.5f, 0)
        } else if (isPaused) {
            animateButtonState(button, progressText, colorResId, 0.8f, resources.getDimensionPixelSize(R.dimen.button_stroke_width))
        } else {
            animateButtonState(button, progressText, colorResId, 1.0f, 0)
        }

        button.isEnabled = !isInactive
    }

    private fun animateButtonState(button: MaterialButton, progressText: TextView, @ColorRes colorResId: Int, targetAlpha: Float, strokeWidth: Int) {
        val context = button.context
        val color = ContextCompat.getColor(context, colorResId)
        
        button.animate()
            .alpha(targetAlpha)
            .setDuration(500)
            .start()
            
        progressText.animate()
            .alpha(targetAlpha)
            .setDuration(500)
            .start()

        val colorAnimator = ValueAnimator.ofObject(ArgbEvaluator(), button.backgroundTintList?.defaultColor, color)
        colorAnimator.duration = 500
        colorAnimator.addUpdateListener { animator ->
            button.backgroundTintList = ColorStateList.valueOf(animator.animatedValue as Int)
            progressText.setTextColor(animator.animatedValue as Int)
        }
        colorAnimator.start()

        // Animate stroke width
        ValueAnimator.ofInt(button.strokeWidth, strokeWidth).apply {
            duration = 500
            addUpdateListener { animator ->
                button.strokeWidth = animator.animatedValue as Int
            }
            start()
        }
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