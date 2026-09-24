package com.dialcadev.dialcash.features.transactions.presentation.ui

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.unit.dp
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.dialcadev.dialcash.R
import com.dialcadev.dialcash.core.datastore.UserDataStore
import com.dialcadev.dialcash.core.models.UserPreferences
import com.dialcadev.dialcash.core.theme.AppTypography
import com.dialcadev.dialcash.core.ui.UiChromeViewModel
import com.dialcadev.dialcash.core.ui.components.AppBarAction
import com.dialcadev.dialcash.core.ui.components.showTransactionDetailsBottomSheet
import com.dialcadev.dialcash.core.ui.components.showTransactionFiltersBottomSheet
import com.dialcadev.dialcash.databinding.FragmentTransactionsBinding
import com.dialcadev.dialcash.features.transactions.presentation.ui.components.TransactionCard
import com.dialcadev.dialcash.features.transactions.presentation.viewmodels.AllTransactionsViewModel
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.viewinterop.AndroidView
import androidx.recyclerview.widget.LinearLayoutManager
import com.dialcadev.dialcash.core.ui.components.GlassIconButton
import com.dialcadev.dialcash.core.utils.extensions.toCurrencyFormat
import com.dialcadev.dialcash.features.transactions.presentation.adapters.TransactionsAdapter
import java.text.SimpleDateFormat
import java.util.Locale
import javax.inject.Inject

@AndroidEntryPoint
class TransactionsFragment : Fragment() {
    private val chromeViewModel: UiChromeViewModel by activityViewModels()
    private val ownerTag = "TransactionsFragment"

    private var _binding: FragmentTransactionsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AllTransactionsViewModel by viewModels()

    @Inject
    lateinit var userDataStore: UserDataStore
    private var preferences: UserPreferences? = null
    private val dateFormat = SimpleDateFormat("dd MMM", Locale.getDefault())
    private val bottomPaddingState = mutableStateOf(0.dp)


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTransactionsBinding.inflate(inflater, container, false)
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                MaterialTheme {
                    Box(modifier = Modifier.fillMaxSize()) {
                        AndroidView(
                            factory = { binding.root },
                            modifier = Modifier.fillMaxSize()
                        )
                        val state by viewModel.uiState.collectAsState()
                        if (state.isFiltered) {
                            GlassIconButton (
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(end = 16.dp, bottom = bottomPaddingState.value + 16.dp),
                                iconRes = R.drawable.ic_clear_filters,
                                contentDescription = "Clear filters",
                                size = 56.dp,
                                iconSize = 28.dp,
                                standalone = true,
                                onClick = { viewModel.clearFilters() }
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.composeTransactionsList.apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                MaterialTheme(typography = AppTypography) {
                    val state by viewModel.uiState.collectAsState()
                    val userPreferences by userDataStore.getUserData().collectAsState(initial = null)
                    val currencySymbol = userPreferences?.currencySymbol ?: "$"

                    if (state.filteredTransactions.isEmpty()) {
                    } else {
                        LazyColumn (
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(
                                top = 8.dp,
                                bottom = 8.dp
                            ),
                            verticalArrangement = Arrangement.spacedBy(0.dp)
                        ) {
                            items(
                                items = state.filteredTransactions,
                                key = { it.id }
                            ) { transaction ->
                                val amountColor = when (transaction.type) {
                                    "income" -> Color(0xFF26E6A4)
                                    "expense" -> Color(0xFFFF4D6D)
                                    "transfer" -> Color(0xFF00B4D8)
                                    else -> MaterialTheme.colorScheme.primary
                                }

                                val date = dateFormat.format(transaction.date)


                                val iconRes = when (transaction.type) {
                                    "income" -> R.drawable.ic_income
                                    "expense" -> R.drawable.ic_expense
                                    "transfer" -> R.drawable.ic_transactions_outline
                                    else -> R.drawable.ic_transactions_outline
                                }

                                val metaText = buildString {
                                    append(transaction.accountName ?: "")
                                    if (!transaction.accountName.isNullOrBlank()) {
                                        append(" • ")
                                    }
                                    append(date ?: "")
                                }
                                TransactionCard(
                                    title = transaction.description ?: "No Description",
                                    meta = metaText,
                                    amountText = "$currencySymbol ${transaction.amount.toCurrencyFormat()}",
                                    amountColor = amountColor,
                                    iconRes = iconRes,
                                    onClick = {
                                        val currentState = viewModel.uiState.value
                                        requireContext().showTransactionDetailsBottomSheet(
                                            transaction = transaction,
                                            accounts = currentState.accounts,
                                            incomeGroups = currentState.incomeGroups,
                                            currencySymbol = currencySymbol,
                                            onUpdate = { updated -> viewModel.updateTransaction(updated) },
                                            onDelete = { toDelete -> viewModel.deleteTransaction(toDelete) },
                                            onValidateBalance = { transactionId, type, accountId, amount, accountToId, incomeGroupId, onResult ->
                                                viewModel.validateTransactionBalance(
                                                    transactionId,
                                                    type,
                                                    accountId,
                                                    amount,
                                                    accountToId,
                                                    incomeGroupId,
                                                    onResult
                                                )
                                            }
                                        )
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                combine(
                    chromeViewModel.topBarHeightPx,
                    chromeViewModel.bottomBarHeightPx
                ) { top, bottom -> top to bottom }
                    .collect { (top, bottom) ->
                        val density = resources.displayMetrics.density
                        val bottomDp = (bottom / density).dp
                        bottomPaddingState.value = bottomDp
                        binding.composeTransactionsList.updatePadding(
                            top = top,
                            bottom = bottom
                        )
                    }
            }
        }
        setupSwipeToRefresh()
        observeState()
    }

    override fun onResume() {
        super.onResume()
        chromeViewModel.setExtraActions(
            ownerTag,
            listOf(
                AppBarAction(
                    iconRes = R.drawable.ic_menu_dots,
                    contentDescription = getString(R.string.options),
                    subActions = listOf(
                        AppBarAction(
                            iconRes = R.drawable.ic_filter_list,
                            contentDescription = getString(R.string.filters),
                            onClick = { onOpenFilters() }
                        ),
                        AppBarAction(
                            iconRes = R.drawable.ic_chart,
                            contentDescription = getString(R.string.view_statistics),
                            onClick =
                                {
                                    startActivity(
                                        Intent(
                                            requireContext(),
                                            ChartsActivity::class.java
                                        )
                                    )
                                })
                    ),
                )
            )
        )
    }

    override fun onPause() {
        chromeViewModel.clearExtraActions(ownerTag)
        super.onPause()
    }

    private fun onOpenFilters() {
        val state = viewModel.uiState.value
        requireContext().showTransactionFiltersBottomSheet(
            currentFilters = state.currentFilters,
            accounts = state.accounts,
            onApplyFilters = { newFilters ->
                viewModel.updateFilters(newFilters)
            }
        )
    }

    private fun setupSwipeToRefresh() {
        binding.swipeRefreshLayout.setOnRefreshListener {
            binding.swipeRefreshLayout.isRefreshing = false
        }
    }
    private fun updateEmptyState(transactionsEmpty: Boolean, isFiltered: Boolean) {
        binding.layoutTransactions.isVisible = !transactionsEmpty
        binding.layoutNoTransactions.isVisible = transactionsEmpty
        if (transactionsEmpty) {
            binding.tvEmptyTitle.text =
                if (isFiltered) getString(R.string.no_results) else getString(R.string.no_transactions_yet)
            binding.tvEmptySubtitle.text =
                if (isFiltered) getString(R.string.try_adjusting_filters) else getString(R.string.add_your_first_transaction)
        }
    }

    private fun observeState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    userDataStore.getUserData().collectLatest { userPreferences ->
                        preferences = userPreferences
                    }
                }
                launch {
                    viewModel.uiState.collect { state ->
                        binding.progressBar.isVisible =
                            state.isLoading && state.errorMessage == null
                        if (!state.isLoading) binding.swipeRefreshLayout.isRefreshing = false
                        updateEmptyState(state.filteredTransactions.isEmpty(), state.isFiltered)
                        state.errorMessage?.let { error ->
                            Snackbar.make(binding.root, error, Snackbar.LENGTH_LONG)
                                .setAction(getString(R.string.retry)) { viewModel.clearError() }
                                .show()
                            viewModel.clearError()
                        }
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}