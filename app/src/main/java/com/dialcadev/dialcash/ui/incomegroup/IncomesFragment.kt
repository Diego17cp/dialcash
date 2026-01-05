package com.dialcadev.dialcash.ui.incomegroup

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.dialcadev.dialcash.NewIncomeActivity
import com.dialcadev.dialcash.R
import com.dialcadev.dialcash.data.UserData
import com.dialcadev.dialcash.data.UserDataStore
import com.dialcadev.dialcash.databinding.FragmentIncomesBinding
import com.dialcadev.dialcash.databinding.RecycleIncomeGroupItemBinding
import com.dialcadev.dialcash.utils.toReadableDate
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class IncomesFragment : Fragment() {

    private var _binding: FragmentIncomesBinding? = null
    private val binding get() = _binding!!

    private val viewModel: IncomesViewModel by viewModels()
    private lateinit var incomesAdapter: IncomesAdapter

    @Inject
    lateinit var userDataStore: UserDataStore
    var userData: UserData? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentIncomesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupSwipeToRefresh()
        setupRecyclerView()
        setupListeners()
        observeViewModel()
    }

    private fun setupSwipeToRefresh() {
        binding.swipeRefreshLayout.setOnRefreshListener {
            viewModel.refreshIncomes()
        }
    }

    private fun setupRecyclerView() {
        incomesAdapter = IncomesAdapter(
            onIncomeClick = { income ->
                val bottomSheet = BottomSheetDialog(requireContext())
                val binding = RecycleIncomeGroupItemBinding.inflate(layoutInflater)
                binding.apply {
                    tvIncomeGroupName.text = income.name
                    etEditAccountName.setText(income.name)
                    "${userData?.currencySymbol ?: "$"} ${"%.2f".format(income.amount)}"
                        .also { tvIncomeGroupBalance.text = it }
                    etInitialBalance.setText(income.amount.toString())
                    "${userData?.currencySymbol ?: "$"} ${"%.2f".format(income.remaining)}"
                        .also { tvIncomeGroupRemaining.text = it }
                    tvCreatedAt.text = getString(R.string.created_at, income.createdAt.toReadableDate())

                    fun validateForm() {
                        val name = etEditAccountName.text.toString().trim()
                        val amountText = etInitialBalance.text.toString().trim()
                        val isNameValid = name.isNotEmpty()
                        val isAmountValid =
                            amountText.isNotEmpty() && amountText.toDoubleOrNull() != null && amountText.toDouble() >= 0.0
                        btnSave.isEnabled = isNameValid && isAmountValid
                        if (!isNameValid) {
                            tilIncomeGroupName.error = getString(R.string.name_cannot_be_empty)
                        }
                        if (!isAmountValid) {
                            tilInitialBalance.error = getString(R.string.amount_required)
                        }
                    }

                    fun resetForm() {
                        tvIncomeGroupName.visibility = View.VISIBLE
                        tilIncomeGroupName.visibility = View.GONE
                        tvIncomeGroupBalance.visibility = View.VISIBLE
                        tilInitialBalance.visibility = View.GONE
                        actionsRow.visibility = View.VISIBLE
                        editFooter.visibility = View.GONE
                        deleteConfirmFooter.visibility = View.GONE
                        etEditAccountName.error = null
                        etInitialBalance.error = null
                    }
                    etEditAccountName.addTextChangedListener { validateForm() }
                    etInitialBalance.addTextChangedListener { validateForm() }

                    btnEdit.setOnClickListener {
                        tvIncomeGroupName.visibility = View.GONE
                        tilIncomeGroupName.visibility = View.VISIBLE
                        tvIncomeGroupBalance.visibility = View.GONE
                        tilInitialBalance.visibility = View.VISIBLE
                        actionsRow.visibility = View.GONE
                        editFooter.visibility = View.VISIBLE
                        validateForm()
                    }
                    btnSave.setOnClickListener {
                        val newName = etEditAccountName.text.toString().trim()
                        val newAmount = etInitialBalance.text.toString().trim().toDoubleOrNull()
                            ?: income.amount
                        viewModel.editIncome(income.copy(name = newName, amount = newAmount))
                        resetForm()
                        bottomSheet.dismiss()
                    }
                    btnCancel.setOnClickListener {
                        resetForm()
                    }
                    btnDelete.setOnClickListener {
                        actionsRow.visibility = View.GONE
                        deleteConfirmFooter.visibility = View.VISIBLE
                    }
                    btnConfirmDelete.setOnClickListener {
                        viewModel.deleteIncome(income)
                        bottomSheet.dismiss()
                    }
                    btnCancelDelete.setOnClickListener {
                        resetForm()
                    }
                }
                bottomSheet.setContentView(binding.root)
                bottomSheet.show()
            },
            currencySymbol = userData?.currencySymbol ?: "$"
        )

        binding.recyclerViewIncomes.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = incomesAdapter
        }
    }

    private fun setupListeners() {
        binding.btnNewIncome.setOnClickListener {
            val intent = Intent(requireContext(), NewIncomeActivity::class.java)
            startActivity(intent)
        }
    }

    private fun updateEmptyState(isEmpty: Boolean) {
        binding.layoutIncomes.visibility = if (isEmpty) View.GONE else View.VISIBLE
        binding.layoutNoIncomes.visibility = if (isEmpty) View.VISIBLE else View.GONE
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            userDataStore.getUserData().collect { user ->
                userData = user
                user.currencySymbol.let { symbol ->
                    incomesAdapter.updateCurrencySymbol(symbol)
                }
            }
        }
        viewModel.incomes.observe(viewLifecycleOwner) { incomes ->
            incomesAdapter.submitList(incomes)
            updateEmptyState(incomes.isEmpty())
        }
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.swipeRefreshLayout.isRefreshing = isLoading
            binding.progressBar.visibility =
                if (isLoading && !binding.swipeRefreshLayout.isRefreshing) View.VISIBLE else View.GONE
        }
        viewModel.errorMessage.observe(viewLifecycleOwner) { errorMessage ->
            errorMessage?.let {
                Snackbar.make(binding.root, it, Snackbar.LENGTH_LONG)
                    .setAction(getString(R.string.retry)) { viewModel.refreshIncomes() }
                    .show()
                viewModel.clearError()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshIncomes()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}