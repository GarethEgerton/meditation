package com.example.meditation.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.NumberPicker
import com.example.meditation.databinding.DialogCustomTimerBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class CustomTimerDialog : BottomSheetDialogFragment() {
    private var _binding: DialogCustomTimerBinding? = null
    private val binding get() = _binding!!

    private var currentHours: Int = 0
    private var currentMinutes: Int = 0
    private var isInfinite: Boolean = false
    
    var onSave: ((hours: Int, minutes: Int, isInfinite: Boolean) -> Unit)? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogCustomTimerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupNumberPickers()
        setupInfiniteSwitch()
        setupSaveButton()

        // Restore saved state if any
        savedInstanceState?.let {
            currentHours = it.getInt(KEY_HOURS, 0)
            currentMinutes = it.getInt(KEY_MINUTES, 0)
            isInfinite = it.getBoolean(KEY_INFINITE, false)
            updateUI()
        }
    }

    private fun setupNumberPickers() {
        binding.hoursPicker.apply {
            minValue = 0
            maxValue = 23
            value = currentHours
            setOnValueChangedListener { _, _, newVal ->
                currentHours = newVal
            }
        }

        binding.minutesPicker.apply {
            minValue = 0
            maxValue = 59
            value = currentMinutes
            setOnValueChangedListener { _, _, newVal ->
                currentMinutes = newVal
            }
        }
    }

    private fun setupInfiniteSwitch() {
        binding.infiniteSwitch.apply {
            isChecked = isInfinite
            setOnCheckedChangeListener { _, isChecked ->
                isInfinite = isChecked
                binding.timePickerContainer.visibility = if (isChecked) View.GONE else View.VISIBLE
            }
        }
    }

    private fun setupSaveButton() {
        binding.saveButton.setOnClickListener {
            onSave?.invoke(currentHours, currentMinutes, isInfinite)
            dismiss()
        }
    }

    private fun updateUI() {
        binding.infiniteSwitch.isChecked = isInfinite
        binding.timePickerContainer.visibility = if (isInfinite) View.GONE else View.VISIBLE
        binding.hoursPicker.value = currentHours
        binding.minutesPicker.value = currentMinutes
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(KEY_HOURS, currentHours)
        outState.putInt(KEY_MINUTES, currentMinutes)
        outState.putBoolean(KEY_INFINITE, isInfinite)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val KEY_HOURS = "hours"
        private const val KEY_MINUTES = "minutes"
        private const val KEY_INFINITE = "infinite"

        fun newInstance(
            hours: Int = 0,
            minutes: Int = 0,
            isInfinite: Boolean = false,
            onSave: (hours: Int, minutes: Int, isInfinite: Boolean) -> Unit
        ): CustomTimerDialog {
            return CustomTimerDialog().apply {
                this.currentHours = hours
                this.currentMinutes = minutes
                this.isInfinite = isInfinite
                this.onSave = onSave
            }
        }
    }
} 