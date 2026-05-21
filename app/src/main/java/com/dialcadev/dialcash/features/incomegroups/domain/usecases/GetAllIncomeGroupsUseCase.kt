package com.dialcadev.dialcash.features.incomegroups.domain.usecases

import com.dialcadev.dialcash.features.incomegroups.data.repositories.IncomeGroupRepository
import com.dialcadev.dialcash.features.incomegroups.domain.models.IncomeGroup
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetAllIncomeGroupsUseCase @Inject constructor(
    private val repository: IncomeGroupRepository
) {
    suspend operator fun invoke(): Flow<List<IncomeGroup>> {
        return repository.getAllIncomeGroups()
    }
}