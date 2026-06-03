package com.dialcadev.dialcash.features.incomegroups.domain.usecases

import com.dialcadev.dialcash.features.incomegroups.data.repositories.IncomeGroupRepository
import javax.inject.Inject

class DeleteIncomeGroupUseCase @Inject constructor(
    private val repository: IncomeGroupRepository
) {
    suspend operator fun invoke(incomeGroupId: Int) : Result<Unit> {
        return try {
            val existingIncomeGroup = repository.getIncomeGroupById(incomeGroupId)
                ?: return Result.failure(NoSuchElementException("IncomeGroup with id=$incomeGroupId not found"))
            repository.deleteIncomeGroup(existingIncomeGroup)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}