package com.dialcadev.dialcash.ui.transactions

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dialcadev.dialcash.data.AppRepository
import com.dialcadev.dialcash.data.dto.TransactionWithDetails
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TransactionsViewModel @Inject constructor(private val repository: AppRepository) : ViewModel() {
    private val _transactions = MutableLiveData<List<TransactionWithDetails>>()
    val transactions: LiveData<List<TransactionWithDetails>> = _transactions

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    init {
        fetchTransactions()
    }

    fun fetchTransactions() {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null

            try {
                val transactions = repository.getAllTransactions().first()
                _transactions.value = transactions
            } catch (e: Exception) {
                _errorMessage.value = "Error fetching transactions: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    fun refreshTransactions() {
        fetchTransactions()
    }
    fun clearError() {
        _errorMessage.value = null
    }
}