package com.dialcadev.dialcash.features.home.presentation.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dialcadev.dialcash.features.accounts.domain.models.Account
import com.dialcadev.dialcash.features.accounts.domain.usecases.DeleteAccountUseCase
import com.dialcadev.dialcash.features.accounts.domain.usecases.DeleteTransactionUseCase
import com.dialcadev.dialcash.features.accounts.domain.usecases.GetAllAccountsUseCase
import com.dialcadev.dialcash.features.accounts.domain.usecases.GetMainAccountBalancesUseCase
import com.dialcadev.dialcash.features.accounts.domain.usecases.UpdateAccountUseCase
import com.dialcadev.dialcash.features.incomegroups.domain.usecases.GetAllIncomeGroupsUseCase
import com.dialcadev.dialcash.features.transactions.domain.models.Transaction
import com.dialcadev.dialcash.features.transactions.domain.usecases.GetRecentTransactionsUseCase
import com.dialcadev.dialcash.features.transactions.domain.usecases.UpdateTransactionUseCase
import com.dialcadev.dialcash.features.transactions.domain.usecases.ValidateExpenseEditUseCase
import com.dialcadev.dialcash.features.transactions.domain.usecases.ValidateTransferEditUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val getMainAccountBalancesUseCase: GetMainAccountBalancesUseCase,
    private val getRecentTransactionsUseCase: GetRecentTransactionsUseCase,
    private val getAllAccountsUseCase: GetAllAccountsUseCase,
    private val getAllIncomeGroupsUseCase: GetAllIncomeGroupsUseCase,
    private val updateAccountUseCase: UpdateAccountUseCase,
    private val deleteAccountUseCase: DeleteAccountUseCase,
    private val updateTransactionUseCase: UpdateTransactionUseCase,
    private val deleteTransactionUseCase: DeleteTransactionUseCase,
    private val validateExpenseEditUseCase: ValidateExpenseEditUseCase,
    private val validateTransferEditUseCase: ValidateTransferEditUseCase
) : ViewModel() {
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadHomeData()
    }

    private fun loadHomeData() {
        _uiState.update {
            it.copy(
                isLoading = true,
                errorMessage = null
            )
        }
        viewModelScope.launch {
            try {
                combine(
                    getMainAccountBalancesUseCase(),
                    getRecentTransactionsUseCase(10),
                    getAllAccountsUseCase(),
                    getAllIncomeGroupsUseCase()
                ) { mainAccounts, recentTransactions, allAccounts, allIncomeGroups ->
                    val total = mainAccounts.sumOf { it.balance }
                    HomeUiState(
                        totalBalance = total,
                        mainAccounts = mainAccounts,
                        recentTransactions = recentTransactions,
                        accounts = allAccounts,
                        incomeGroups = allIncomeGroups,
                        isLoading = false,
                        errorMessage = null
                    )
                }.collect { newState ->
                    _uiState.value = newState
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Error loading data: ${e.message}"
                    )
                }
            }
        }
    }

    fun refreshData() {
        loadHomeData()
    }

    fun deleteAccount(account: Account) {
        viewModelScope.launch {
            deleteAccountUseCase(account.id).onFailure { error ->
                _uiState.update { it.copy(errorMessage = "Error deleting account: ${error.message}") }
            }
        }
    }

    fun updateAccount(account: Account) {
        viewModelScope.launch {
            updateAccountUseCase(
                accountId = account.id,
                name = account.name,
                type = account.type,
                balance = account.balance,
            ).onFailure { error ->
                _uiState.update { it.copy(errorMessage = "Error updating account: ${error.message}") }
            }
        }
    }

    fun updateTransaction(transaction: Transaction) {
        viewModelScope.launch {
            updateTransactionUseCase(
                transactionId = transaction.id,
                amount = transaction.amount,
                date = transaction.date,
                description = transaction.description?.trim() ?: "",
                toAccountId = transaction.accountId,
                transferAccountId = transaction.transferAccountId,
                relatedIncomeId = transaction.relatedIncomeId
            ).onFailure { error ->
                _uiState.update { it.copy(errorMessage = "Error updating transaction: ${error.message}") }
            }
        }
    }

    fun deleteTransaction(transaction: Transaction) {
        viewModelScope.launch {
            deleteTransactionUseCase(transaction.id).onFailure { error ->
                _uiState.update { it.copy(errorMessage = "Error deleting transaction: ${error.message}") }
            }
        }
    }

    fun validateTransactionBalance(
        transactionId: Int,
        type: String,
        accountId: Int,
        amount: Double,
        accountToId: Int?,
        incomeGroupId: Int?,
        onResult: (Boolean, String?) -> Unit
    ) {
        viewModelScope.launch {
            val result = when (type) {
                "expense" -> validateExpenseEditUseCase(
                    transactionId,
                    accountId,
                    amount,
                    incomeGroupId
                )

                "transfer" -> validateTransferEditUseCase(
                    transactionId,
                    accountId,
                    accountToId ?: 0,
                    amount
                )

                else -> Result.success(Unit)
            }
            result.fold(
                onSuccess = { onResult(true, null) },
                onFailure = { error ->
                    onResult(
                        false,
                        "Error validating transaction: ${error.message}"
                    )
                }
            )
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}