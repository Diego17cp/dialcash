package com.dialcadev.dialcash.features.incomegroups.presentation.viewmodels

data class CreateIncomeGroupUiState(
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false,
    val errorMessage: String? = null
)
