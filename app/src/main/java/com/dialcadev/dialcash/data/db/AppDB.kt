package com.dialcadev.dialcash.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

// Tables or entities used in the database
import  com.dialcadev.dialcash.data.entities.*
// Data Access Objects (DAOs) for the database
import com.dialcadev.dialcash.data.dao.*

@Database(
    entities = [Account::class, IncomeGroup::class, Transaction::class, Checkpoint::class],
    version = 1,
    exportSchema = true
)

abstract class AppDB : RoomDatabase() {
    abstract fun accountDao(): AccountDao
    abstract fun transactionDao(): TransactionDao
    abstract fun checkpointDao(): CheckpointDao
    abstract fun incomeGroupDao(): IncomeGroupDao

    companion object {
        @Volatile
        private var INSTANCE: AppDB? = null

        fun getInstance(context: Context): AppDB {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext, AppDB::class.java, "dialcash_db"
                ).fallbackToDestructiveMigration().build().also { INSTANCE = it }
            }
        }
    }
}