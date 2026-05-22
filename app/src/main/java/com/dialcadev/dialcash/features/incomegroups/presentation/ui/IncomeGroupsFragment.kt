package com.dialcadev.dialcash.features.incomegroups.presentation.ui

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.dialcadev.dialcash.R
import com.dialcadev.dialcash.core.datastore.UserDataStore
import com.dialcadev.dialcash.core.models.UserPreferences
import com.dialcadev.dialcash.core.ui.components.showIncomeGroupDetailsBottomSheet
import com.dialcadev.dialcash.databinding.FragmentIncomesBinding
import com.dialcadev.dialcash.features.incomegroups.presentation.adapters.IncomeGroupsAdapter
import com.dialcadev.dialcash.features.incomegroups.presentation.viewmodels.AllIncomeGroupsViewModel
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class IncomeGroupsFragment : Fragment() {
    private var _binding: FragmentIncomesBinding? = null
    private val binding get() = _binding!!
    private val viewModel: AllIncomeGroupsViewModel by viewModels()
    private lateinit var incomeGroupsAdapter: IncomeGroupsAdapter

    @Inject
    lateinit var userDataStore: UserDataStore
    var preferences: UserPreferences? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentIncomesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupSwipeToRefresh()
        setupRecyclerView()
        setupListeners()
        observeState()
    }
    private fun setupSwipeToRefresh() {
        binding.swipeRefreshLayout.setOnRefreshListener {
            viewModel.refreshIncomeGroups()
        }
    }
    private fun setupRecyclerView() {
        incomeGroupsAdapter = IncomeGroupsAdapter(
            onIncomeClick = { incomeGroup ->
                requireContext().showIncomeGroupDetailsBottomSheet(
                    incomeGroup = incomeGroup,
                    currencySymbol = preferences?.currencySymbol ?: "$",
                    onUpdate = { updated -> viewModel.updateIncomeGroup(updated) },
                    onDelete = { toDelete -> viewModel.deleteIncomeGroup(toDelete) }
                )
            },
            currencySymbol = preferences?.currencySymbol ?: "$"
        )
        binding.recyclerViewIncomes.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = incomeGroupsAdapter
        }
    }
    private fun setupListeners() {
        binding.btnNewIncome.setOnClickListener {
            val intent = Intent(
                requireContext(),
                NewIncomeGroupActivity::class.java
            )
            startActivity(intent)
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
        lifecycleScope.launch {
            userDataStore.getUserData().collect { userPreferences ->
                preferences = userPreferences
                incomeGroupsAdapter.updateCurrencySymbol(preferences?.currencySymbol ?: "$")
            }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    binding.swipeRefreshLayout.isRefreshing = state.isLoading
                    binding.progressBar.visibility =
                        if (state.isLoading && !binding.swipeRefreshLayout.isRefreshing) View.VISIBLE else View.GONE
                    incomeGroupsAdapter.submitList(state.incomeGroups)
                    updateEmptyState(state.incomeGroups.isEmpty())
                    state.errorMessage?.let { error ->
                        Snackbar.make(
                            binding.root, error, Snackbar.LENGTH_LONG
                        ).setAction(getString(R.string.retry)) { viewModel.refreshIncomeGroups() }
                            .show()
                        viewModel.clearError()
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