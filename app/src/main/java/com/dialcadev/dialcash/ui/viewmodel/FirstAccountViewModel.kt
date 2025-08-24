package com.dialcadev.dialcash.ui.viewmodel

import android.util.Log
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
class FirstAccountViewModel @Inject constructor(private val repository: AppRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(FirstAccountUiState())
    val uiState: StateFlow<FirstAccountUiState> = _uiState.asStateFlow()

    fun createAccount(name: String, type: String, balance: Double) {
        viewModelScope.launch {
            try {
                Log.d("FirstAccountVM", "Creating account: $name, $type, $balance")
                _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
                val account = Account(name = name, type = type, balance = balance)
                val accountId = repository.createAccount(account)
                Log.d("FirstAccountVM", "Account created with ID: $accountId")
                _uiState.value = _uiState.value.copy(isLoading = false, isSuccess = true, errorMessage = null)
            } catch (e: Exception) {
                Log.e("FirstAccountVM", "Error creating account", e)
                _uiState.value = _uiState.value.copy(isLoading = false, isSuccess = false, errorMessage = e.message ?: "An error occurred")
            }
        }
    }
}
data class FirstAccountUiState(
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false,
    val errorMessage: String? = null
)