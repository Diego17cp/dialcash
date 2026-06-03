package com.dialcadev.dialcash.features.incomegroups.data.repositories

import com.dialcadev.dialcash.features.incomegroups.data.dao.IncomeGroupDao
import com.dialcadev.dialcash.features.incomegroups.domain.dtos.IncomeGroupRemaining
import com.dialcadev.dialcash.features.incomegroups.domain.models.IncomeGroup
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class IncomeGroupRepository @Inject constructor(
    private val incomeGroupDao: IncomeGroupDao
) {
    suspend fun createIncomeGroup(incomeGroup: IncomeGroup) =
        incomeGroupDao.insert(incomeGroup.name, incomeGroup.amount)

    suspend fun updateIncomeGroup(incomeGroup: IncomeGroup) = incomeGroupDao.update(
        incomeGroup.id,
        incomeGroup.name,
        incomeGroup.amount
    )

    suspend fun deleteIncomeGroup(incomeGroup: IncomeGroup) = incomeGroupDao.delete(incomeGroup)
    fun getAllIncomeGroupsWithRemaining(): Flow<List<IncomeGroupRemaining>> =
        incomeGroupDao.getIncomeGroupsRemaining()

    fun getAllIncomeGroups(): Flow<List<IncomeGroup>> = incomeGroupDao.getAllIncomeGroups()
    suspend fun getRemainingForIncomeGroup(incomeGroupId: Int): Double {
        return incomeGroupDao.getRemainingAmount(incomeGroupId) ?: 0.0
    }

    suspend fun getIncomeGroupById(incomeGroupId: Int): IncomeGroup? =
        incomeGroupDao.getIncomeGroupById(incomeGroupId)
}