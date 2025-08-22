package com.dialcadev.dialcash.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "income_groups")
data class IncomeGroup (
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val name: String,
    val amount: Double = 0.0,
    val remaining: Double = 0.0,
)