package com.dialcadev.dialcash.features.incomegroups.domain.usecases

import com.dialcadev.dialcash.features.incomegroups.data.repositories.IncomeGroupRepository
import javax.inject.Inject

class UpdateIncomeGroupUseCase @Inject constructor(
    private val repository: IncomeGroupRepository
) {
    suspend operator fun invoke(incomeGroupId: Int, name: String, amount: Double) : Result<Unit> {
        val existingIncomeGroup = repository.getIncomeGroupById(incomeGroupId)
            ?: return Result.failure(NoSuchElementException("IncomeGroup with id=$incomeGroupId not found"))
        val updatedIncomeGroup = existingIncomeGroup.copy(name = name, amount = amount)
        return try {
            repository.updateIncomeGroup(updatedIncomeGroup)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}