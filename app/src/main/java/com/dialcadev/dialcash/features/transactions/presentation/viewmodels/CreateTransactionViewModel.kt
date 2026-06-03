package com.dialcadev.dialcash.features.transactions.presentation.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dialcadev.dialcash.R
import com.dialcadev.dialcash.core.datastore.UserDataStore
import com.dialcadev.dialcash.features.accounts.data.repositories.AccountRepository
import com.dialcadev.dialcash.features.accounts.domain.dtos.AccountBalanceWithOriginal
import com.dialcadev.dialcash.features.incomegroups.data.repositories.IncomeGroupRepository
import com.dialcadev.dialcash.features.incomegroups.domain.dtos.IncomeGroupRemaining
import com.dialcadev.dialcash.features.transactions.domain.models.TransactionType
import com.dialcadev.dialcash.features.transactions.domain.usecases.AddExpenseUseCase
import com.dialcadev.dialcash.features.transactions.domain.usecases.AddIncomeUseCase
import com.dialcadev.dialcash.features.transactions.domain.usecases.MakeTransferUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CreateTransactionViewModel @Inject constructor(
    private val userDataStore: UserDataStore,
    private val accountRepository: AccountRepository,
    private val incomeGroupRepository: IncomeGroupRepository,
    private val addIncomeUseCase: AddIncomeUseCase,
    private val addExpenseUseCase: AddExpenseUseCase,
    private val makeTransferUseCase: MakeTransferUseCase
) : ViewModel() {
    private val _uiState = MutableStateFlow(CreateTransactionUiState())
    val uiState: StateFlow<CreateTransactionUiState> = _uiState

    init {
        loadInitialData()
    }

    private fun loadInitialData() {
        viewModelScope.launch {
            userDataStore.getUserData().collect { userPreferences ->
                _uiState.value = _uiState.value.copy(
                    currencySymbol = userPreferences.currencySymbol
                )
            }
        }
        viewModelScope.launch {
            accountRepository.getAllAccountBalances().collect { accounts ->
                _uiState.value = _uiState.value.copy(
                    accounts = accounts
                )
            }
        }
        viewModelScope.launch {
            incomeGroupRepository.getAllIncomeGroupsWithRemaining().collect { incomeGroups ->
                _uiState.value = _uiState.value.copy(
                    incomeGroups = incomeGroups
                )
            }
        }
    }

    fun onAmountChanged(amount: String) {
        val amountValue = amount.toDoubleOrNull()
        val error = when {
            amount.isBlank() -> R.string.amount_required
            amountValue == null || amountValue <= 0 -> R.string.enter_valid_amount
            else -> null
        }
        _uiState.value = _uiState.value.copy(
            amount = amount,
            amountError = error
        )
    }

    fun onDescriptionChanged(description: String) {
        val error = if (description.isBlank()) R.string.description_required else null
        _uiState.value = _uiState.value.copy(
            description = description,
            descriptionError = error
        )
    }

    fun onDateSelected(date: Long) {
        _uiState.value = _uiState.value.copy(
            date = date
        )
    }

    fun onAccountFromSelected(account: AccountBalanceWithOriginal) {
        _uiState.value = _uiState.value.copy(
            selectedAccountFrom = account
        )
    }

    fun onAccountToSelected(account: AccountBalanceWithOriginal) {
        _uiState.value = _uiState.value.copy(
            selectedAccountTo = account
        )
    }

    fun onIncomeGroupSelected(incomeGroup: IncomeGroupRemaining?) {
        _uiState.value = _uiState.value.copy(
            selectedIncomeGroup = incomeGroup
        )
    }

    fun saveTransaction(type: TransactionType) {
        val state = _uiState.value
        if (state.amountError != null || state.descriptionError != null || state.amount.isBlank() || state.description.isBlank()) {
            onAmountChanged(state.amount)
            onDescriptionChanged(state.description)
            return
        }
        viewModelScope.launch {
            _uiState.value = state.copy(isLoading = true, errorMessage = null)
            val parsedAmount = state.amount.toDoubleOrNull() ?: 0.0
            val result = when (type) {
                TransactionType.INCOME -> addIncomeUseCase(
                    accountId = state.selectedAccountFrom?.id,
                    amount = parsedAmount,
                    description = state.description,
                    date = state.date
                )
                TransactionType.EXPENSE -> addExpenseUseCase(
                    accountId = state.selectedAccountFrom?.id,
                    amount = parsedAmount,
                    description = state.description,
                    date = state.date,
                    relatedIncomeId = state.selectedIncomeGroup?.id
                )
                TransactionType.TRANSFER -> makeTransferUseCase(
                    fromAccountId = state.selectedAccountFrom?.id,
                    toAccountId = state.selectedAccountTo?.id,
                    amount = parsedAmount,
                    description = state.description,
                    date = state.date
                )
                else -> Result.failure(IllegalArgumentException("Invalid transaction type"))
            }
            result.fold(
                onSuccess = {
                    _uiState.value = state.copy(isLoading = false, isSuccess = true)
                },
                onFailure = { e ->
                    _uiState.value = state.copy(isLoading = false, errorMessage = e.message ?: "An error occurred")
                }
            )
        }
    }
}