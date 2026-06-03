package com.dialcadev.dialcash.features.settings.presentation.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dialcadev.dialcash.features.settings.domain.usecases.ExportBackupUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DownloadDataViewModel @Inject constructor(
    private val exportBackupUseCase: ExportBackupUseCase
) : ViewModel() {
    private val _uiState = MutableStateFlow(DownloadDataUiState())
    val uiState = _uiState.asStateFlow()

    fun toggleCheckbox() {
        _uiState.update { it.copy(isCheckboxChecked = !it.isCheckboxChecked) }
    }
    fun setCheckboxChecked(checked: Boolean) {
        _uiState.update { it.copy(isCheckboxChecked = checked) }
    }
    fun startExport(uriString: String) {
        _uiState.update { it.copy(isLoading = true, errorMessage = null, isSuccess = false) }
        viewModelScope.launch {
            try {
                exportBackupUseCase(uriString)
                _uiState.update { it.copy(isLoading = false, isSuccess = true) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, errorMessage = e.message ?: "Failed to backup", isSuccess = false) }
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