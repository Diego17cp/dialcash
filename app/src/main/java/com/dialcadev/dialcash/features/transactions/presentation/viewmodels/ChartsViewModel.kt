package com.dialcadev.dialcash.features.transactions.presentation.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dialcadev.dialcash.features.accounts.domain.usecases.GetAccountByIdUseCase
import com.dialcadev.dialcash.features.accounts.domain.usecases.GetAllAccountsUseCase
import com.dialcadev.dialcash.features.accounts.domain.usecases.GetAllAccountsWithBalanceUseCase
import com.dialcadev.dialcash.features.incomegroups.domain.usecases.GetIncomeGroupByIdUseCase
import com.dialcadev.dialcash.features.transactions.domain.dtos.TransactionWithDetails
import com.dialcadev.dialcash.features.transactions.domain.usecases.GetBalanceAtDateUseCase
import com.dialcadev.dialcash.features.transactions.domain.usecases.GetTransactionsBetweenUseCase
import com.dialcadev.dialcash.features.transactions.domain.usecases.GetTransactionsForAccountBetweenUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ChartsViewModel @Inject constructor(
    private val getAllAccountsWithBalanceUseCase: GetAllAccountsWithBalanceUseCase,
    private val getTransactionsBetweenUseCase: GetTransactionsBetweenUseCase,
    private val getBalanceAtDateUseCase: GetBalanceAtDateUseCase,
    private val getTransactionsForAccountBetweenUseCase: GetTransactionsForAccountBetweenUseCase,
    private val getAccountByIdUseCase: GetAccountByIdUseCase,
    private val getIncomeGroupByIdUseCase: GetIncomeGroupByIdUseCase
) : ViewModel() {
    private val _uiState = MutableStateFlow(ChartsUiState())
    val uiState = _uiState.asStateFlow()

    private val _currentMonthStart = MutableStateFlow(getStartOfCurrentMonth())
    private val _snapshotFilters = MutableStateFlow(Pair<Int?, Long?>(null, null)) // Pair<accountId, date>

    init {
        _uiState.update { it.copy(currentMonthTimestamp = _currentMonthStart.value) }
        loadAccounts()
        observeMonthDataForChart()
        observeSnapshotData()
    }

    private fun loadAccounts() {
        viewModelScope.launch {
            getAllAccountsWithBalanceUseCase().collect { accounts ->
                _uiState.update { it.copy(accountsList = accounts) }
            }
        }
    }

    private fun observeMonthDataForChart() {
        viewModelScope.launch {
            _currentMonthStart.flatMapLatest { startMonth ->
                _uiState.update { it.copy(isLoading = true, errorMessage = null) }
                val calendar = Calendar.getInstance().apply { timeInMillis = startMonth }
                calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH))
                calendar.set(Calendar.HOUR_OF_DAY, 23)
                calendar.set(Calendar.MINUTE, 59)
                calendar.set(Calendar.SECOND, 59)
                val endMonth = calendar.timeInMillis
                getTransactionsBetweenUseCase(startMonth, endMonth)
            }.collect { transactions ->
                val income = transactions.filter { it.type == "income" }.sumOf { it.amount }.toFloat()
                val expense = transactions.filter { it.type == "expense" }.sumOf { it.amount }.toFloat()
                val transfer = transactions.filter { it.type == "transfer" }.sumOf { it.amount }.toFloat()
                _uiState.update {
                    it.copy(
                        monthTransactions = transactions,
                        totalIncome = income,
                        totalExpense = expense,
                        totalTransfer = transfer,
                        isLoading = false
                    )
                }
            }
        }
    }
    private fun observeSnapshotData() {
        viewModelScope.launch {
            _snapshotFilters.flatMapLatest { (accountId, targetDate) ->
                if (accountId != null && targetDate != null) {
                    val calendar = Calendar.getInstance().apply { timeInMillis = targetDate }
                    calendar.set(Calendar.HOUR_OF_DAY, 0)
                    calendar.set(Calendar.MINUTE, 0)
                    calendar.set(Calendar.SECOND, 0)
                    val startOfDay = calendar.timeInMillis
                    calendar.set(Calendar.HOUR_OF_DAY, 23)
                    calendar.set(Calendar.MINUTE, 59)
                    calendar.set(Calendar.SECOND, 59)
                    val endOfDay = calendar.timeInMillis

                    val balance = try { getBalanceAtDateUseCase(accountId, targetDate) } catch (e:Exception) { 0.0 }
                    _uiState.update { it.copy(snapshotBalance = balance) }
                    getTransactionsForAccountBetweenUseCase(accountId, startOfDay, endOfDay)
                } else flowOf(emptyList())
            }.collect { transactions ->
                val mapped = transactions.map { TransactionWithDetails(
                    id = it.id,
                    amount = it.amount,
                    type = it.type,
                    date = it.date,
                    description = it.description,
                    accountName = getAccountByIdUseCase(it.accountId)?.name ?: "",
                    accountToName = getAccountByIdUseCase(it.transferAccountId ?: 0)?.name ?: "",
                    incomeGroupName = getIncomeGroupByIdUseCase(it.relatedIncomeId ?: 0)?.name ?: ""
                ) }
                _uiState.update { it.copy(snapshotTransactions = mapped) }
            }
        }
    }
    fun shiftMonth(amount: Int) {
        val calendar = Calendar.getInstance().apply { timeInMillis = _currentMonthStart.value }
        calendar.add(Calendar.MONTH, amount)
        val newStart = calendar.timeInMillis
        _currentMonthStart.value = newStart
        _uiState.update { it.copy(currentMonthTimestamp = newStart) }
    }

    fun selectAccount(accountId: Int, accountName: String) {
        _uiState.update { it.copy(selectedAccountId = accountId, selectedAccountName = accountName) }
        _snapshotFilters.value = Pair(accountId, _uiState.value.selectedDate)
    }

    fun selectDate(timestamp: Long) {
        _uiState.update { it.copy(selectedDate = timestamp) }
        _snapshotFilters.value = Pair(_uiState.value.selectedAccountId, timestamp)
    }

    fun clearSnapshotSearch() {
        _uiState.update {
            it.copy(
                selectedAccountId = null,
                selectedAccountName = null,
                selectedDate = null,
                snapshotBalance = null,
                snapshotTransactions = emptyList()
            )
        }
        _snapshotFilters.value = Pair(null, null)
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    private fun getStartOfCurrentMonth(): Long {
        return Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }
}