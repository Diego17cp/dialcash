package com.dialcadev.dialcash.features.settings.presentation.viewmodels

data class DownloadDataUiState(
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false,
    val errorMessage: String? = null,
    val isCheckboxChecked: Boolean = false
)
