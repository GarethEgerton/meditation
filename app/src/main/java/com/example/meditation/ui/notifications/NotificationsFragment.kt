package com.example.meditation.ui.notifications

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.meditation.databinding.FragmentNotificationsBinding
import java.time.format.DateTimeFormatter
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter

class NotificationsFragment : Fragment() {
    private var _binding: FragmentNotificationsBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: NotificationsViewModel
    private val adapter = StatsAdapter()
    private val dateFormatter = DateTimeFormatter.ofPattern("MMM d")

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        viewModel = ViewModelProvider(
            this,
            ViewModelProvider.AndroidViewModelFactory(requireActivity().application)
        ).get(NotificationsViewModel::class.java)

        _binding = FragmentNotificationsBinding.inflate(inflater, container, false)
        
        binding.statsRecyclerView.adapter = adapter

        viewModel.statsRows.observe(viewLifecycleOwner) { stats ->
            adapter.submitList(stats)
            updateChart(stats)
        }

        return binding.root
    }

    private fun updateChart(stats: List<StatsRow>) {
        val entries = mutableListOf<BarEntry>()

        // Process data in reverse order (oldest to newest)
        stats.asReversed().forEachIndexed { index, row ->
            // Create stacked bar entry with [completed, remaining] values
            val completed = row.totalSessions.toFloat()
            val totalGoals = (row.oneMinGoal + row.twoMinGoal + row.fiveMinGoal).toFloat()
            val remaining = if (totalGoals > completed) totalGoals - completed else 0f
            
            entries.add(BarEntry(index.toFloat(), floatArrayOf(completed, remaining)))
        }

        val completedDataSet = BarDataSet(entries, "Completed Sessions").apply {
            color = resources.getColor(android.R.color.holo_green_light, null)
            valueTextColor = resources.getColor(android.R.color.white, null)
            valueTextSize = 8f
            setDrawValues(true)
            stackLabels = arrayOf("Completed", "Remaining")
        }

        completedDataSet.colors = listOf(
            resources.getColor(android.R.color.holo_green_light, null),
            resources.getColor(android.R.color.holo_orange_light, null)
        )

        val barData = BarData(completedDataSet).apply {
            barWidth = 0.5f
        }

        // Get dates for X-axis labels
        val dates = stats.asReversed().map { it.date.format(DateTimeFormatter.ofPattern("MMM d")) }

        binding.statsChart.apply {
            data = barData
            description.isEnabled = false
            
            // X-axis styling
            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                textColor = resources.getColor(android.R.color.white, null)
                granularity = 1f
                valueFormatter = IndexAxisValueFormatter(dates)
                labelRotationAngle = -45f
                setDrawGridLines(true)
                gridColor = resources.getColor(android.R.color.darker_gray, null)
                axisLineColor = resources.getColor(android.R.color.white, null)
                textSize = 8f
                yOffset = 10f
            }

            // Left Y-axis styling
            axisLeft.apply {
                textColor = resources.getColor(android.R.color.white, null)
                setDrawGridLines(true)
                gridColor = resources.getColor(android.R.color.darker_gray, null)
                axisMinimum = 0f
                granularity = 1f
                setDrawZeroLine(true)
                zeroLineColor = resources.getColor(android.R.color.white, null)
                axisLineColor = resources.getColor(android.R.color.white, null)
                textSize = 8f
                axisLineWidth = 1f
                labelCount = 6
                setPosition(YAxis.YAxisLabelPosition.OUTSIDE_CHART)
            }

            // Right Y-axis styling
            axisRight.isEnabled = false

            // Legend styling
            legend.apply {
                textColor = resources.getColor(android.R.color.white, null)
                textSize = 10f
                verticalAlignment = Legend.LegendVerticalAlignment.TOP
                horizontalAlignment = Legend.LegendHorizontalAlignment.RIGHT
                orientation = Legend.LegendOrientation.HORIZONTAL
                setDrawInside(false)
                yOffset = 10f
                xOffset = 10f
                form = Legend.LegendForm.SQUARE
                formSize = 8f
                formLineWidth = 1f
            }

            // Spacing around the chart
            setExtraOffsets(10f, 10f, 10f, 25f)

            // Animation
            animateY(1000)

            // Set the visible range
            setVisibleXRangeMaximum(7f)
            moveViewToX(data.entryCount.toFloat())

            // Refresh the chart
            invalidate()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}