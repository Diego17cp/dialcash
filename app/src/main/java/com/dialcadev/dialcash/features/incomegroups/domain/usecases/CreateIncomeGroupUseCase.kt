package com.dialcadev.dialcash.features.incomegroups.domain.usecases

import com.dialcadev.dialcash.features.incomegroups.data.repositories.IncomeGroupRepository
import com.dialcadev.dialcash.features.incomegroups.domain.models.IncomeGroup
import javax.inject.Inject

class CreateIncomeGroupUseCase @Inject constructor(
    private val repository: IncomeGroupRepository
) {
    suspend operator fun invoke(name: String, amount: Double) : Result<Unit> {
        if (name.isBlank()) return Result.failure(IllegalArgumentException("Income group name cannot be empty"))
        if (amount < 0) return Result.failure(IllegalArgumentException("Amount cannot be negative"))

        val incomeGroup = IncomeGroup(
            name = name.trim(),
            amount = amount
        )
        return try {
            repository.createIncomeGroup(incomeGroup)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}