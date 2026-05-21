package com.dialcadev.dialcash.features.accounts.presentation.viewmodels

import com.dialcadev.dialcash.features.accounts.domain.dtos.AccountBalanceWithOriginal

data class AllAccountsUiState(
    val isLoading: Boolean = false,
    val accounts: List<AccountBalanceWithOriginal> = emptyList(),
    val errorMessage: String? = null
)
