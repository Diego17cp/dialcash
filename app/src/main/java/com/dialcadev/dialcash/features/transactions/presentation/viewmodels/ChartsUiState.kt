package com.dialcadev.dialcash.features.transactions.presentation.viewmodels

import com.dialcadev.dialcash.features.accounts.domain.dtos.AccountBalanceWithOriginal
import com.dialcadev.dialcash.features.accounts.domain.models.Account
import com.dialcadev.dialcash.features.transactions.domain.dtos.TransactionWithDetails
import com.dialcadev.dialcash.features.transactions.domain.models.Transaction

data class ChartsUiState(
    val isLoading: Boolean = true,
    val errorMessage: String? = null,

    val currentMonthTimestamp: Long = 0L,
    val monthTransactions: List<Transaction> = emptyList(),
    val totalIncome: Float = 0f,
    val totalExpense: Float = 0f,
    val totalTransfer: Float = 0f,

    val accountsList: List<AccountBalanceWithOriginal> = emptyList(),
    val selectedAccountId: Int? = null,
    val selectedAccountName: String? = null,
    val selectedDate: Long? = null,

    val snapshotBalance: Double? = null,
    val snapshotTransactions: List<TransactionWithDetails> = emptyList()
)
