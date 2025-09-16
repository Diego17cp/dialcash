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

    @Query("INSERT INTO income_groups (name, amount) VALUES (:name, :amount)")
    suspend fun insert(name: String, amount: Double): Long

    @Query("UPDATE income_groups SET name = :name, amount = :amount WHERE id = :id")
    suspend fun update(id: Int, name: String, amount: Double)

    @Delete
    suspend fun delete(incomeGroup: IncomeGroup)

    @Query("SELECT * FROM income_groups ORDER BY id ASC")
    fun getAllIncomeGroups(): Flow<List<IncomeGroup>>

    @Query("SELECT * FROM income_groups WHERE id = :incomeGroupId LIMIT 1")
    suspend fun getIncomeGroupById(incomeGroupId: Int): IncomeGroup?

    @Query(
        """
        SELECT ig.id, ig.name, ig.amount, ig.created_at as createdAt,
        ig.amount + IFNULL(SUM(
            CASE WHEN t.type = 'income' THEN t.amount
                 WHEN t.type = 'expense' THEN -t.amount
                 ELSE 0 END), 0) AS remaining
                 FROM income_groups ig
        LEFT JOIN transactions t ON ig.id = t.related_income_id
        GROUP BY ig.id
    """
    )
    fun getIncomeGroupsRemaining(): Flow<List<IncomeGroupRemaining>>

    @Query("""
        SELECT ig.amount - COALESCE(SUM(t.amount), 0) AS remaining
        FROM income_groups ig
        LEFT JOIN transactions t ON t.related_income_id = ig.id AND t.type = 'expense'
        WHERE ig.id = :incomeGroupId
    """)
    suspend fun getRemainingAmount(incomeGroupId: Int): Double?
}