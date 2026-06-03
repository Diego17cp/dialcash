package com.dialcadev.dialcash.features.settings.presentation.viewmodels

data class DeleteAccountUiState(
    val isLoading: Boolean = false,
    val isCheckboxChecked: Boolean = false,
    val isSuccess: Boolean = false,
    val errorMessage: String? = null
)