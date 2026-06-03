package com.dialcadev.dialcash.features.incomegroups.domain.usecases

import com.dialcadev.dialcash.features.incomegroups.data.repositories.IncomeGroupRepository
import javax.inject.Inject

class GetIncomeGroupByIdUseCase @Inject constructor(
    private val repository: IncomeGroupRepository
) {
    suspend operator fun invoke(incomeGroupId: Int) = repository.getIncomeGroupById(incomeGroupId)
}