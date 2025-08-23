package com.dialcadev.dialcash.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.dialcadev.dialcash.data.dto.IncomeGroupRemaining
import com.dialcadev.dialcash.data.entities.IncomeGroup
import kotlinx.coroutines.flow.Flow

@Dao
interface IncomeGroupDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(incomeGroup: IncomeGroup): Long

    @Update
    suspend fun update(incomeGroup: IncomeGroup)

    @Delete
    suspend fun delete(incomeGroup: IncomeGroup)

    @Query("SELECT * FROM income_groups ORDER BY id ASC")
    suspend fun getAllIncomeGroups(): List<IncomeGroup>

    @Query("SELECT * FROM income_groups WHERE id = :incomeGroupId LIMIT 1")
    suspend fun getIncomeGroupById(incomeGroupId: Long): IncomeGroup?

    @Query("SELECT remaining FROM income_groups WHERE id = :incomeGroupId LIMIT 1")
    suspend fun getRemainingForGroup(incomeGroupId: Int): Double?

    @Query(
        """
        SELECT ig.id, ig.name, ig.amount,
        IFNULL(SUM(
            CASE WHEN t.type = 'income' THEN t.amount
                 WHEN t.type = 'expense' THEN -t.amount
                 ELSE 0 END), 0) AS remaining
                 FROM income_groups ig
        LEFT JOIN transactions t ON ig.id = t.related_income_id
        GROUP BY ig.id
    """
    )
    fun getIncomeGroupsRemaining(): Flow<List<IncomeGroupRemaining>>
}