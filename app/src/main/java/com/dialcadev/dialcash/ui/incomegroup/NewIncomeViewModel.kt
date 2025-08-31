package com.dialcadev.dialcash.ui.incomegroup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dialcadev.dialcash.data.AppRepository
import com.dialcadev.dialcash.data.entities.IncomeGroup
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NewIncomeViewModel @Inject constructor(private val repo: AppRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(NewIncomeUiState())
    val uiState: StateFlow<NewIncomeUiState> = _uiState.asStateFlow()

    fun createIncomeGroup(name: String, amount: Double, remaining: Double?) {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
                val remainingToSend = remaining ?: amount
                val incomeGroup = IncomeGroup(name = name, amount = amount, remaining = remainingToSend)
                val incomeId = repo.createIncomeGroup(incomeGroup)
                _uiState.value = _uiState.value.copy(isLoading = false, isSuccess = true, errorMessage = null)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, isSuccess = false, errorMessage = e.message ?: "An error occurred")
            }
        }
    }
}

data class NewIncomeUiState(
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false,
    val errorMessage: String? = null
)