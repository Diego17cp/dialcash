package com.dialcadev.dialcash.core.database

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import com.dialcadev.dialcash.features.accounts.data.dao.AccountDao
import com.dialcadev.dialcash.data.dao.CheckpointDao
import com.dialcadev.dialcash.data.dao.IncomeGroupDao
import com.dialcadev.dialcash.data.dao.TransactionDao

abstract class AppDB: RoomDatabase() {
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