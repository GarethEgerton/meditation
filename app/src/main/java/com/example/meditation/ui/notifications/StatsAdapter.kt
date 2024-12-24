package com.example.meditation.ui.notifications

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.meditation.databinding.ItemStatsRowBinding
import java.time.LocalDate

data class StatsRow(
    val date: LocalDate,
    val totalMinutes: Int,
    val totalSessions: Int
)

class StatsAdapter : RecyclerView.Adapter<StatsAdapter.StatsViewHolder>() {
    private var statsList = emptyList<StatsRow>()

    class StatsViewHolder(private val binding: ItemStatsRowBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(stats: StatsRow) {
            binding.dateText.text = stats.date.toString()
            binding.dayText.text = stats.date.dayOfWeek.toString().substring(0, 3)
            binding.timeText.text = stats.totalMinutes.toString()
            binding.sessionsText.text = stats.totalSessions.toString()
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