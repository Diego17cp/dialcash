package com.dialcadev.dialcash.features.incomegroups.presentation.viewmodels

import com.dialcadev.dialcash.features.incomegroups.domain.dtos.IncomeGroupRemaining

data class AllIncomeGroupsUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val incomeGroups: List<IncomeGroupRemaining> = emptyList()
)
