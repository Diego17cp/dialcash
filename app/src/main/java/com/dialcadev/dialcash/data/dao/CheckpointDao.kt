package com.dialcadev.dialcash.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.dialcadev.dialcash.data.entities.Checkpoint
import kotlinx.coroutines.flow.Flow

@Dao
interface CheckpointDao {

    @Query("INSERT INTO checkpoints (date, balance_snapshot) VALUES (:date, :balanceSnapshot)")
    suspend fun insert(date: Long, balanceSnapshot: String): Long

    @Query("UPDATE checkpoints SET date = :date, balance_snapshot = :balanceSnapshot WHERE id = :id")
    suspend fun update(id: Int, date: Long, balanceSnapshot: String)

    @Delete
    suspend fun delete(checkpoint: Checkpoint)

    @Query("SELECT * FROM checkpoints ORDER BY date DESC")
    suspend fun getAllCheckpoints(): List<Checkpoint>

    @Query("SELECT * FROM checkpoints WHERE id = :checkpointId LIMIT 1")
    suspend fun getCheckpointById(checkpointId: Long): Checkpoint?

    @Query("SELECT * FROM checkpoints WHERE date BETWEEN :startDate AND :endDate ORDER BY date ASC")
    fun getCheckpointsBetween(startDate: Long, endDate: Long): Flow<List<Checkpoint>>

    @Query("SELECT * FROM checkpoints WHERE date <= :date ORDER BY date DESC LIMIT 1")
    suspend fun getLastCheckpointBefore(date: Long): Checkpoint?
}