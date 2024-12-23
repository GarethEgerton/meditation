package com.example.meditation.ui.home

import android.animation.AnimatorInflater
import android.animation.ArgbEvaluator
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        toneGenerator.release()
    }
}