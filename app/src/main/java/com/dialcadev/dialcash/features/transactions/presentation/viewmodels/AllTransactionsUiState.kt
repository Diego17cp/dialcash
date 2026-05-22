package com.dialcadev.dialcash.features.transactions.presentation.viewmodels

import com.dialcadev.dialcash.features.accounts.domain.models.Account
import com.dialcadev.dialcash.features.incomegroups.domain.models.IncomeGroup
import com.dialcadev.dialcash.features.transactions.domain.dtos.TransactionWithDetails

data class TransactionFilters(
    val searchQuery: String = "",
    val transactionTypes: List<String> = emptyList(),
    val startDate: Long? = null,
    val endDate: Long? = null,
    val accountNames: List<String> = emptyList(),
) {
    val hasActiveFilters: Boolean
        get() = searchQuery.isNotBlank() || transactionTypes.isNotEmpty() || startDate != null || endDate != null || accountNames.isNotEmpty()
}

data class AllTransactionsUiState(
    val isLoading: Boolean = false,
    val allTransactions: List<TransactionWithDetails> = emptyList(),
    val filteredTransactions: List<TransactionWithDetails> = emptyList(),
    val currentFilters: TransactionFilters = TransactionFilters(),
    val isFiltered: Boolean = false,
    val accounts: List<Account> = emptyList(),
    val incomeGroups: List<IncomeGroup> = emptyList(),
    val errorMessage: String? = null

)
