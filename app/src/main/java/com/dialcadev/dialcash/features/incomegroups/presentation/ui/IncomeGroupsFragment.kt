package com.dialcadev.dialcash.features.incomegroups.presentation.ui

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
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
import androidx.core.view.updateLayoutParams
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
import com.dialcadev.dialcash.core.ui.components.showIncomeGroupDetailsBottomSheet
import com.dialcadev.dialcash.databinding.FragmentIncomesBinding
import com.dialcadev.dialcash.features.incomegroups.presentation.adapters.IncomeGroupsAdapter
import com.dialcadev.dialcash.features.incomegroups.presentation.viewmodels.AllIncomeGroupsViewModel
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.dialcadev.dialcash.core.ui.components.GlassIconButton
import com.dialcadev.dialcash.core.utils.extensions.toCurrencyFormat
import com.dialcadev.dialcash.features.accounts.presentation.ui.NewAccountActivity
import com.dialcadev.dialcash.features.incomegroups.presentation.ui.components.IncomeGroupCard
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource

@AndroidEntryPoint
class IncomeGroupsFragment : Fragment() {
    private val localHazeState = HazeState()

    private var _binding: FragmentIncomesBinding? = null
    private val binding get() = _binding!!
    private val viewModel: AllIncomeGroupsViewModel by viewModels()
    private val chromeViewModel: UiChromeViewModel by activityViewModels()

    @Inject
    lateinit var userDataStore: UserDataStore
    var preferences: UserPreferences? = null
    private val bottomPaddingState = mutableStateOf(0.dp)


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentIncomesBinding.inflate(inflater, container, false)
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
                                    Intent(requireContext(), NewIncomeGroupActivity::class.java)
                                startActivity(intent)
                            })
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
                        binding.composeIncomeGroupsList.updatePadding(top = top, bottom = bottom)
                        binding.layoutNoIncomes.updatePadding(top = top, bottom = bottom)
                        val density = resources.displayMetrics.density
                        bottomPaddingState.value = (bottom / density).dp
                    }
            }
        }
        setupIncomeGroupsComposeList()
        setupSwipeToRefresh()
        observeState()
    }
    private fun setupIncomeGroupsComposeList() {
        binding.composeIncomeGroupsList.apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                MaterialTheme {
                    val state by viewModel.uiState.collectAsState()
                    val userPreferences by userDataStore.getUserData().collectAsState(initial = null)
                    val currencySymbol = userPreferences?.currencySymbol ?: "$"

                    androidx.compose.foundation.lazy.LazyColumn(
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(top = 8.dp, bottom = 8.dp),
                        verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(0.dp)
                    ) {
                        items(
                            items = state.incomeGroups,
                            key = { it.id }
                        ) { income ->
                            val totalText = getString(
                                R.string.original_balance_with_value,
                                "$currencySymbol ${income.amount.toCurrencyFormat()}"
                            )
                            val remainingText = "$currencySymbol ${income.remaining.toCurrencyFormat()}"

                            IncomeGroupCard(
                                title = income.name,
                                totalText = totalText,
                                remainingText = remainingText,
                                iconRes = R.drawable.ic_income_item,
                                onClick = {
                                    requireContext().showIncomeGroupDetailsBottomSheet(
                                        incomeGroup = income,
                                        currencySymbol = currencySymbol,
                                        onUpdate = { updated -> viewModel.updateIncomeGroup(updated) },
                                        onDelete = { toDelete -> viewModel.deleteIncomeGroup(toDelete) }
                                    )
                                }
                            )
                        }
                    }
                }
            }
        }
    }
    private fun setupSwipeToRefresh() {
        binding.swipeRefreshLayout.setOnRefreshListener {
            viewModel.refreshIncomeGroups()
        }
    }
    private fun updateEmptyState(incomeGroupsEmpty: Boolean) {
        if (incomeGroupsEmpty) {
            binding.layoutIncomes.visibility = View.GONE
            binding.layoutNoIncomes.visibility = View.VISIBLE
        } else {
            binding.layoutIncomes.visibility = View.VISIBLE
            binding.layoutNoIncomes.visibility = View.GONE
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
                        updateEmptyState(state.incomeGroups.isEmpty())
                        state.errorMessage?.let { error ->
                            Snackbar.make(
                                binding.root, error, Snackbar.LENGTH_LONG
                            )
                                .setAction(getString(R.string.retry)) { viewModel.refreshIncomeGroups() }
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
        viewModel.refreshIncomeGroups()
    }
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}