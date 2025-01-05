package com.example.meditation.ui.notifications

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.meditation.databinding.ItemStatsRowBinding
import java.time.LocalDate
import java.time.format.DateTimeFormatter

data class StatsRow(
    val date: LocalDate,
    val totalMinutes: Int,
    val totalMinutesGoal: Int,
    val totalSessions: Int,
    val totalSessionsGoal: Int,
    val isGoalCompleted: Boolean = false,
    val oneMinGoal: Int = 0,
    val twoMinGoal: Int = 0,
    val fiveMinGoal: Int = 0,
    val oneMinCompletions: Int = 0,
    val twoMinCompletions: Int = 0,
    val fiveMinCompletions: Int = 0
)

class StatsAdapter : RecyclerView.Adapter<StatsAdapter.StatsViewHolder>() {
    private var statsList = emptyList<StatsRow>()

    class StatsViewHolder(private val binding: ItemStatsRowBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(stats: StatsRow) {
            binding.root.isActivated = stats.isGoalCompleted
            binding.dateText.text = stats.date.format(DateTimeFormatter.ofPattern("MMM d"))
            binding.dayText.text = stats.date.dayOfWeek.toString().substring(0, 3)
            binding.timeText.text = stats.totalMinutes.toString()
            binding.sessionsText.text = stats.totalSessions.toString()
            
            // Show completions even when there's no goal
            binding.oneMinGoalText.text = if (stats.oneMinCompletions > 0 || stats.oneMinGoal > 0) 
                "${stats.oneMinCompletions}/${stats.oneMinGoal}" else "-"
            binding.twoMinGoalText.text = if (stats.twoMinCompletions > 0 || stats.twoMinGoal > 0) 
                "${stats.twoMinCompletions}/${stats.twoMinGoal}" else "-"
            binding.fiveMinGoalText.text = if (stats.fiveMinCompletions > 0 || stats.fiveMinGoal > 0) 
                "${stats.fiveMinCompletions}/${stats.fiveMinGoal}" else "-"
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StatsViewHolder {
        val binding = ItemStatsRowBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return StatsViewHolder(binding)
    }

    override fun onBindViewHolder(holder: StatsViewHolder, position: Int) {
        holder.bind(statsList[position])
    }

    override fun getItemCount() = statsList.size

    fun submitList(list: List<StatsRow>) {
        statsList = list
        notifyDataSetChanged()
    }
} 