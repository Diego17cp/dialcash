package com.dialcadev.dialcash.features.settings.presentation.viewmodels

data class ImportDataUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isSuccess: Boolean = false,
    val isCheckboxChecked: Boolean = false
)
