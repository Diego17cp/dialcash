package com.dialcadev.dialcash.features.settings.presentation.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dialcadev.dialcash.features.settings.domain.usecases.ImportBackupUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ImportDataViewModel @Inject constructor(
    private val importBackupUseCase: ImportBackupUseCase
) : ViewModel() {
    private val _uiState = MutableStateFlow(ImportDataUiState())
    val uiState = _uiState.asStateFlow()

    fun toggleCheckbox() {
        _uiState.value = _uiState.value.copy(isCheckboxChecked = !_uiState.value.isCheckboxChecked)
    }
    fun setCheckboxChecked(checked: Boolean) {
        _uiState.value = _uiState.value.copy(isCheckboxChecked = checked)
    }
    fun startImport(uriString: String) {
        _uiState.update { it.copy(isLoading = true, errorMessage = null, isSuccess = false) }

        viewModelScope.launch {
            try {
                importBackupUseCase(uriString)
                _uiState.update { it.copy(isLoading = false, isSuccess = true) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, errorMessage = e.message ?: "Failed to import backup") }
            }
        }
    }
    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun onCompleteHandled() {
        _uiState.update { it.copy(isSuccess = false) }
    }
}