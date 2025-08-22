package com.dialcadev.dialcash.data.db

import androidx.room.Database
import androidx.room.RoomDatabase

// Tables or entities used in the database
import  com.dialcadev.dialcash.data.entities.*
// Data Access Objects (DAOs) for the database
import com.dialcadev.dialcash.data.dao.*

@Database(
    entities = [Account::class, IncomeGroup::class, Transaction::class, Checkpoint::class],
    version = 1,
    exportSchema = false
)

abstract class AppDB : RoomDatabase() {
    abstract fun accountDao(): AccountDao
    abstract fun transactionDao(): TransactionDao
    abstract fun checkpointDao(): CheckpointDao
    abstract fun incomeGroupDao(): IncomeGroupDao
}