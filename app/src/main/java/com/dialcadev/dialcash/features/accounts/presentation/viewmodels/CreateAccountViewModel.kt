package com.dialcadev.dialcash.features.accounts.presentation.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dialcadev.dialcash.R
import com.dialcadev.dialcash.features.accounts.domain.usecases.CreateAccountUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject


@HiltViewModel
class CreateAccountViewModel @Inject constructor(
    private val createAccountUseCase: CreateAccountUseCase
) : ViewModel() {
    private val _uiState = MutableStateFlow(CreateAccountUiState())
    val uiState: StateFlow<CreateAccountUiState> = _uiState.asStateFlow()

    fun onNameChanged(name: String) {
        val error = if (name.isBlank()) R.string.account_name_required else null
        updateStateAndValidate(name = name, nameError = error)
    }
    fun onTypeChanged(type: String) {
        val error = if (type.isBlank()) R.string.account_type_required else null
        updateStateAndValidate(type = type, typeError = error)
    }
    fun onBalanceChanged(balance: String) {
        val balanceValue = balance.toDoubleOrNull()
        val error = when {
            balance.isBlank() -> R.string.initial_balance_required
            balanceValue == null || balanceValue < 0 -> R.string.enter_valid_amount
            else -> null
        }
        updateStateAndValidate(balance = balance, balanceError = error)
    }

    private fun updateStateAndValidate(
        name: String = _uiState.value.accountName,
        type: String? = _uiState.value.accountType,
        balance: String = _uiState.value.initialBalance,
        nameError: Int? = _uiState.value.nameError,
        typeError: Int? = _uiState.value.typeError,
        balanceError: Int? = _uiState.value.balanceError
    ) {
        val isValid = name.isNotBlank() && !type.isNullOrBlank() &&
                balance.isNotBlank() && balance.toDoubleOrNull() != null && balance.toDouble() >= 0 &&
                nameError == null && typeError == null && balanceError == null
        _uiState.value = _uiState.value.copy(
            accountName = name,
            accountType = type,
            initialBalance = balance,
            nameError = nameError,
            typeError = typeError,
            balanceError = balanceError,
            isFormValid = isValid
        )
    }

    fun createAccount() {
        val currentState = _uiState.value
        if (!currentState.isFormValid) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            val balance = currentState.initialBalance.toDoubleOrNull() ?: 0.0
            val result = createAccountUseCase(
                name = currentState.accountName,
                type = currentState.accountType!!,
                balance = balance
            )
            result.fold(
                onSuccess = {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isSuccess = true
                    )
                },
                onFailure = { e ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isSuccess = false,
                        errorMessage = e.message ?: "An unknown error occurred"
                    )
                }
            )
        }
    }
}