package com.dialcadev.dialcash.features.incomegroups.domain.usecases

import com.dialcadev.dialcash.features.incomegroups.data.repositories.IncomeGroupRepository
import com.dialcadev.dialcash.features.incomegroups.domain.dtos.IncomeGroupRemaining
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetAllIncomeGroupsWithRemainingUseCase @Inject constructor(
    private val repository: IncomeGroupRepository
) {
    operator fun invoke(): Flow<List<IncomeGroupRemaining>> {
        return repository.getAllIncomeGroupsWithRemaining()
    }
}