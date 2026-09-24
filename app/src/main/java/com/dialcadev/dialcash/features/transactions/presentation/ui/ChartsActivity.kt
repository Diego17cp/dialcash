package com.dialcadev.dialcash.features.transactions.presentation.ui

import android.app.DatePickerDialog
import android.content.res.Configuration
import android.icu.util.Calendar
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.dialcadev.dialcash.R
import com.dialcadev.dialcash.core.datastore.UserDataStore
import com.dialcadev.dialcash.core.models.UserPreferences
import com.dialcadev.dialcash.core.theme.AppTypography
import com.dialcadev.dialcash.core.ui.components.LiquidAppBar
import com.dialcadev.dialcash.databinding.ChartsActivityBinding
import com.dialcadev.dialcash.features.accounts.presentation.adapter.SelectorAccountAdapter
import com.dialcadev.dialcash.features.transactions.presentation.adapters.TransactionsAdapter
import com.dialcadev.dialcash.features.transactions.presentation.viewmodels.ChartsViewModel
import com.github.AAChartModel.AAChartCore.AAChartCreator.AAChartModel
import com.github.AAChartModel.AAChartCore.AAChartCreator.AASeriesElement
import com.github.AAChartModel.AAChartCore.AAChartEnum.AAChartType
import com.github.AAChartModel.AAChartCore.AAOptionsModel.AAStyle
import com.google.android.material.bottomsheet.BottomSheetDialog
import dagger.hilt.android.AndroidEntryPoint
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale
import javax.inject.Inject

@AndroidEntryPoint
class ChartsActivity : AppCompatActivity() {
    private lateinit var binding: ChartsActivityBinding
    private val hazeState = HazeState()
    private val viewModel: ChartsViewModel by viewModels()
    private lateinit var transactionsAdapter: TransactionsAdapter

    @Inject
    lateinit var userDataStore: UserDataStore
    private var userPreferences: UserPreferences? = null

    private val locale = Locale(
        System.getProperty("user.language") ?: "en", System.getProperty("user.language") ?: "en"
    )
    private val monthFormat = SimpleDateFormat("MMMM yyyy", locale)
    private val dateFormat = SimpleDateFormat("dd/MM/yyyy", locale)
    private val textDateFormat = SimpleDateFormat("dd MMMM yyyy", locale)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        WindowCompat.setDecorFitsSystemWindows(window, false)
        binding = ChartsActivityBinding.inflate(layoutInflater)
        val composeView = ComposeView(this).apply {
            setContent {
                MaterialTheme(
                    typography = AppTypography
                ) {
                    var appBarHeightPx by remember { mutableIntStateOf(0) }
                    val extraTopPaddingPx = (12 * resources.displayMetrics.density).toInt()

                    Box(Modifier.fillMaxSize()) {
                        AndroidView(
                            factory = { binding.root },
                            modifier = Modifier
                                .fillMaxSize()
                                .hazeSource(hazeState),
                            update = {
                                val nested = binding.nestedScrollView
                                val desiredTop = appBarHeightPx + extraTopPaddingPx
                                if (nested.paddingTop != desiredTop) {
                                    nested.updatePadding(top = desiredTop)
                                }
                            })
                        LiquidAppBar(
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .onGloballyPositioned { coordinates ->
                                    val measured = coordinates.size.height
                                    if (measured != appBarHeightPx) {
                                        appBarHeightPx = measured
                                    }
                                },
                            hazeState = hazeState,
                            title = getString(R.string.charts),
                            onBackClick = { onBackPressedDispatcher.onBackPressed() })
                    }
                }
            }
        }
        setContentView(composeView)
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, 0, systemBars.right, systemBars.bottom)
            insets
        }

        setupTransactionsRecyclerView()
        setupListeners()
        observeState()
    }

    private fun setupTransactionsRecyclerView() {
        transactionsAdapter = TransactionsAdapter(
            onTransactionClick = {}, currencySymbol = "$"
        )
        binding.recyclerViewTransactions.apply {
            layoutManager = LinearLayoutManager(this@ChartsActivity)
            adapter = transactionsAdapter
        }
    }

    private fun setupListeners() {
        binding.btnPrevMonth.setOnClickListener { viewModel.shiftMonth(-1) }
        binding.btnNextMonth.setOnClickListener { viewModel.shiftMonth(1) }
        binding.accountSelector.setOnClickListener { showAccountSelector() }
        binding.dateSelector.setOnClickListener { showDatePicker() }
        binding.etDate.setOnClickListener { showDatePicker() }
        binding.btnClearSearch.setOnClickListener {
            viewModel.clearSnapshotSearch()
            binding.tvAccountName.text = getString(R.string.select_valid_acc)
            binding.tvDateValue.text = getString(R.string.today)
            binding.etDate.setText("")
        }

        binding.btnViewAllTransactions.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
    }

    private fun showDatePicker() {
        val calendar = Calendar.getInstance()
        val currentSelected = viewModel.uiState.value.selectedDate
        if (currentSelected != null) calendar.timeInMillis = currentSelected
        DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->
                val newCalendar = Calendar.getInstance().apply { set(year, month, dayOfMonth) }
                val newTimestamp = newCalendar.timeInMillis

                binding.etDate.setText(dateFormat.format(newCalendar.time))
                binding.tvDateValue.text =
                    textDateFormat.format(newCalendar.time).replaceFirstChar { it.uppercase() }

                viewModel.selectDate(newTimestamp)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun showAccountSelector() {
        val dialog = BottomSheetDialog(this)
        val view = LayoutInflater.from(this).inflate(R.layout.accounts_selector_bottomsheet, null)
        val rv = view.findViewById<RecyclerView>(R.id.rvAccounts)

        rv.layoutManager = LinearLayoutManager(this)
        val stateAccounts = viewModel.uiState.value.accountsList

        val adapter = SelectorAccountAdapter(
            onClick = { selected ->
                viewModel.selectAccount(selected.id, selected.name)
                binding.tvAccountName.text = selected.name
                dialog.dismiss()
            }, currencySymbol = userPreferences?.currencySymbol ?: "$"
        )

        rv.adapter = adapter
        adapter.submitList(stateAccounts)
        dialog.setContentView(view)
        dialog.show()
    }

    private fun setupChart(income: Float, expense: Float, transfer: Float) {
        val isDarkMode =
            (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
        val textColor = if (isDarkMode) "#FFFFFF" else "#333333"

        val typedValue = android.util.TypedValue()
        theme.resolveAttribute(com.google.android.material.R.attr.colorSurface, typedValue, true)
        val surfaceColorHex = String.format("#%06X", 0xFFFFFF and typedValue.data)

        val model =
            AAChartModel().chartType(AAChartType.Pie).title("").backgroundColor(surfaceColorHex)
                .dataLabelsEnabled(true).dataLabelsStyle(AAStyle().color(textColor))
                .legendEnabled(true).colorsTheme(arrayOf("#4CAF50", "#FF4D4D", "#2196F3")).series(
                    arrayOf(
                        AASeriesElement().name(getString(R.string.til_amount)).innerSize("60%")
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

    private fun observeState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    userDataStore.getUserData().collectLatest { user ->
                        userPreferences = user
                        transactionsAdapter.updateCurrencySymbol(user.currencySymbol)
                    }
                }
                launch {
                    viewModel.uiState.collect { state ->
                        binding.tvMonth.text = monthFormat.format(state.currentMonthTimestamp)
                            .replaceFirstChar { it.uppercase() }
                        if (state.isLoading) {
                            binding.progressBar.visibility = View.VISIBLE
                            binding.contentLayout.visibility = View.GONE
                            binding.emptyStateLayout.visibility = View.GONE
                            binding.errorStateLayout.visibility = View.GONE
                        } else if (state.errorMessage != null) {
                            binding.progressBar.visibility = View.GONE
                            binding.contentLayout.visibility = View.GONE
                            binding.emptyStateLayout.visibility = View.GONE
                            binding.errorStateLayout.visibility = View.VISIBLE
                            binding.tvErrorMessage.text = state.errorMessage
                        } else if (state.monthTransactions.isEmpty()) {
                            binding.progressBar.visibility = View.GONE
                            binding.contentLayout.visibility = View.GONE
                            binding.emptyStateLayout.visibility = View.VISIBLE
                            binding.errorStateLayout.visibility = View.GONE
                        } else {
                            binding.progressBar.visibility = View.GONE
                            binding.contentLayout.visibility = View.VISIBLE
                            binding.emptyStateLayout.visibility = View.GONE
                            binding.errorStateLayout.visibility = View.GONE

                            setupChart(state.totalIncome, state.totalExpense, state.totalTransfer)
                        }
                        if (state.selectedAccountId != null && state.selectedDate != null) {
                            binding.layoutDateData.visibility = View.VISIBLE
                            binding.btnClearSearch.visibility = View.VISIBLE

                            val balance = state.snapshotBalance ?: 0.0
                            binding.tvBalance.text = "${userPreferences?.currencySymbol ?: "$"} ${
                                String.format(
                                    "%.2f", balance
                                )
                            }"

                            transactionsAdapter.submitList(state.snapshotTransactions)
                            if (state.snapshotTransactions.isEmpty()) {
                                binding.layoutTransactions.visibility = View.GONE
                                binding.layoutNoInfo.visibility = View.VISIBLE
                            } else {
                                binding.layoutTransactions.visibility = View.VISIBLE
                                binding.layoutNoInfo.visibility = View.GONE
                            }
                        } else {
                            binding.layoutDateData.visibility = View.GONE
                            binding.btnClearSearch.visibility = View.GONE
                        }
                    }
                }
            }
        }
    }
}