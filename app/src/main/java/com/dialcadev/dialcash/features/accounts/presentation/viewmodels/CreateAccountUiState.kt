package com.dialcadev.dialcash.features.accounts.presentation.viewmodels

import androidx.annotation.StringRes

data class CreateAccountUiState(
    val accountName: String = "",
    val accountType: String? = null,
    val initialBalance: String = "0.00",

    @StringRes val nameError: Int? = null,
    @StringRes val typeError: Int? = null,
    @StringRes val balanceError: Int? = null,

    val isFormValid: Boolean = false,

    val isLoading: Boolean = false,
    val isSuccess: Boolean = false,
    val errorMessage: String? = null
)
