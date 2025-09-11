package com.dialcadev.dialcash.ui.incomegroup

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dialcadev.dialcash.data.AppRepository
import com.dialcadev.dialcash.data.dto.IncomeGroupRemaining
import com.dialcadev.dialcash.data.entities.IncomeGroup
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class IncomesViewModel @Inject constructor(private val repo: AppRepository): ViewModel() {
    private val _incomes = MutableLiveData<List<IncomeGroupRemaining>>()
    val incomes: LiveData<List<IncomeGroupRemaining>> = _incomes

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    init {
        fetchIncomes()
    }
    fun fetchIncomes(){
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            try {
                val incomes = repo.getAllIncomeGroupsWithRemaining().first()
                _incomes.value = incomes
            } catch (e: Exception) {
                _errorMessage.value = "Error fetching incomes: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    fun refreshIncomes() {
        fetchIncomes()
    }
    fun clearError() {
        _errorMessage.value = null
    }
}