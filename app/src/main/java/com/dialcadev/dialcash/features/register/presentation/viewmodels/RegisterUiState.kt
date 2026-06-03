package com.dialcadev.dialcash.features.register.presentation.viewmodels

import android.net.Uri
import androidx.annotation.StringRes

data class RegisterUiState(
    val name: String = "",
    val currency: String = "$",
    val photoUri: Uri? = null,

    @StringRes val nameError: Int? = null,
    @StringRes val currencyError: Int? = null,

    val isFormValid: Boolean = false,

    val isLoading: Boolean = false,
    val isSuccess: Boolean = false,
    val errorMessage: String? = null
)
