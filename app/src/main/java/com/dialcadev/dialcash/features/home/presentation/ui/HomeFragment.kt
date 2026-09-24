package com.dialcadev.dialcash.features.home.presentation.ui

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.dialcadev.dialcash.R
import com.dialcadev.dialcash.core.datastore.UserDataStore
import com.dialcadev.dialcash.core.models.UserPreferences
import com.dialcadev.dialcash.core.theme.AppTypography
import com.dialcadev.dialcash.core.ui.UiChromeViewModel
import com.dialcadev.dialcash.core.ui.components.showAccountDetailsBottomSheet
import com.dialcadev.dialcash.core.ui.components.showTransactionDetailsBottomSheet
import com.dialcadev.dialcash.core.utils.extensions.toCurrencyFormat
import com.dialcadev.dialcash.databinding.FragmentHomeBinding
import com.dialcadev.dialcash.features.accounts.presentation.adapter.MainAccountsAdapter
import com.dialcadev.dialcash.features.home.presentation.ui.components.HomeBalanceCard
import com.dialcadev.dialcash.features.home.presentation.viewmodels.HomeViewModel
import com.dialcadev.dialcash.features.transactions.presentation.adapters.RecentTransactionsAdapter
import com.dialcadev.dialcash.features.transactions.presentation.ui.NewTransactionActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import dev.chrisbanes.haze.HazeState
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class HomeFragment : Fragment() {
    private val localHazeState = HazeState()

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val viewModel: HomeViewModel by viewModels()
    private val chromeViewModel: UiChromeViewModel by activityViewModels()

    private lateinit var accountsAdapter: MainAccountsAdapter
    private lateinit var transactionsAdapter: RecentTransactionsAdapter

    @Inject
    lateinit var userDataStore: UserDataStore
    private var preferences: UserPreferences? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupBalanceCard()
        setupRecyclerViews()
        setupOnClickListeners()
        setupSwipeRefresh()
        setupChromePadding()
        observeState()
    }

    private fun setupBalanceCard() {
        binding.composeBalanceCard.apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                val userPreferences by userDataStore.getUserData().collectAsState(initial = null)
                val state by viewModel.uiState.collectAsState()

                val isVisible = userPreferences?.isBalanceVisible ?: true
                val currencySymbol = userPreferences?.currencySymbol ?: "$"

                val formattedBalance = remember(isVisible, currencySymbol, state.totalBalance) {
                    if (isVisible) {
                        "$currencySymbol ${state.totalBalance.toCurrencyFormat()}"
                    } else {
                        val balanceParts = state.totalBalance.toCurrencyFormat().split(".")
                        val decimals = if (balanceParts.size > 1) balanceParts[1] else "00"
                        "$currencySymbol ****.$decimals"
                    }
                }
                MaterialTheme(typography = AppTypography) {
                    HomeBalanceCard(
                        totalBalanceText = formattedBalance,
                        isBalanceVisible = preferences?.isBalanceVisible ?: true,
                        hazeState = localHazeState,
                        actionsEnabled = state.mainAccounts.isNotEmpty(),
                        onToggleVisibility = {
                            lifecycleScope.launch { userDataStore.toggleBalanceVisibility() }
                        },
                        onQuickIncomeClick = { navigateToTransactionType("income") },
                        onQuickExpenseClick = { navigateToTransactionType("expense") },
                        onQuickTransferClick = { navigateToTransactionType("transfer") }
                    )
                }
            }
        }
    }

    private fun setupChromePadding() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                combine(
                    chromeViewModel.topBarHeightPx,
                    chromeViewModel.bottomBarHeightPx
                ) { top, bottom -> top to bottom }
                    .collect { (top, bottom) ->
                        val extra = (8 * resources.displayMetrics.density).toInt()
                        binding.nestedScrollView.updatePadding(
                            top = top + extra,
                            bottom = bottom + extra
                        )
                    }
            }
        }
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefreshLayout.setOnRefreshListener {
            viewModel.refreshData()
        }
    }

    private fun setupRecyclerViews() {
        accountsAdapter = MainAccountsAdapter(
            onAccountClick = { account ->
                requireContext().showAccountDetailsBottomSheet(
                    account = account,
                    currencySymbol = preferences?.currencySymbol ?: "$",
                    onUpdate = { updated -> viewModel.updateAccount(updated) },
                    onDelete = { toDeleted -> viewModel.deleteAccount(toDeleted) })
            }, currencySymbol = preferences?.currencySymbol ?: "$"
        )
        binding.recyclerViewAccounts.apply {
            layoutManager = LinearLayoutManager(
                context, LinearLayoutManager.HORIZONTAL, false
            )
            adapter = accountsAdapter
        }
        transactionsAdapter = RecentTransactionsAdapter(
            onTransactionClick = { transaction ->
                val uiState = viewModel.uiState.value
                requireContext().showTransactionDetailsBottomSheet(
                    transaction = transaction,
                    currencySymbol = preferences?.currencySymbol ?: "$",
                    accounts = uiState.accounts,
                    incomeGroups = uiState.incomeGroups,
                    onUpdate = { updated -> viewModel.updateTransaction(updated) },
                    onDelete = { toDeleted -> viewModel.deleteTransaction(toDeleted) },
                    onValidateBalance = { id, type, accId, amount, accToId, incomeId, onResult ->
                        viewModel.validateTransactionBalance(
                            id,
                            type,
                            accId,
                            amount,
                            accToId,
                            incomeId,
                            onResult
                        )
                    }
                )
            }, currencySymbol = preferences?.currencySymbol ?: "$"
        )
        binding.recyclerViewTransactions.apply {
            layoutManager = LinearLayoutManager(
                context, LinearLayoutManager.VERTICAL, false
            )
            adapter = transactionsAdapter
        }
    }

    private fun setupOnClickListeners() {
        binding.btnQuickIncome.setOnClickListener { navigateToTransactionType("income") }
        binding.btnQuickExpense.setOnClickListener { navigateToTransactionType("expense") }
        binding.btnQuickTransfer.setOnClickListener { navigateToTransactionType("transfer") }

        binding.btnViewAllTransactions.setOnClickListener {
            navigateToTopLevelDestination(R.id.transactionsFragment)
        }
        binding.btnViewAllAccounts.setOnClickListener {
            navigateToTopLevelDestination(R.id.accountsFragment)
        }
        binding.btnToggleEye.setOnClickListener {
            lifecycleScope.launch { userDataStore.toggleBalanceVisibility() }
        }
    }

    private fun navigateToTopLevelDestination(destinationId: Int) {
        val nav = findNavController()
        if (nav.currentDestination?.id == destinationId) return

        val options = NavOptions.Builder()
            .setPopUpTo(nav.graph.startDestinationId, false, saveState = true)
            .setLaunchSingleTop(true)
            .setRestoreState(true)
            .build()
        nav.navigate(destinationId, null, options)
    }

    private fun updateBalanceVisibility(isVisible: Boolean, totalBalance: Double) {
        val formattedBalance = if (isVisible) {
            "${preferences?.currencySymbol ?: "$"} ${totalBalance.toCurrencyFormat()}"
        } else {
            val balanceParts = totalBalance.toCurrencyFormat().split(".")
            val decimals = if (balanceParts.size > 1) balanceParts[1] else "00"
            "${preferences?.currencySymbol ?: "$"} ****.$decimals"
        }
        binding.textTotalBalance.text = formattedBalance
        binding.btnToggleEye.setImageResource(if (!isVisible) R.drawable.ic_eye else R.drawable.ic_eye_closed)
    }

    private fun updateEmptyState(accountsEmpty: Boolean, transactionsEmpty: Boolean) {
        binding.btnQuickIncome.isEnabled = !accountsEmpty
        binding.btnQuickExpense.isEnabled = !accountsEmpty
        binding.btnQuickTransfer.isEnabled = !accountsEmpty

        binding.layoutNoInfo.visibility =
            if (accountsEmpty && transactionsEmpty) View.VISIBLE else View.GONE
        binding.layoutMainAccounts.visibility = if (accountsEmpty) View.GONE else View.VISIBLE
        binding.layoutRecentTransactions.visibility =
            if (transactionsEmpty) View.GONE else View.VISIBLE
    }

    private fun navigateToTransactionType(transactionType: String) {
        val intent = Intent(
            this.context, NewTransactionActivity::class.java
        ).apply {
            putExtra("transaction_type", transactionType)
        }
        startActivity(intent)
    }

    private fun observeState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    userDataStore.getUserData().collect { userPreferences ->
                        preferences = userPreferences
                        accountsAdapter.updateCurrencySymbol(preferences?.currencySymbol ?: "$")
                        transactionsAdapter.updateCurrencySymbol(preferences?.currencySymbol ?: "$")
                        updateBalanceVisibility(
                            isVisible = preferences?.isBalanceVisible ?: true,
                            totalBalance = viewModel.uiState.value.totalBalance
                        )
                    }
                }
                launch {
                    viewModel.uiState.collect { state ->
                        binding.swipeRefreshLayout.isRefreshing = state.isLoading
                        binding.progressBar.visibility =
                            if (state.isLoading && !binding.swipeRefreshLayout.isRefreshing) View.VISIBLE else View.GONE
                        accountsAdapter.submitList(state.mainAccounts)
                        transactionsAdapter.submitList(state.recentTransactions)
                        preferences?.let {
                            updateBalanceVisibility(
                                isVisible = it.isBalanceVisible, totalBalance = state.totalBalance
                            )
                        }
                        updateEmptyState(
                            accountsEmpty = state.mainAccounts.isEmpty(),
                            transactionsEmpty = state.recentTransactions.isEmpty()
                        )
                        state.errorMessage?.let { error ->
                            Snackbar.make(
                                binding.root, error, Snackbar.LENGTH_LONG
                            ).setAction(getString(R.string.retry)) { viewModel.refreshData() }
                                .show()
                            viewModel.clearError()
                        }
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshData()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}