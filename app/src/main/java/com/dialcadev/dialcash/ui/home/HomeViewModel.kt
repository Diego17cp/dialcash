package com.dialcadev.dialcash.ui.home

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dialcadev.dialcash.data.AppRepository
import com.dialcadev.dialcash.data.ValidationResult
import com.dialcadev.dialcash.data.dao.AccountBalanceWithOriginal
import com.dialcadev.dialcash.data.dto.TransactionWithDetails
import com.dialcadev.dialcash.data.entities.Account
import com.dialcadev.dialcash.data.entities.IncomeGroup
import com.dialcadev.dialcash.data.entities.Transaction
import com.dialcadev.dialcash.ui.shared.contracts.AccountOperations
import com.dialcadev.dialcash.ui.shared.contracts.TransactionsOperations
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: AppRepository
) : ViewModel(), TransactionsOperations, AccountOperations {

    //    Total Balance of accounts
    private val _totalBalance = MutableLiveData<Double>()
    val totalBalance: LiveData<Double> = _totalBalance

    //    Main Accounts
    private val _mainAccounts = MutableLiveData<List<AccountBalanceWithOriginal>>()
    val mainAccounts: LiveData<List<AccountBalanceWithOriginal>> = _mainAccounts

    //    Last transactions
    private val _recentTransactions = MutableLiveData<List<TransactionWithDetails>>()
    val recentTransactions: LiveData<List<TransactionWithDetails>> = _recentTransactions

    //    Accounts, Income Groups for dropdowns
    private val _accounts = MutableLiveData<List<Account>>()
    val accounts: LiveData<List<Account>> = _accounts
    private val _incomeGroups = MutableLiveData<List<IncomeGroup>>()
    val incomeGroups: LiveData<List<IncomeGroup>> = _incomeGroups

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
    override fun deleteAccount(account: Account) {
        viewModelScope.launch {
            try {
                repository.deleteAccount(account)
                fetchHomeData()
            } catch (e: Exception) {
                _errorMessage.value = "Error deleting account: ${e.message}"
            }
        }
    }
    override fun updateAccount(account: Account) {
        viewModelScope.launch {
            try {
                repository.updateAccount(account)
                fetchHomeData()
            } catch (e: Exception) {
                _errorMessage.value = "Error updating account: ${e.message}"
            }
        }
    }
    fun loadAccounts() {
        viewModelScope.launch {
            try {
                val accounts = repository.getAllAccounts().first()
                _accounts.value = accounts
            } catch (e: Exception) {
                _errorMessage.value = "Error loading accounts: ${e.message}"
            }
        }
    }
    fun loadIncomeGroups() {
        viewModelScope.launch {
            try {
                val incomeGroups = repository.getAllIncomeGroups().first()
                _incomeGroups.value = incomeGroups
            } catch (e: Exception) {
                _errorMessage.value = "Error loading income groups: ${e.message}"
            }
        }
    }
    override fun updateTransaction(transaction: Transaction) {
        viewModelScope.launch {
            try {
                repository.updateTransaction(transaction)
                fetchHomeData()
            } catch (e: Exception) {
                _errorMessage.value = "Error updating transaction: ${e.message}"
                Log.d("HomeViewModel", "updateTransaction: ${e.message}")
            }
        }
    }
    override fun deleteTransaction(transaction: Transaction) {
        viewModelScope.launch {
            try {
                repository.deleteTransaction(transaction)
                fetchHomeData()
            } catch (e: Exception) {
                _errorMessage.value = "Error deleting transaction: ${e.message}"
                Log.d("HomeViewModel", "deleteTransaction: ${e.message}")
            }
        }
    }
    suspend fun getAccountBalance(accountId: Int): Double? {
        return repository.getAccountBalance(accountId)
    }
    suspend fun getRemainingForIncomeGroup(incomeGroupId: Int): Double? {
        return repository.getRemainingForIncomeGroup(incomeGroupId)
    }
    override fun validateTransactionBalance(
        transactionId: Int,
        type: String,
        accountId: Int,
        amount: Double,
        accountToId: Int?,
        incomeGroupId: Int?,
        onResult: (Boolean, String?) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val result = when (type) {
                    "expense" -> repository.validateExpenseEdit(
                        transactionId,
                        accountId,
                        amount,
                        incomeGroupId
                    )
                    "transfer" -> repository.validateTransferEdit(
                        transactionId,
                        accountId,
                        accountToId!!,
                        amount
                    )
                    else -> ValidationResult(true, "")
                }
                onResult(result.isValid, result.message)
            } catch (e: Exception) {
                onResult(false, "Error validating transaction: ${e.message}")
            }
        }
    }
    fun clearError() {
        _errorMessage.value = null
    }
}