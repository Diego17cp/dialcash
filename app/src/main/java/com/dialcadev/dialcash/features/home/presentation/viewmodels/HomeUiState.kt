package com.dialcadev.dialcash.features.home.presentation.viewmodels

import com.dialcadev.dialcash.features.accounts.domain.dtos.AccountBalanceWithOriginal
import com.dialcadev.dialcash.features.accounts.domain.models.Account
import com.dialcadev.dialcash.features.incomegroups.domain.models.IncomeGroup
import com.dialcadev.dialcash.features.transactions.domain.dtos.TransactionWithDetails

data class HomeUiState(
    val isLoading: Boolean = false,
    val totalBalance: Double = 0.0,
    val mainAccounts: List<AccountBalanceWithOriginal> = emptyList(),
    val recentTransactions: List<TransactionWithDetails> = emptyList(),

    val accounts: List<Account> = emptyList(),
    val incomeGroups: List<IncomeGroup> = emptyList(),

    val errorMessage: String? = null
)
