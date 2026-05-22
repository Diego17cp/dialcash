package com.dialcadev.dialcash.features.incomegroups.presentation.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dialcadev.dialcash.features.incomegroups.domain.dtos.IncomeGroupRemaining
import com.dialcadev.dialcash.features.incomegroups.domain.usecases.DeleteIncomeGroupUseCase
import com.dialcadev.dialcash.features.incomegroups.domain.usecases.GetAllIncomeGroupsWithRemainingUseCase
import com.dialcadev.dialcash.features.incomegroups.domain.usecases.UpdateIncomeGroupUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AllIncomeGroupsViewModel @Inject constructor(
    private val getAllIncomeGroupsWithRemainingUseCase: GetAllIncomeGroupsWithRemainingUseCase,
    private val deleteIncomeGroupUseCase: DeleteIncomeGroupUseCase,
    private val updateIncomeGroupUseCase: UpdateIncomeGroupUseCase
) : ViewModel() {
    private val _uiState = MutableStateFlow(AllIncomeGroupsUiState())
    val uiState: StateFlow<AllIncomeGroupsUiState> = _uiState.asStateFlow()

    init {
        loadIncomeGroups()
    }
    private fun loadIncomeGroups() {
        _uiState.update {
            it.copy(
                isLoading = true,
                errorMessage = null
            )
        }
        viewModelScope.launch {
            try {
                getAllIncomeGroupsWithRemainingUseCase().collect { incomeGroups ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            incomeGroups = incomeGroups,
                            errorMessage = null
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Error loading income groups: ${e.message}"
                    )
                }
            }
        }
    }
    fun refreshIncomeGroups() {
        loadIncomeGroups()
    }
    fun deleteIncomeGroup(income: IncomeGroupRemaining) {
        viewModelScope.launch {
            deleteIncomeGroupUseCase(income.id).onFailure { error ->
                _uiState.update {
                    it.copy(
                        errorMessage = "Error deleting income group: ${error.message}"
                    )
                }
            }
        }
    }
    fun updateIncomeGroup(income: IncomeGroupRemaining) {
        viewModelScope.launch {
            updateIncomeGroupUseCase(
                incomeGroupId = income.id,
                name = income.name,
                amount = income.amount
            ).onFailure { error ->
                _uiState.update {
                    it.copy(
                        errorMessage = "Error updating income group: ${error.message}"
                    )
                }
            }
        }
    }
    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}