package com.example.meditation.ui.timer

import androidx.annotation.ColorRes
import com.example.meditation.R
import com.google.android.material.button.MaterialButton
import android.widget.TextView

data class TimerConfig(
    val minutes: Int,
    @ColorRes val activeColor: Int,
    @ColorRes val inactiveColor: Int,
    @ColorRes val pausedColor: Int,
    @ColorRes val strokeColor: Int,
    val buttonId: Int,
    val progressTextId: Int,
    val cancelButtonId: Int
)

object TimerConfigs {
    const val CUSTOM_TIMER_ID = -1
    
    val configs = mapOf(
        1 to TimerConfig(
            minutes = 1,
            activeColor = R.color.timer_1min,
            inactiveColor = R.color.timer_1min_inactive,
            pausedColor = R.color.timer_1min_paused,
            strokeColor = R.color.timer_1min_stroke,
            buttonId = R.id.timer_1min,
            progressTextId = R.id.progress_1min,
            cancelButtonId = R.id.cancel_1min
        ),
        2 to TimerConfig(
            minutes = 2,
            activeColor = R.color.timer_2min,
            inactiveColor = R.color.timer_2min_inactive,
            pausedColor = R.color.timer_2min_paused,
            strokeColor = R.color.timer_2min_stroke,
            buttonId = R.id.timer_2min,
            progressTextId = R.id.progress_2min,
            cancelButtonId = R.id.cancel_2min
        ),
        5 to TimerConfig(
            minutes = 5,
            activeColor = R.color.timer_5min,
            inactiveColor = R.color.timer_5min_inactive,
            pausedColor = R.color.timer_5min_paused,
            strokeColor = R.color.timer_5min_stroke,
            buttonId = R.id.timer_5min,
            progressTextId = R.id.progress_5min,
            cancelButtonId = R.id.cancel_5min
        ),
        CUSTOM_TIMER_ID to TimerConfig(
            minutes = CUSTOM_TIMER_ID,
            activeColor = R.color.timer_custom,
            inactiveColor = R.color.timer_custom_inactive,
            pausedColor = R.color.timer_custom_paused,
            strokeColor = R.color.timer_custom_stroke,
            buttonId = R.id.timer_custom,
            progressTextId = R.id.progress_custom,
            cancelButtonId = R.id.cancel_custom
        )
    )

    fun getConfigForMinutes(minutes: Int): TimerConfig = configs[minutes] 
        ?: throw IllegalArgumentException("No config for $minutes minutes")

    fun getConfigForButtonId(buttonId: Int): TimerConfig = configs.values.find { it.buttonId == buttonId }
        ?: throw IllegalArgumentException("No config for button ID $buttonId")
} 