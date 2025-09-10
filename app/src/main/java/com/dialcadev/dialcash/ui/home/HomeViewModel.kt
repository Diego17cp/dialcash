package com.dialcadev.dialcash.ui.home

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dialcadev.dialcash.data.AppRepository
import com.dialcadev.dialcash.data.dao.AccountBalanceWithOriginal
import com.dialcadev.dialcash.data.dto.AccountBalance
import com.dialcadev.dialcash.data.dto.TransactionWithDetails
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: AppRepository
) : ViewModel() {
//    Total Balance of accounts
    private val _totalBalance = MutableLiveData<Double>()
    val totalBalance: LiveData<Double> = _totalBalance

//    Main Accounts
    private val _mainAccounts = MutableLiveData<List<AccountBalanceWithOriginal>>()
    val mainAccounts: LiveData<List<AccountBalanceWithOriginal>> = _mainAccounts

//    Last transactions
    private val _recentTransactions = MutableLiveData<List<TransactionWithDetails>>()
    val recentTransactions: LiveData<List<TransactionWithDetails>> = _recentTransactions

//    Loader state
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    init {
        fetchHomeData()
    }

    fun fetchHomeData() {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null

            try {
                val accounts = repository.getMainAccounts().first()
                _mainAccounts.value = accounts
                _totalBalance.value = accounts.sumOf { it.balance }

                val transactions = repository.getRecentTransactions(limit = 10).first()
                _recentTransactions.value = transactions
            } catch (e: Exception) {
                _errorMessage.value = "Error fetching data: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    fun refreshData() {
        fetchHomeData()
    }
    fun clearError() {
        _errorMessage.value = null
    }
}