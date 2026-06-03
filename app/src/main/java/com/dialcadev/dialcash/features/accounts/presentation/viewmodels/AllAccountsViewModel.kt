package com.dialcadev.dialcash.features.accounts.presentation.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dialcadev.dialcash.features.accounts.domain.models.Account
import com.dialcadev.dialcash.features.accounts.domain.usecases.DeleteAccountUseCase
import com.dialcadev.dialcash.features.accounts.domain.usecases.GetAllAccountsWithBalanceUseCase
import com.dialcadev.dialcash.features.accounts.domain.usecases.UpdateAccountUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AllAccountsViewModel @Inject constructor(
    private val getAllAccountsWithBalanceUseCase: GetAllAccountsWithBalanceUseCase,
    private val updateAccountUseCase: UpdateAccountUseCase,
    private val deleteAccountUseCase: DeleteAccountUseCase
) : ViewModel() {
    private val _uiState = MutableStateFlow(AllAccountsUiState())
    val uiState: StateFlow<AllAccountsUiState> = _uiState.asStateFlow()

    init {
        loadAllAccounts()
    }

    private fun loadAllAccounts() {
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            try {
                getAllAccountsWithBalanceUseCase().collect { accounts ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            accounts = accounts,
                            errorMessage = null
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Error loading accounts: ${e.message}"
                    )
                }
            }
        }
    }

    fun refreshAccounts() {
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }    }

    fun deleteAccount(account: Account) {
        viewModelScope.launch {
            deleteAccountUseCase(account.id).onFailure { error ->
                _uiState.update { it.copy(errorMessage = "Error deleting account: ${error.message}") }
            }
        }
    }

    fun updateAccount(account: Account) {
        viewModelScope.launch {
            updateAccountUseCase(
                accountId = account.id,
                name = account.name,
                type = account.type,
                balance = account.balance,
            ).onFailure { error ->
                _uiState.update { it.copy(errorMessage = "Error updating account: ${error.message}") }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}