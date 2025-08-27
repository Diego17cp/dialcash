package com.dialcadev.dialcash.ui.accounts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dialcadev.dialcash.data.AppRepository
import com.dialcadev.dialcash.data.entities.Account
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NewAccountViewModel @Inject constructor(private val repository: AppRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(NewAccountUiState())
    val uiState: StateFlow<NewAccountUiState> = _uiState.asStateFlow()

    fun createAccount(name: String, type: String, balance: Double) {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
                val account = Account(name= name, type = type, balance = balance)
                val accountId = repository.createAccount(account)
                _uiState.value = _uiState.value.copy(isLoading = false, isSuccess = true, errorMessage = null)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, isSuccess = false, errorMessage = e.message ?: "An error occurred")
            }
        }
    }
}
data class NewAccountUiState(
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false,
    val errorMessage: String? = null
)