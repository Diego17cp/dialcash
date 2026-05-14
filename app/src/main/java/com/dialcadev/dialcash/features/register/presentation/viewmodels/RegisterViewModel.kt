package com.dialcadev.dialcash.features.register.presentation.viewmodels

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dialcadev.dialcash.R
import com.dialcadev.dialcash.features.register.domain.usecases.RegisterUserUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RegisterViewModel @Inject constructor(
    private val registerUserUseCase: RegisterUserUseCase
): ViewModel() {
    private val _uiState = MutableStateFlow(RegisterUiState())
    val uiState: StateFlow<RegisterUiState> = _uiState.asStateFlow()

    fun onNameChanged(name: String) {
        val error = if (name.isBlank()) R.string.name_cannot_be_empty else null
        updateStateAndValidate(name = name, nameError = error)
    }
    fun onCurrencyChanged(currency: String) {
        val error = if (currency.isBlank()) R.string.currency_cannot_be_empty else null
        updateStateAndValidate(currency = currency, currencyError = error)
    }
    fun onPhotoSelected(uri: Uri?) {
        _uiState.value = _uiState.value.copy(photoUri = uri)
    }
    private fun updateStateAndValidate(
        name: String = _uiState.value.name,
        currency: String = _uiState.value.currency,
        nameError: Int? = _uiState.value.nameError,
        currencyError: Int? = _uiState.value.currencyError
    ) {
        val isFormValid = name.isNotBlank() && currency.isNotBlank() &&
                nameError == null && currencyError == null
        _uiState.value = _uiState.value.copy(
            name = name,
            currency = currency,
            nameError = nameError,
            currencyError = currencyError,
            isFormValid = isFormValid
        )
    }
    fun registerUser() {
        val currentState = _uiState.value
        if (!currentState.isFormValid) return

        viewModelScope.launch {
            _uiState.value = currentState.copy(isLoading = true, errorMessage = null)
            val result = registerUserUseCase(
                name = currentState.name,
                currency = currentState.currency,
                profilePictureUri = currentState.photoUri?.toString()
            )
            result.fold(
                onSuccess = {
                    _uiState.value = _uiState.value.copy(isLoading = false, isSuccess = true)
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = error.message ?: "An unknown error occurred"
                    )
                }
            )
        }
    }
}