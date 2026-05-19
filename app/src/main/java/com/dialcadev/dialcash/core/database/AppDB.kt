package com.dialcadev.dialcash.core.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.dialcadev.dialcash.features.accounts.domain.models.Account
import com.dialcadev.dialcash.features.transactions.domain.models.Transaction
import com.dialcadev.dialcash.features.incomegroups.domain.models.IncomeGroup
import com.dialcadev.dialcash.features.checkpoints.domain.models.Checkpoint
import com.dialcadev.dialcash.features.accounts.data.dao.AccountDao
import com.dialcadev.dialcash.features.checkpoints.data.dao.CheckpointDao
import com.dialcadev.dialcash.features.transactions.data.dao.TransactionDao
import com.dialcadev.dialcash.features.incomegroups.data.dao.IncomeGroupDao

@Database(
    entities = [
        Account::class,
        Transaction::class,
        IncomeGroup::class,
        Checkpoint::class
    ],
    version = 3,
    exportSchema = true
)

abstract class AppDB: RoomDatabase() {
    abstract fun accountDao(): AccountDao
    abstract fun transactionDao(): TransactionDao
    abstract fun incomeGroupDao(): IncomeGroupDao
    abstract fun checkpointDao(): CheckpointDao

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