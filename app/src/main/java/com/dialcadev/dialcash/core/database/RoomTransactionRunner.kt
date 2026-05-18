package com.dialcadev.dialcash.core.database

import androidx.room.withTransaction
import com.dialcadev.dialcash.core.domain.DatabaseTransactionRunner
import javax.inject.Inject

class RoomTransactionRunner @Inject constructor(
    private val db: AppDB
): DatabaseTransactionRunner {
    override suspend operator fun <T> invoke(block: suspend () -> T): T {
        return db.withTransaction { block() }
    }
}