package com.dialcadev.dialcash.features.incomegroups.presentation.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dialcadev.dialcash.features.incomegroups.domain.usecases.CreateIncomeGroupUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CreateIncomeGroupViewModel @Inject constructor(
    private val createIncomeGroupUseCase: CreateIncomeGroupUseCase
) : ViewModel() {
    private val _uiState = MutableStateFlow(CreateIncomeGroupUiState())
    val uiState: StateFlow<CreateIncomeGroupUiState> = _uiState.asStateFlow()

    fun createIncomeGroup(name: String, amount: Double) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            val result = createIncomeGroupUseCase(name, amount)
            result.fold(
                onSuccess = {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isSuccess = true,
                        errorMessage = null
                    )
                },
                onFailure = { e ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isSuccess = false,
                        errorMessage = e.message ?: "An error occurred"
                    )
                }
            )
        }
    }
}