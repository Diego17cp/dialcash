package com.dialcadev.dialcash.features.settings.presentation.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dialcadev.dialcash.features.settings.domain.usecases.DeleteAccountUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DeleteAccountViewModel @Inject constructor(
    private val deleteAccountUseCase: DeleteAccountUseCase
) : ViewModel() {
    private val _uiState = MutableStateFlow(DeleteAccountUiState())
    val uiState = _uiState.asStateFlow()
    fun toggleCheckbox() {
        _uiState.update { it.copy(isCheckboxChecked = !it.isCheckboxChecked) }
    }

    fun setCheckboxChecked(isChecked: Boolean) {
        _uiState.update { it.copy(isCheckboxChecked = isChecked) }
    }

    fun deleteAccount() {
        if (!_uiState.value.isCheckboxChecked) return

        _uiState.update { it.copy(isLoading = true, errorMessage = null) }

        viewModelScope.launch {
            try {
                deleteAccountUseCase()
                _uiState.update { it.copy(isLoading = false, isSuccess = true) }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isLoading = false, errorMessage = e.message ?: "Unknown error")
                }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}