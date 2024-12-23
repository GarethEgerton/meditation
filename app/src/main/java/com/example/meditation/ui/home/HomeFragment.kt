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
        val colorFrom = Color.parseColor("#BB86FC")
        val colorTo = Color.parseColor("#FF5252")

        ValueAnimator.ofObject(ArgbEvaluator(), colorFrom, colorTo).apply {
            duration = 100
            addUpdateListener { animator ->
                button.setBackgroundColor(animator.animatedValue as Int)
            }
            start()
        }

        ValueAnimator.ofObject(ArgbEvaluator(), colorTo, colorFrom).apply {
            duration = 100
            startDelay = 100
            addUpdateListener { animator ->
                button.setBackgroundColor(animator.animatedValue as Int)
            }
            start()
        }
    }

    private fun updateActiveTimerAppearance(activeTimer: Int, isPaused: Boolean) {
        // Reset all buttons to default state
        resetButtonAppearance(binding.timer1min)
        resetButtonAppearance(binding.timer2min)
        resetButtonAppearance(binding.timer5min)

        // Update active timer appearance
        when (activeTimer) {
            1 -> setActiveButtonAppearance(binding.timer1min, isPaused)
            2 -> setActiveButtonAppearance(binding.timer2min, isPaused)
            5 -> setActiveButtonAppearance(binding.timer5min, isPaused)
        }
    }

    private fun resetButtonAppearance(button: MaterialButton) {
        // Cancel any existing pulse animation
        pulseAnimations[button]?.cancel()
        pulseAnimations.remove(button)

        button.apply {
            strokeWidth = 0
            setBackgroundColor(Color.parseColor("#BB86FC"))
            elevation = resources.getDimension(R.dimen.default_button_elevation)
            scaleX = 1.0f
            scaleY = 1.0f
        }
    }

    private fun setActiveButtonAppearance(button: MaterialButton, isPaused: Boolean) {
        // Cancel any existing pulse animation
        pulseAnimations[button]?.cancel()
        pulseAnimations.remove(button)

        button.apply {
            if (isPaused) {
                // Paused state appearance
                strokeWidth = resources.getDimensionPixelSize(R.dimen.button_stroke_width)
                strokeColor = ColorStateList.valueOf(Color.parseColor("#BB86FC"))
                setBackgroundColor(Color.parseColor("#3D2E5C"))
            } else {
                // Active state appearance
                strokeWidth = 0
                setBackgroundColor(Color.parseColor("#9965F4"))
                elevation = resources.getDimension(R.dimen.active_button_elevation)
                
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