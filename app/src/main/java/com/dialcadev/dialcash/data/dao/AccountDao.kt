package com.dialcadev.dialcash.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.dialcadev.dialcash.data.dto.AccountBalance

import com.dialcadev.dialcash.data.entities.Account
import kotlinx.coroutines.flow.Flow

@Dao
interface AccountDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(account: Account): Long

    @Update
    suspend fun updateAccount(account: Account)

    @Delete
    suspend fun delete(account: Account)

    @Query("SELECT * FROM accounts ORDER BY id ASC")
    fun getAllAccounts(): Flow<List<Account>>

    @Query("SELECT * FROM accounts WHERE id = :accountId LIMIT 1")
    suspend fun getAccountById(accountId: Long): Account?

    @Query("SELECT * FROM accounts WHERE name LIKE :name LIMIT 1")
    suspend fun getAccountByName(name: String): Account?

    @Query("SELECT COUNT(*) FROM accounts")
    suspend fun getAccountCount(): Int

    @Query("SELECT * FROM accounts WHERE type IN ('bank', 'card', 'cash', 'wallet') ORDER BY name ASC")
    fun getMainAccounts(): Flow<List<Account>>

    @Query("""
    SELECT a.id, a.name, a.type,
    IFNULL(SUM(
        CASE WHEN t.type = 'income' THEN t.amount
             WHEN t.type = 'expense' THEN -t.amount
             WHEN t.type = 'transfer' AND t.account_id = a.id THEN -t.amount
             WHEN t.type = 'transfer' AND t.transfer_account_id = a.id THEN t.amount
             ELSE 0 END), 0) AS saldo
    FROM accounts a
    LEFT JOIN transactions t ON (a.id = t.account_id OR a.id = t.transfer_account_id)
    GROUP BY a.id
""")
    fun getAccountBalances(): Flow<List<AccountBalance>>
}