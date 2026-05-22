package com.dialcadev.dialcash.features.transactions.presentation.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dialcadev.dialcash.features.transactions.domain.usecases.DeleteTransactionUseCase
import com.dialcadev.dialcash.features.accounts.domain.usecases.GetAllAccountsUseCase
import com.dialcadev.dialcash.features.incomegroups.domain.usecases.GetAllIncomeGroupsUseCase
import com.dialcadev.dialcash.features.transactions.domain.models.Transaction
import com.dialcadev.dialcash.features.transactions.domain.usecases.GetAllTransactionsUseCase
import com.dialcadev.dialcash.features.transactions.domain.usecases.UpdateTransactionUseCase
import com.dialcadev.dialcash.features.transactions.domain.usecases.ValidateExpenseEditUseCase
import com.dialcadev.dialcash.features.transactions.domain.usecases.ValidateTransferEditUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AllTransactionsViewModel @Inject constructor(
    private val getAllTransactionsUseCase: GetAllTransactionsUseCase,
    private val getAllAccountsUseCase: GetAllAccountsUseCase,
    private val getAllIncomeGroupsUseCase: GetAllIncomeGroupsUseCase,
    private val updateTransactionUseCase: UpdateTransactionUseCase,
    private val deleteTransactionUseCase: DeleteTransactionUseCase,
    private val validateExpenseEditUseCase: ValidateExpenseEditUseCase,
    private val validateTransferEditUseCase: ValidateTransferEditUseCase
) : ViewModel() {
    private val _filters = MutableStateFlow(TransactionFilters())
    private val _isLoading = MutableStateFlow(false)
    private val _errorMessage = MutableStateFlow<String?>(null)

    private val domainDataFlow = combine(
        getAllTransactionsUseCase(),
        getAllAccountsUseCase(),
        getAllIncomeGroupsUseCase()
    ) { transactions, accounts, incomeGroups ->
        Triple(transactions, accounts, incomeGroups)
    }

    val uiState: StateFlow<AllTransactionsUiState> = combine(
        domainDataFlow,
        _filters,
        _isLoading,
        _errorMessage
    ) { (transactions, accounts, incomeGroups), currentFilters, loading, error ->

        val filteredList = transactions.filter { tx ->
            val query = currentFilters.searchQuery
            val matchesQuery = if (query.isBlank()) true else {
                listOf(tx.description, tx.accountName, tx.incomeGroupName)
                    .any { it?.contains(query, ignoreCase = true) == true }
            }

            val types = currentFilters.transactionTypes
            val matchesTypes = if (types.isEmpty()) true else {
                types.any { selected -> tx.type?.equals(selected, ignoreCase = true) == true }
            }

            val txDate = tx.date ?: 0L
            val startDate = currentFilters.startDate
            val endDate = currentFilters.endDate
            val matchesDate = if (startDate == null && endDate == null) true else {
                val afterStart = startDate?.let { txDate >= it } ?: true
                val beforeEnd = endDate?.let { txDate <= it } ?: true
                afterStart && beforeEnd
            }

            val accountNames = currentFilters.accountNames
            val matchesAccount = if (accountNames.isEmpty()) true else {
                accountNames.any { accName -> tx.accountName?.equals(accName, ignoreCase = true) == true }
            }

            matchesQuery && matchesTypes && matchesDate && matchesAccount
        }

        val isActiveFilter = currentFilters.hasActiveFilters || filteredList.size != transactions.size

        AllTransactionsUiState(
            isLoading = loading,
            allTransactions = transactions,
            filteredTransactions = filteredList,
            currentFilters = currentFilters,
            isFiltered = isActiveFilter,
            accounts = accounts,
            incomeGroups = incomeGroups,
            errorMessage = error
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AllTransactionsUiState(isLoading = true))

    fun updateFilters(newFilters: TransactionFilters) {
        _filters.value = newFilters
    }
    fun clearFilters() {
        _filters.value = TransactionFilters()
    }
    fun clearError() {
        _errorMessage.value = null
    }
    fun updateTransaction(transaction: Transaction) {
        viewModelScope.launch {
            updateTransactionUseCase(
                transactionId = transaction.id,
                amount = transaction.amount,
                date = transaction.date,
                description = transaction.description?.trim() ?: "",
                toAccountId = transaction.accountId,
                transferAccountId = transaction.transferAccountId,
                relatedIncomeId = transaction.relatedIncomeId
            ).onFailure { error ->
                _errorMessage.value = "Error updating transaction: ${error.message}"
            }
        }
    }

    fun deleteTransaction(transaction: Transaction) {
        viewModelScope.launch {
            deleteTransactionUseCase(transaction.id).onFailure { error ->
                _errorMessage.value = "Error deleting transaction: ${error.message}"
            }
        }
    }

    fun validateTransactionBalance(
        transactionId: Int,
        type: String,
        accountId: Int,
        amount: Double,
        accountToId: Int?,
        incomeGroupId: Int?,
        onResult: (Boolean, String?) -> Unit
    ) {
        viewModelScope.launch {
            val result = when (type) {
                "expense" -> validateExpenseEditUseCase(
                    transactionId,
                    accountId,
                    amount,
                    incomeGroupId
                )

                "transfer" -> validateTransferEditUseCase(
                    transactionId,
                    accountId,
                    accountToId ?: 0,
                    amount
                )

                else -> Result.success(Unit)
            }
            result.fold(
                onSuccess = { onResult(true, null) },
                onFailure = { error ->
                    onResult(
                        false,
                        "Error validating transaction: ${error.message}"
                    )
                }
            )
        }
    }
}