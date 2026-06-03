package com.dialcadev.dialcash.features.checkpoints.data.repositories

import com.dialcadev.dialcash.features.checkpoints.data.dao.CheckpointDao
import com.dialcadev.dialcash.features.checkpoints.domain.models.Checkpoint
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class CheckpointRepository @Inject constructor(
    private val checkpointDao: CheckpointDao
) {
    suspend fun createCheckpoint(checkpoint: Checkpoint) = checkpointDao.insert(checkpoint.date, checkpoint.balanceSnapshot)
    suspend fun updateCheckpoint(checkpoint: Checkpoint) = checkpointDao.update(
        checkpoint.id,
        checkpoint.date,
        checkpoint.balanceSnapshot
    )
    suspend fun deleteCheckpoint(checkpoint: Checkpoint) = checkpointDao.delete(checkpoint)
    suspend fun getAllCheckpoints(): List<Checkpoint> = checkpointDao.getAllCheckpoints()
    suspend fun getCheckpointById(checkpointId: Int): Checkpoint? = checkpointDao.getCheckpointById(checkpointId)
    fun getCheckpointsBetween(startDate: Long, endDate: Long): Flow<List<Checkpoint>> = checkpointDao.getCheckpointsBetween(startDate, endDate)
    suspend fun getLastCheckpointBefore(date: Long): Checkpoint? = checkpointDao.getLastCheckpointBefore(date)
}