package com.dialcadev.dialcash.features.settings.presentation.viewmodels

data class EditProfileUiState(
    val isInitialized: Boolean = false,
    val isLoading: Boolean = false,
    val initialName: String = "",
    val initialCurrency: String = "",
    val currentPhotoUri: String = "",

    val isNameError: Boolean = false,
    val isCurrencyError: Boolean = false,
    val isSaveEnabled: Boolean = true,

    val showSuccessMessage: Boolean = false,
    val showErrorMessage: Boolean = false
)