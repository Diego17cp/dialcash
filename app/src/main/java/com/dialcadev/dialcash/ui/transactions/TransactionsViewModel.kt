package com.dialcadev.dialcash.ui.transactions

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
import com.dialcadev.dialcash.ui.shared.contracts.TransactionsOperations
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

@HiltViewModel
class TransactionsViewModel @Inject constructor(private val repository: AppRepository) :
    ViewModel(), TransactionsOperations {
    private val _transactions = MutableLiveData<List<TransactionWithDetails>>()
    val transactions: LiveData<List<TransactionWithDetails>> = _transactions
    private val _forChartTransactions = MutableLiveData<List<Transaction>>()
    val forChartTransactions: LiveData<List<Transaction>> = _forChartTransactions
    private val _accounts = MutableLiveData<List<Account>>()
    val accounts: LiveData<List<Account>> = _accounts
    private val _accountBalances = MutableLiveData<List<AccountBalanceWithOriginal>>()
    val accountBalances: LiveData<List<AccountBalanceWithOriginal>> = _accountBalances
    private val _specificDateBalance = MutableLiveData<Double>()
    val specificDateBalance: LiveData<Double> = _specificDateBalance

    private val _specificDateTransactions = MutableLiveData<List<TransactionWithDetails>>()
    val specificDateTransactions: LiveData<List<TransactionWithDetails>> = _specificDateTransactions
    private val _incomeGroups = MutableLiveData<List<IncomeGroup>>()
    val incomeGroups: LiveData<List<IncomeGroup>> = _incomeGroups
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
        fetchAccountBalances()
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
    fun fetchTransactionsBetweenDates(startDate: Long, endDate: Long) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            try {
                val transactions = repository.getTransactionBetween(startDate, endDate).first()
                _forChartTransactions.value = transactions
            } catch (e: Exception) {
                _errorMessage.value = "Error fetching transactions for charts: ${e.message}"
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
    fun fetchAccountBalances() {
        viewModelScope.launch {
            try {
                repository.getAllAccountBalances().collect { accounts ->
                    _accountBalances.value = accounts
                }
            } catch (e: Exception) {
                _errorMessage.value = "Error fetching account balances: ${e.message}"
            }
        }
    }
    fun fetchBalanceAtDate(accountId: Int, targetDate: Long) {
        viewModelScope.launch {
//            _isLoading.value = true
            _errorMessage.value = null
            try {
                val calendar = Calendar.getInstance().apply { timeInMillis = targetDate }
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                val startOfDay = calendar.timeInMillis
                calendar.set(Calendar.HOUR_OF_DAY, 23)
                calendar.set(Calendar.MINUTE, 59)
                calendar.set(Calendar.SECOND, 59)
                calendar.set(Calendar.MILLISECOND, 999)
                val endOfDay = calendar.timeInMillis
                val balance = repository.getBalanceAtDate(accountId, targetDate)
                _specificDateBalance.value = balance
                repository.getTransactionsForAccountBetween(accountId, startOfDay, endOfDay).collect { transactions ->
                    _specificDateTransactions.value = transactions.map { tx ->
                        TransactionWithDetails(
                            id = tx.id,
                            amount = tx.amount,
                            type = tx.type,
                            date = tx.date,
                            description = tx.description,
                            accountName = repository.getAccountById(tx.accountId)?.name ?: "",
                            accountToName = repository.getAccountById(tx.transferAccountId ?: 0)?.name ?: "",
                            incomeGroupName = repository.getIncomeGroupById(tx.relatedIncomeId ?: 0)?.name ?: ""
                        )
                    }
                }
            } catch (e: Exception) {
                _errorMessage.value = "Error fetching balance at date: ${e.message}"
                Log.d("TransactionsViewModel", "fetchBalanceAtDate: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }
    fun clearDateSearch() {
        _specificDateBalance.value = 0.0
        _specificDateTransactions.value = emptyList()
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
    override fun updateTransaction(transaction: Transaction) {
        viewModelScope.launch {
            try {
                repository.updateTransaction(transaction)
                fetchTransactions()
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
                fetchTransactions()
            } catch (e: Exception) {
                _errorMessage.value = "Error deleting transaction: ${e.message}"
                Log.d("HomeViewModel", "deleteTransaction: ${e.message}")
            }
        }
    }
    fun setSearchQuery(query: String) {
        _searchQuery.value = query
        applyFilters()
    }
    fun getSearchQuery(): String {
        return _searchQuery.value ?: ""
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
    fun getTypesFilter(): List<String> {
        return _typesFilter.value ?: emptyList()
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