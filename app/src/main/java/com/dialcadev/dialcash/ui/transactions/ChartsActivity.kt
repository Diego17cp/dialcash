package com.dialcadev.dialcash.ui.transactions

import android.graphics.Color
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.toColorInt
import com.dialcadev.dialcash.R
import com.dialcadev.dialcash.databinding.ChartsActivityBinding
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.listener.OnChartValueSelectedListener
import dagger.hilt.android.AndroidEntryPoint
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@AndroidEntryPoint
class ChartsActivity : AppCompatActivity() {
    private lateinit var binding: ChartsActivityBinding
    private val viewModel: TransactionsViewModel by viewModels()

    object ChartColors {
        val EXPENSE = "#FF4D4D".toColorInt()
        val INCOME = "#4CAF50".toColorInt()
        val TRANSFER = "#2196F3".toColorInt()
    }

    private val currentMonth = Calendar.getInstance().apply {
        set(Calendar.DAY_OF_MONTH, 1)
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ChartsActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setHomeAsUpIndicator(android.R.drawable.ic_menu_close_clear_cancel)
        }
        setupListeners()
        setupObservers()
        updateMonthUI()
    }
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressedDispatcher.onBackPressed()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
    private fun setupObservers() {
        viewModel.forChartTransactions.observe(this) { transactions ->
            if (transactions.isEmpty()) {
                showEmptyState()
            } else {
                val totalIncome = transactions.filter { it.type == "income" }
                    .sumOf { it.amount }.toFloat()
                val totalExpense = transactions.filter { it.type == "expense" }
                    .sumOf { it.amount }.toFloat()
                val totalTransfer = transactions.filter { it.type == "transfer" }
                    .sumOf { it.amount }.toFloat()

                setupChart(totalIncome, totalExpense, totalTransfer)
                showContent()
            }
        }

        viewModel.isLoading.observe(this) { isLoading ->
            if (isLoading) showLoading() else hideLoading()
        }
        viewModel.errorMessage.observe(this) { errorMessage ->
            errorMessage?.let {
                showError(it)
            }
        }
    }

    private fun showContent() {
        binding.contentLayout.visibility = View.VISIBLE
        binding.progressBar.visibility = View.GONE
        binding.emptyStateLayout.visibility = View.GONE
        binding.errorStateLayout.visibility = View.GONE
    }

    private fun showLoading() {
        binding.contentLayout.visibility = View.GONE
        binding.progressBar.visibility = View.VISIBLE
        binding.emptyStateLayout.visibility = View.GONE
        binding.errorStateLayout.visibility = View.GONE
    }

    private fun hideLoading() {
        binding.progressBar.visibility = View.GONE
    }

    private fun showEmptyState() {
        binding.contentLayout.visibility = View.GONE
        binding.progressBar.visibility = View.GONE
        binding.emptyStateLayout.visibility = View.VISIBLE
        binding.errorStateLayout.visibility = View.GONE
    }

    private fun showError(message: String) {
        binding.contentLayout.visibility = View.GONE
        binding.progressBar.visibility = View.GONE
        binding.emptyStateLayout.visibility = View.GONE
        binding.errorStateLayout.visibility = View.VISIBLE
        binding.tvErrorMessage.text = message
    }

    private fun setupListeners() {
        binding.btnPrevMonth.setOnClickListener {
            currentMonth.add(Calendar.MONTH, -1)
            updateMonthUI()
        }

        binding.btnNextMonth.setOnClickListener {
            currentMonth.add(Calendar.MONTH, 1)
            updateMonthUI()
        }

        binding.btnRetry.setOnClickListener {
            loadChartData()
        }
    }

    private fun updateMonthUI() {
        val monthFormat = SimpleDateFormat("MMMM yyyy", Locale(System.getProperty("user.language") ?: "en"))
        binding.tvMonth.text = monthFormat.format(currentMonth.time)
            .replaceFirstChar { it.uppercase() }

        loadChartData()
    }

    private fun setupChart(income: Float, expense: Float, transfer: Float) {
        val entriesIncome = BarEntry(0f, income)
        val entriesExpense = BarEntry(1f, expense)
        val entriesTransfer = BarEntry(2f, transfer)

        val incomeSet = BarDataSet(listOf(entriesIncome), "").apply {
            color = ChartColors.INCOME
            valueTextColor = Color.TRANSPARENT
        }

        val expenseSet = BarDataSet(listOf(entriesExpense), "").apply {
            color = ChartColors.EXPENSE
            valueTextColor = Color.TRANSPARENT
        }

        val transferSet = BarDataSet(listOf(entriesTransfer), "").apply {
            color = ChartColors.TRANSFER
            valueTextColor = Color.TRANSPARENT
        }

        val data = BarData(incomeSet, expenseSet, transferSet)
        data.barWidth = 0.5f

        binding.barChart.apply {
            this.data = data
            description.isEnabled = false
            legend.isEnabled = false
            axisRight.isEnabled = false

            axisLeft.apply {
                textColor = getColor(R.color.text_secondary)
                gridColor = Color.TRANSPARENT
                axisMinimum = 0f
            }

            xAxis.apply {
                valueFormatter = IndexAxisValueFormatter(
                    listOf(
                        getString(R.string.incomes),
                        getString(R.string.expenses),
                        getString(R.string.transfers)
                    )
                )
                position = XAxis.XAxisPosition.BOTTOM
                textColor = getColor(R.color.text_secondary)
                setDrawGridLines(false)
                granularity = 1f
                labelCount = 3
            }
            setDrawValueAboveBar(true)
            setTouchEnabled(true)
            setOnChartValueSelectedListener(object : OnChartValueSelectedListener {
                override fun onValueSelected(e: Entry?, h: Highlight?) {
                    e?.let {
                        val value = it.y
                        val type = when (it.x.toInt()) {
                            0 -> getString(R.string.incomes)
                            1 -> getString(R.string.expenses)
                            2 -> getString(R.string.transfers)
                            else -> ""
                        }
                        Toast.makeText(
                            this@ChartsActivity,
                            "$type: ${String.format("%.2f", value)}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
                override fun onNothingSelected() {
                }
            })
            animateY(600)
            invalidate()
        }
    }

    private fun loadChartData() {
        val startDate = currentMonth.timeInMillis

        val endDate = Calendar.getInstance().apply {
            timeInMillis = currentMonth.timeInMillis
            set(Calendar.DAY_OF_MONTH, getActualMaximum(Calendar.DAY_OF_MONTH))
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }.timeInMillis
        viewModel.fetchTransactionsBetweenDates(startDate, endDate)
    }
}