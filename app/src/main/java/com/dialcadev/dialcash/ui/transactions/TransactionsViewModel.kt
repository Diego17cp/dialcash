package com.dialcadev.dialcash.ui.transactions

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dialcadev.dialcash.data.AppRepository
import com.dialcadev.dialcash.data.dto.TransactionWithDetails
import com.dialcadev.dialcash.data.entities.Account
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TransactionsViewModel @Inject constructor(private val repository: AppRepository) :
    ViewModel() {
    private val _transactions = MutableLiveData<List<TransactionWithDetails>>()
    val transactions: LiveData<List<TransactionWithDetails>> = _transactions
    private val _accounts = MutableLiveData<List<Account>>()
    val accounts: LiveData<List<Account>> = _accounts

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    private val _searchQuery = MutableLiveData<String>("")
    private val _typesFilter = MutableLiveData<List<String>>(emptyList())
    private val _startDate = MutableLiveData<Long?>(null)
    private val _endDate = MutableLiveData<Long?>(null)
    val _accountNames = MutableLiveData<List<String>>(emptyList())

    private val _isFiltered = MutableLiveData<Boolean>(false)
    val isFiltered: LiveData<Boolean> = _isFiltered

    private var allTransactions: List<TransactionWithDetails> = emptyList()
    private var initialTransactions: List<TransactionWithDetails> = emptyList()

    init {
        fetchTransactions()
        fetchAccounts()
    }

    fun fetchTransactions() {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null

            try {
                val transactions = repository.getAllTransactions().first()
                initialTransactions = transactions
                allTransactions = transactions
                applyFilters()
            } catch (e: Exception) {
                _errorMessage.value = "Error fetching transactions: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    fun fetchAccounts() {
        viewModelScope.launch {
            _errorMessage.value = null
            try {
                repository.getAllAccounts().collect { accounts ->
                    _accounts.value = accounts
                }
            } catch (e: Exception) {
                _errorMessage.value = "Error fetching accounts: ${e.message}"
            }
        }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
        applyFilters()
    }

    fun setTypesFilter(types: List<String>) {
        _typesFilter.value = types
        applyFilters()
    }

    fun setStartDate(timestamp: Long?) {
        _startDate.value = timestamp
        applyFilters()
    }

    fun setEndDate(timestamp: Long?) {
        _endDate.value = timestamp
        applyFilters()
    }

    fun setAccountFilter(accountNames: List<String>) {
        _accountNames.value = accountNames
        applyFilters()
    }

    private fun applyFilters() {
        val query = _searchQuery.value ?: ""
        val types = _typesFilter.value ?: emptyList()
        val startDate = _startDate.value
        val endDate = _endDate.value
        val accountNames = _accountNames.value ?: emptyList()

        val filteredTransactions = allTransactions.filter { transaction ->
            val matchesQuery = if (query.isBlank()) true
            else
                listOf(
                    transaction.description,
                    transaction.accountName,
                    transaction.incomeGroupName
                )
                    .any { it?.contains(query, ignoreCase = true) == true }

            val matchesTypes = if (types.isEmpty()) true
            else types.any { selected ->
                transaction.type?.equals(selected, ignoreCase = true) == true
            }

            val matchesDate = if (startDate == null && endDate == null) true
            else {
                val txDate = transaction.date ?: return@filter false
                val afterStart = startDate?.let { txDate >= it } ?: true
                val beforeEnd = endDate?.let { txDate <= it } ?: true
                afterStart && beforeEnd
            }

            val matchesAccount = if (accountNames.isEmpty()) true
            else accountNames.any { accName ->
                transaction.accountName?.equals(accName, ignoreCase = true) == true
            }

            matchesQuery && matchesTypes && matchesDate && matchesAccount
        }

        _transactions.value = filteredTransactions
        _isFiltered.value =
            query.isNotBlank() || filteredTransactions.size != initialTransactions.size || startDate != null || endDate != null
    }

    fun clearFilters() {
        _searchQuery.value = ""
        _typesFilter.value = emptyList()
        _startDate.value = null
        _endDate.value = null
        _isFiltered.value = false
        _accountNames.value = emptyList()
        allTransactions = initialTransactions
        applyFilters()
    }

    fun refreshTransactions() {
        fetchTransactions()
    }

    fun clearError() {
        _errorMessage.value = null
    }
}