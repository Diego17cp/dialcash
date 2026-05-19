package com.dialcadev.dialcash.features.transactions.presentation.viewmodels

import androidx.annotation.StringRes
import com.dialcadev.dialcash.features.accounts.domain.dtos.AccountBalanceWithOriginal
import com.dialcadev.dialcash.features.incomegroups.domain.dtos.IncomeGroupRemaining

data class CreateTransactionUiState(
    val currencySymbol: String = "$",
    val accounts: List<AccountBalanceWithOriginal> = emptyList(),
    val incomeGroups: List<IncomeGroupRemaining> = emptyList(),

    val amount: String = "",
    val description: String = "",
    val date: Long? = null,
    val selectedAccountFrom: AccountBalanceWithOriginal? = null,
    val selectedAccountTo: AccountBalanceWithOriginal? = null,
    val selectedIncomeGroup: IncomeGroupRemaining? = null,

    @StringRes val amountError: Int? = null,
    @StringRes val descriptionError: Int? = null,

    val isLoading: Boolean = false,
    val isSuccess: Boolean = false,
    val errorMessage: String? = null
)
