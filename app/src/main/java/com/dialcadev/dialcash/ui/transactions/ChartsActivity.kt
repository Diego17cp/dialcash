package com.dialcadev.dialcash.ui.transactions

import android.app.DatePickerDialog
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.toColorInt
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.dialcadev.dialcash.R
import com.dialcadev.dialcash.data.UserData
import com.dialcadev.dialcash.data.UserDataStore
import com.dialcadev.dialcash.data.dao.AccountBalanceWithOriginal
import com.dialcadev.dialcash.data.entities.Account
import com.dialcadev.dialcash.databinding.ChartsActivityBinding
import com.dialcadev.dialcash.ui.accounts.SelectorAccountAdapter
import com.github.AAChartModel.AAChartCore.AAChartCreator.AAChartModel
import com.github.AAChartModel.AAChartCore.AAChartCreator.AASeriesElement
import com.github.AAChartModel.AAChartCore.AAChartEnum.AAChartStackingType
import com.github.AAChartModel.AAChartCore.AAChartEnum.AAChartType
import com.github.AAChartModel.AAChartCore.AAOptionsModel.AAStyle
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.listener.OnChartValueSelectedListener
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.datepicker.MaterialDatePicker
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import javax.inject.Inject

@AndroidEntryPoint
class ChartsActivity : AppCompatActivity() {
    @Inject
    lateinit var userDataStore: UserDataStore
    private var userData: UserData? = null
    private lateinit var binding: ChartsActivityBinding
    private lateinit var transactionsAdapter: TransactionsAdapter
    private val viewModel: TransactionsViewModel by viewModels()
    private var accountsList: List<AccountBalanceWithOriginal> = emptyList()
    private var selectedAccountId: Int? = null
    private var selectedDate: Long? = null

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

        lifecycleScope.launch {
            userDataStore.getUserData().collect { user -> userData = user }
        }

        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setHomeAsUpIndicator(android.R.drawable.ic_menu_close_clear_cancel)
        }
        setupListeners()
        setupTransactionsRecyclerView()
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
    private fun setupTransactionsRecyclerView() {
        transactionsAdapter = TransactionsAdapter(
            onTransactionClick = { transaction ->
                // Handle transaction click if needed
            },
            currencySymbol = userData?.currencySymbol ?: "$"
        )
        binding.recyclerViewTransactions.apply {
            layoutManager = LinearLayoutManager(this@ChartsActivity)
            adapter = transactionsAdapter
        }
    }
    private fun setupObservers() {
        viewModel.accountBalances.observe(this) { accounts ->
            accountsList = accounts
            if (accounts.isNotEmpty() && selectedAccountId == null) {
                val firstAccount = accounts.first()
                selectedAccountId = firstAccount.id
                binding.tvAccountName.text = firstAccount.name
            }
        }
        viewModel.specificDateBalance.observe(this) { balance ->
            balance.let {
                val formattedBalance = "${userData?.currencySymbol ?: "$"} ${String.format("%.2f", it)}"
                binding.tvBalance.text = formattedBalance
            }
        }
        viewModel.specificDateTransactions.observe(this) { transactions ->
            transactionsAdapter.submitList(transactions)
            if (transactions.isEmpty()) {
                binding.layoutTransactions.visibility = View.GONE
                binding.layoutNoInfo.visibility = View.VISIBLE
            } else {
                binding.layoutTransactions.visibility = View.VISIBLE
                binding.layoutNoInfo.visibility = View.GONE
            }
        }
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
    private fun checkAndFetchDateBalance() {
        val accountId = selectedAccountId
        val date = selectedDate
        if (accountId != null && date != null) {
            viewModel.fetchBalanceAtDate(accountId, date)
            binding.layoutDateData.visibility = View.VISIBLE
            binding.btnClearSearch.visibility = View.VISIBLE
        } else {
            binding.layoutDateData.visibility = View.GONE
            binding.btnClearSearch.visibility = View.GONE
        }
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
        binding.accountSelector.setOnClickListener { showAccountSelector() }
        binding.dateSelector.setOnClickListener { showDatePicker() }
        binding.etDate.setOnClickListener { showDatePicker() }
        binding.btnClearSearch.setOnClickListener {
            selectedAccountId = null
            selectedDate = null
            binding.tvAccountName.text = getString(R.string.select_valid_acc)
            binding.tvDateValue.text = getString(R.string.today)
            binding.btnClearSearch.visibility = View.GONE
            binding.layoutDateData.visibility = View.GONE
            viewModel.clearDateSearch()
        }
        binding.btnViewAllTransactions.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
    }
        private fun showDatePicker() {
            val calendar = Calendar.getInstance()
            val datePickerDialog = DatePickerDialog(
                this,
                { _, year, month, dayOfMonth ->
                    calendar.set(year, month, dayOfMonth)
                    selectedDate = calendar.timeInMillis
                    if (selectedAccountId != null) {
                        checkAndFetchDateBalance()
                    }
                    binding.etDate.setText(SimpleDateFormat("dd/MM/YYYY").format(calendar.time))
                    binding.tvDateValue.text = SimpleDateFormat("dd MMMM yyyy", Locale(System.getProperty("user.language") ?: "en"))
                        .format(calendar.time)
                        .replaceFirstChar { it.uppercase() }
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            )
            datePickerDialog.show()
        }

        private fun showAccountSelector() {
            val dialog = BottomSheetDialog(this)
            val view = LayoutInflater.from(this)
                .inflate(R.layout.accounts_selector_bottomsheet, null)
            val rv = view.findViewById<RecyclerView>(R.id.rvAccounts)
            rv.layoutManager = LinearLayoutManager(this)
            val adapter = SelectorAccountAdapter(
                onClick = { selected ->
                    selectedAccountId = selected.id
                    if (selectedDate != null) {
                        checkAndFetchDateBalance()
                    }
                    binding.tvAccountName.text = selected.name
                    dialog.dismiss()
                },
                currencySymbol = userData?.currencySymbol ?: "$"
            )
            rv.adapter = adapter
            adapter.submitList(accountsList)
            dialog.setContentView(view)
            dialog.show()
        }

    private fun updateMonthUI() {
        val monthFormat = SimpleDateFormat("MMMM yyyy", Locale(System.getProperty("user.language") ?: "en"))
        binding.tvMonth.text = monthFormat.format(currentMonth.time)
            .replaceFirstChar { it.uppercase() }

        loadChartData()
    }

    private fun setupChart(income: Float, expense: Float, transfer: Float) {
        val isDarkMode = (resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK) == android.content.res.Configuration.UI_MODE_NIGHT_YES
        val textColor = if (isDarkMode) "#FFFFFF" else "#333333"
        val typedValue = android.util.TypedValue()
        theme.resolveAttribute(com.google.android.material.R.attr.colorSurface, typedValue, true)
        val surfaceColorHex = String.format("#%06X", 0xFFFFFF and typedValue.data)
        val model = AAChartModel()
            .chartType(AAChartType.Pie)
            .title("")
            .backgroundColor(surfaceColorHex)
            .dataLabelsEnabled(true)
            .dataLabelsStyle(AAStyle().color(textColor))
            .legendEnabled(true)
            .colorsTheme(arrayOf("#4CAF50", "#FF4D4D", "#2196F3"))
            .series(
                arrayOf(
                    AASeriesElement()
                        .name(getString(R.string.til_amount))
                        .innerSize("60%")
                        .data(
                            arrayOf(
                                arrayOf(getString(R.string.incomes), income),
                                arrayOf(getString(R.string.expenses), expense),
                                arrayOf(getString(R.string.transfers), transfer)
                            )
                        )
                )
            )

        val options = model.aa_toAAOptions()
        options.legend?.itemStyle?.color(textColor)

        binding.speciaAreaChart.aa_drawChartWithChartOptions(options)
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