package com.example.meditation.ui.home

import android.media.ToneGenerator
import android.media.AudioManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.meditation.databinding.FragmentHomeBinding

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
        binding.timer1min.setOnClickListener { homeViewModel.startTimer(1) }
        binding.timer2min.setOnClickListener { homeViewModel.startTimer(2) }
        binding.timer5min.setOnClickListener { homeViewModel.startTimer(5) }

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

        // Observe timer completion
        homeViewModel.timerFinished.observe(viewLifecycleOwner) { finished ->
            if (finished) {
                toneGenerator.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 1000)
            }
        }

        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        toneGenerator.release()
    }
}