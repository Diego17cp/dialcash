package com.dialcadev.dialcash.features.accounts.presentation.ui

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
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
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.dialcadev.dialcash.R
import com.dialcadev.dialcash.core.datastore.UserDataStore
import com.dialcadev.dialcash.core.models.UserPreferences
import com.dialcadev.dialcash.core.ui.UiChromeViewModel
import com.dialcadev.dialcash.core.ui.components.GlassIconButton
import com.dialcadev.dialcash.core.ui.components.showAccountDetailsBottomSheet
import com.dialcadev.dialcash.core.utils.extensions.toCurrencyFormat
import com.dialcadev.dialcash.databinding.FragmentAccountsBinding
import com.dialcadev.dialcash.features.accounts.presentation.adapter.AccountsAdapter
import com.dialcadev.dialcash.features.accounts.presentation.ui.components.AccountCard
import com.dialcadev.dialcash.features.accounts.presentation.viewmodels.AllAccountsViewModel
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.getValue

@AndroidEntryPoint
class AccountsFragment : Fragment() {
    private val localHazeState = HazeState()
    private var _binding: FragmentAccountsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: AllAccountsViewModel by viewModels()
    private val chromeViewModel: UiChromeViewModel by activityViewModels()

    @Inject
    lateinit var userDataStore: UserDataStore
    private var preferences: UserPreferences? = null

    private val bottomPaddingState = mutableStateOf(0.dp)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAccountsBinding.inflate(inflater, container, false)
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                MaterialTheme {
                    Box(modifier = Modifier.fillMaxSize()) {
                        AndroidView(
                            factory = { binding.root },
                            modifier = Modifier
                                .fillMaxSize()
                                .hazeSource(localHazeState)
                        )
                        GlassIconButton(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(end = 16.dp, bottom = bottomPaddingState.value + 16.dp),
                            iconRes = R.drawable.ic_plus,
                            hazeState = localHazeState,
                            size = 56.dp,
                            standalone = true,
                            iconTint = colorResource(R.color.colorPrimary),
                            contentDescription = getString(R.string.new_acc_title),
                            onClick = {
                                val intent =
                                    Intent(requireContext(), NewAccountActivity::class.java)
                                startActivity(intent)
                            }
                        )
                    }
                }
            }
        }
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                combine(
                    chromeViewModel.topBarHeightPx,
                    chromeViewModel.bottomBarHeightPx
                ) { top, bottom -> top to bottom }
                    .collect { (top, bottom) ->
                        binding.composeAccountsList.updatePadding(top = top, bottom = bottom)
                        binding.layoutNoAccounts.updatePadding(top = top, bottom = bottom)
                        val density = resources.displayMetrics.density
                        bottomPaddingState.value = (bottom / density).dp
                    }
            }
        }
        setupAccountsComposeList()
        setupSwipeToRefresh()
        observeState()
    }

    private fun setupSwipeToRefresh() {
        binding.swipeRefreshLayout.setOnRefreshListener {
            viewModel.refreshAccounts()
        }
    }
    private fun setupAccountsComposeList() {
        binding.composeAccountsList.apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                MaterialTheme {
                    val state by viewModel.uiState.collectAsState()
                    val userPreferences by userDataStore.getUserData().collectAsState(initial = null)
                    val currencySymbol = userPreferences?.currencySymbol ?: "$"

                    LazyColumn (
                        contentPadding = PaddingValues(top = 8.dp, bottom = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(0.dp)
                    ) {
                        items(
                            items = state.accounts,
                            key = { it.id }
                        ) { account ->
                            val iconRes = when (account.type) {
                                "cash" -> R.drawable.ic_cash
                                "bank" -> R.drawable.ic_building_bank
                                "card" -> R.drawable.ic_card
                                "wallet" -> R.drawable.ic_accounts_filled
                                "debt" -> R.drawable.ic_debt_payment
                                "savings" -> R.drawable.ic_bank
                                else -> R.drawable.ic_account_default
                            }

                            val isMain = account.type == "bank" ||
                                    account.type == "cash" ||
                                    account.type == "wallet" ||
                                    account.type == "card"

                            val current = "$currencySymbol ${account.balance.toCurrencyFormat()}"
                            val original = getString(
                                R.string.original_balance_with_value,
                                "$currencySymbol ${account.originalBalance.toCurrencyFormat()}"
                            )

                            AccountCard(
                                title = account.name,
                                originalBalanceText = original,
                                currentBalanceText = current,
                                iconRes = iconRes,
                                isMainAccount = isMain,
                                onClick = {
                                    requireContext().showAccountDetailsBottomSheet(
                                        account = account,
                                        currencySymbol = currencySymbol,
                                        onUpdate = { updated -> viewModel.updateAccount(updated) },
                                        onDelete = { toDelete -> viewModel.deleteAccount(toDelete) }
                                    )
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    private fun updateEmptyState(accountsEmpty: Boolean) {
        if (accountsEmpty) {
            binding.layoutAccounts.visibility = View.GONE
            binding.layoutNoAccounts.visibility = View.VISIBLE
        } else {
            binding.layoutAccounts.visibility = View.VISIBLE
            binding.layoutNoAccounts.visibility = View.GONE
        }
    }

    private fun observeState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    userDataStore.getUserData().collect { userPreferences ->
                        preferences = userPreferences
                    }
                }
                launch {
                    viewModel.uiState.collect { state ->
                        binding.swipeRefreshLayout.isRefreshing = state.isLoading
                        binding.progressBar.visibility =
                            if (state.isLoading && !binding.swipeRefreshLayout.isRefreshing) View.VISIBLE else View.GONE
                        updateEmptyState(state.accounts.isEmpty())
                        state.errorMessage?.let { error ->
                            Snackbar.make(
                                binding.root, error, Snackbar.LENGTH_LONG
                            ).setAction(getString(R.string.retry)) { viewModel.refreshAccounts() }
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
        viewModel.refreshAccounts()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}