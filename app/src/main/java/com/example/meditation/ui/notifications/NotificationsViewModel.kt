package com.example.meditation.ui.notifications

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.meditation.data.MeditationCompletion
import com.example.meditation.data.MeditationDatabase
import com.example.meditation.data.MeditationRepository
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class NotificationsViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: MeditationRepository
    private val _statsRows = MutableLiveData<List<StatsRow>>()
    val statsRows: LiveData<List<StatsRow>> = _statsRows

    init {
        val database = MeditationDatabase.getDatabase(application)
        repository = MeditationRepository(database.meditationDao())
        loadStats()
    }

    private fun loadStats() {
        viewModelScope.launch {
            repository.getAllCompletions().collect { completions ->
                val today = LocalDate.now()
                val twoWeeksAgo = today.minusDays(13) // 14 days including today

                val statsMap = mutableMapOf<LocalDate, MutableList<MeditationCompletion>>()
                
                // Initialize the map with all dates
                for (i in 0..13) {
                    val date = today.minusDays(i.toLong())
                    statsMap[date] = mutableListOf()
                }

                // Group completions by date
                completions.forEach { completion ->
                    val date = LocalDate.parse(completion.date)
                    if (date >= twoWeeksAgo && date <= today) {
                        statsMap[date]?.add(completion)
                    }
                }

                // Convert to StatsRow objects
                val stats = statsMap.entries
                    .sortedByDescending { it.key }
                    .map { (date, completions) ->
                        StatsRow(
                            date = date,
                            totalMinutes = completions.sumOf { it.timerMinutes },
                            totalSessions = completions.size
                        )
                    }

                _statsRows.value = stats
            }
        }
    }
}