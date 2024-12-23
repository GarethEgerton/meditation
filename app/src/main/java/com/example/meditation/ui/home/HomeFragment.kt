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
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.meditation.R
import com.example.meditation.databinding.FragmentHomeBinding
import com.google.android.material.button.MaterialButton

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private lateinit var toneGenerator: ToneGenerator
    private var currentAnimation: ObjectAnimator? = null
    private var pulseAnimations = mutableMapOf<MaterialButton, AnimatorSet>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val homeViewModel = ViewModelProvider(this).get(HomeViewModel::class.java)
        _binding = FragmentHomeBinding.inflate(inflater, container, false)

        // Initialize ToneGenerator for bell sound
        toneGenerator = ToneGenerator(AudioManager.STREAM_ALARM, 100)

        // Set up button click listeners
        binding.timer1min.setOnClickListener { homeViewModel.handleTimerClick(1) }
        binding.timer2min.setOnClickListener { homeViewModel.handleTimerClick(2) }
        binding.timer5min.setOnClickListener { homeViewModel.handleTimerClick(5) }

        // Set up cancel button click listeners
        binding.cancel1min.setOnClickListener { homeViewModel.cancelTimer(1) }
        binding.cancel2min.setOnClickListener { homeViewModel.cancelTimer(2) }
        binding.cancel5min.setOnClickListener { homeViewModel.cancelTimer(5) }

        // Observe timer text changes
        homeViewModel.timerOneText.observe(viewLifecycleOwner) {
            binding.timer1min.text = it
        }
        homeViewModel.timerTwoText.observe(viewLifecycleOwner) {
            binding.timer2min.text = it
        }
        homeViewModel.timerFiveText.observe(viewLifecycleOwner) {
            binding.timer5min.text = it
        }

        // Observe timer state
        homeViewModel.timerState.observe(viewLifecycleOwner) { state ->
            updateCancelButtonsVisibility(state.activeTimer, state.isPaused)
            updateActiveTimerAppearance(state.activeTimer, state.isPaused)
        }

        // Observe timer completion
        homeViewModel.timerFinished.observe(viewLifecycleOwner) { finished ->
            if (finished) {
                toneGenerator.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 1000)
            }
        }

        // Observe error events
        homeViewModel.errorEvent.observe(viewLifecycleOwner) { minutes ->
            when (minutes) {
                1 -> showErrorAnimation(binding.timer1min)
                2 -> showErrorAnimation(binding.timer2min)
                5 -> showErrorAnimation(binding.timer5min)
            }
        }

        return binding.root
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
        setButtonAppearance(binding.timer1min, activeTimer, 1, isPaused)
        setButtonAppearance(binding.timer2min, activeTimer, 2, isPaused)
        setButtonAppearance(binding.timer5min, activeTimer, 5, isPaused)
    }

    private fun setButtonAppearance(button: MaterialButton, activeTimer: Int, buttonMinutes: Int, isPaused: Boolean) {
        // Cancel any existing pulse animation
        pulseAnimations[button]?.cancel()
        pulseAnimations.remove(button)

        val baseColor = when (button.id) {
            R.id.timer_1min -> resources.getColor(R.color.timer_1min, null)
            R.id.timer_2min -> resources.getColor(R.color.timer_2min, null)
            else -> resources.getColor(R.color.timer_5min, null)
        }

        val pausedColor = when (button.id) {
            R.id.timer_1min -> resources.getColor(R.color.timer_1min_paused, null)
            R.id.timer_2min -> resources.getColor(R.color.timer_2min_paused, null)
            else -> resources.getColor(R.color.timer_5min_paused, null)
        }

        val inactiveColor = when (button.id) {
            R.id.timer_1min -> resources.getColor(R.color.timer_1min_inactive, null)
            R.id.timer_2min -> resources.getColor(R.color.timer_2min_inactive, null)
            else -> resources.getColor(R.color.timer_5min_inactive, null)
        }

        button.apply {
            when {
                // This is the active timer
                activeTimer == buttonMinutes -> {
                    if (isPaused) {
                        // Paused state appearance
                        strokeWidth = resources.getDimensionPixelSize(R.dimen.button_stroke_width)
                        strokeColor = ColorStateList.valueOf(baseColor)
                        animateButtonState(this, backgroundTintList?.defaultColor ?: baseColor, pausedColor, 1.0f)
                        isEnabled = true
                    } else {
                        // Active state appearance
                        strokeWidth = 0
                        animateButtonState(this, backgroundTintList?.defaultColor ?: baseColor, baseColor, 1.0f)
                        elevation = resources.getDimension(R.dimen.active_button_elevation)
                        isEnabled = true
                        
                        // Start pulsing animation
                        val pulseAnimation = AnimatorInflater.loadAnimator(
                            context,
                            R.anim.button_pulse
                        ) as AnimatorSet
                        pulseAnimation.setTarget(this)
                        pulseAnimation.start()
                        pulseAnimations[button] = pulseAnimation
                    }
                }
                // Another timer is active
                activeTimer != 0 -> {
                    // Inactive state appearance
                    strokeWidth = 0
                    animateButtonState(this, backgroundTintList?.defaultColor ?: baseColor, inactiveColor, 0.5f)
                    elevation = 0f
                    isEnabled = false
                    scaleX = 1.0f
                    scaleY = 1.0f
                }
                // No timer is active
                else -> {
                    // Default state appearance
                    strokeWidth = 0
                    animateButtonState(this, backgroundTintList?.defaultColor ?: baseColor, baseColor, 1.0f)
                    elevation = resources.getDimension(R.dimen.default_button_elevation)
                    isEnabled = true
                    scaleX = 1.0f
                    scaleY = 1.0f
                }
            }
        }
    }

    private fun animateButtonState(button: MaterialButton, fromColor: Int, toColor: Int, targetAlpha: Float) {
        // Color animation
        ValueAnimator.ofObject(ArgbEvaluator(), fromColor, toColor).apply {
            duration = 500 // 0.5 seconds
            addUpdateListener { animator ->
                button.backgroundTintList = ColorStateList.valueOf(animator.animatedValue as Int)
            }
            start()
        }

        // Alpha animation
        ValueAnimator.ofFloat(button.alpha, targetAlpha).apply {
            duration = 500 // 0.5 seconds
            addUpdateListener { animator ->
                button.alpha = animator.animatedValue as Float
            }
            start()
        }
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