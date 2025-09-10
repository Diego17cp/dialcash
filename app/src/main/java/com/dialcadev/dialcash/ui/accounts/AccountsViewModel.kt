package com.dialcadev.dialcash.ui.accounts

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dialcadev.dialcash.data.AppRepository
import com.dialcadev.dialcash.data.dao.AccountBalanceWithOriginal
import com.dialcadev.dialcash.data.entities.Account
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AccountsViewModel @Inject constructor(private val repository: AppRepository): ViewModel() {
    private val _accounts = MutableLiveData<List<AccountBalanceWithOriginal>>()
    val accounts: MutableLiveData<List<AccountBalanceWithOriginal>> = _accounts

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading
    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage
    init {
        fetchAccounts()
    }
    fun fetchAccounts(){
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            try {
                val accounts = repository.getAllAccountBalances().first()
                _accounts.value = accounts
            } catch (e: Exception) {
                _errorMessage.value = "Error fetching accounts: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    fun deleteAccount(account: Account) {
        viewModelScope.launch {
            try {
                repository.deleteAccount(account)
                fetchAccounts()
            } catch (e: Exception) {
                _errorMessage.value = "Error deleting account: ${e.message}"
            }
        }
    }
    fun updateAccount(account: Account) {
        viewModelScope.launch {
            try {
                repository.updateAccount(account)
                fetchAccounts()
            } catch (e: Exception) {
                _errorMessage.value = "Error updating account: ${e.message}"
            }
        }
    }
    fun refreshAccounts() {
        fetchAccounts()
    }
    fun clearError() {
        _errorMessage.value = null
    }
}