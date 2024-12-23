package com.example.meditation.ui.home

import android.animation.AnimatorInflater
import android.animation.ArgbEvaluator
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.animation.AnimatorSet
import android.content.res.ColorStateList
import android.graphics.Color
import android.media.ToneGenerator
import android.media.AudioManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.meditation.R
import com.example.meditation.databinding.FragmentHomeBinding
import com.google.android.material.button.MaterialButton
import androidx.annotation.ColorRes
import androidx.core.content.ContextCompat

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: HomeViewModel
    private lateinit var toneGenerator: ToneGenerator
    private var currentAnimation: ObjectAnimator? = null
    private var pulseAnimations = mutableMapOf<MaterialButton, AnimatorSet>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Use activity scope for ViewModel to share data between fragments
        viewModel = ViewModelProvider(
            requireActivity(),
            ViewModelProvider.AndroidViewModelFactory(requireActivity().application)
        ).get(HomeViewModel::class.java)
        _binding = FragmentHomeBinding.inflate(inflater, container, false)

        // Initialize ToneGenerator for bell sound
        toneGenerator = ToneGenerator(AudioManager.STREAM_ALARM, 100)

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
                toneGenerator.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 1000)
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
        viewModel.oneMinCompletions.observe(viewLifecycleOwner) { completions ->
            viewModel.oneMinGoal.observe(viewLifecycleOwner) { goal ->
                updateProgressText(binding.progress1min, completions, goal)
            }
        }

        // Observe 2-minute timer progress
        viewModel.twoMinCompletions.observe(viewLifecycleOwner) { completions ->
            viewModel.twoMinGoal.observe(viewLifecycleOwner) { goal ->
                updateProgressText(binding.progress2min, completions, goal)
            }
        }

        // Observe 5-minute timer progress
        viewModel.fiveMinCompletions.observe(viewLifecycleOwner) { completions ->
            viewModel.fiveMinGoal.observe(viewLifecycleOwner) { goal ->
                updateProgressText(binding.progress5min, completions, goal)
            }
        }
    }

    private fun updateProgressText(textView: TextView, completions: Int, goal: Int?) {
        textView.apply {
            if (goal == null) {
                visibility = View.GONE
            } else {
                visibility = View.VISIBLE
                text = "$completions/$goal"
                alpha = if (completions >= goal) 1.0f else 0.8f
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

        val colorResId = when (minutes) {
            1 -> if (isInactive) R.color.timer_1min_inactive else R.color.timer_1min
            2 -> if (isInactive) R.color.timer_2min_inactive else R.color.timer_2min
            5 -> if (isInactive) R.color.timer_5min_inactive else R.color.timer_5min
            else -> R.color.black
        }

        if (isInactive) {
            animateButtonState(button, progressText, colorResId, 0.5f)
        } else {
            animateButtonState(button, progressText, colorResId, 1.0f)
        }

        button.isEnabled = !isInactive
    }

    private fun animateButtonState(button: MaterialButton, progressText: TextView, @ColorRes colorResId: Int, targetAlpha: Float) {
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
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Cancel all animations
        pulseAnimations.values.forEach { it.cancel() }
        pulseAnimations.clear()
        _binding = null
        toneGenerator.release()
    }
}