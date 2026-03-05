package com.discordnotificationlogger.ui.stats

import android.graphics.Color
import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.discordnotificationlogger.R
import com.discordnotificationlogger.databinding.FragmentStatsBinding
import com.discordnotificationlogger.viewmodel.NotificationViewModel
import com.github.mikephil.charting.animation.Easing
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.utils.ColorTemplate

class StatsFragment : Fragment() {

    private var _binding: FragmentStatsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: NotificationViewModel by activityViewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentStatsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupChart()
        observeData()

        binding.btnRefreshStats.setOnClickListener {
            viewModel.computeWordFrequency()
        }

        viewModel.computeWordFrequency()
    }

    private fun setupChart() {
        binding.barChart.apply {
            setBackgroundColor(requireContext().getColor(R.color.discord_bg_secondary))
            setDrawGridBackground(false)
            description.isEnabled = false
            setDrawBarShadow(false)
            setDrawValueAboveBar(true)
            setPinchZoom(false)
            setScaleEnabled(true)
            isDragEnabled = true
            legend.isEnabled = false

            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                setDrawGridLines(false)
                granularity = 1f
                textColor = requireContext().getColor(R.color.discord_text_secondary)
                textSize = 10f
                labelRotationAngle = -45f
            }

            axisLeft.apply {
                setDrawGridLines(true)
                gridColor = Color.parseColor("#3A3D43")
                textColor = requireContext().getColor(R.color.discord_text_secondary)
                axisMinimum = 0f
            }

            axisRight.isEnabled = false
        }
    }

    private fun observeData() {
        viewModel.wordFrequency.observe(viewLifecycleOwner) { wordMap ->
            if (wordMap.isEmpty()) {
                binding.tvNoData.visibility = View.VISIBLE
                binding.barChart.visibility = View.GONE
                return@observe
            }

            binding.tvNoData.visibility = View.GONE
            binding.barChart.visibility = View.VISIBLE

            val entries = wordMap.entries.mapIndexed { index, entry ->
                BarEntry(index.toFloat(), entry.value.toFloat())
            }

            val labels = wordMap.keys.toList()

            val dataSet = BarDataSet(entries, "Word Frequency").apply {
                colors = listOf(
                    Color.parseColor("#5865F2"),
                    Color.parseColor("#57F287"),
                    Color.parseColor("#FEE75C"),
                    Color.parseColor("#EB459E"),
                    Color.parseColor("#ED4245"),
                    Color.parseColor("#5865F2"),
                    Color.parseColor("#57F287"),
                    Color.parseColor("#FEE75C"),
                    Color.parseColor("#EB459E"),
                    Color.parseColor("#ED4245"),
                    Color.parseColor("#5865F2"),
                    Color.parseColor("#57F287"),
                    Color.parseColor("#FEE75C"),
                    Color.parseColor("#EB459E"),
                    Color.parseColor("#ED4245")
                )
                valueTextColor = requireContext().getColor(R.color.discord_text_primary)
                valueTextSize = 10f
            }

            val barData = BarData(dataSet).apply {
                barWidth = 0.7f
            }

            binding.barChart.apply {
                data = barData
                xAxis.valueFormatter = IndexAxisValueFormatter(labels)
                xAxis.labelCount = labels.size
                animateY(800, Easing.EaseInOutQuad)
                invalidate()
            }
        }

        viewModel.allNotificationsLive.observe(viewLifecycleOwner) { notifs ->
            binding.tvTotalLogged.text = "Total logged: ${notifs?.size ?: 0}"
        }

        viewModel.keywordNotificationsLive.observe(viewLifecycleOwner) { notifs ->
            binding.tvKeywordMatches.text = "Keyword matches: ${notifs?.size ?: 0}"
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.computeWordFrequency()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
