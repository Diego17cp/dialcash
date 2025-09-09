package com.dialcadev.dialcash.data.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "transactions", foreignKeys = [
        ForeignKey(
            entity = Account::class,
            parentColumns = ["id"],
            childColumns = ["account_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = IncomeGroup::class,
            parentColumns = ["id"],
            childColumns = ["related_income_id"],
            onDelete = ForeignKey.SET_NULL
        ),
        ForeignKey(
            entity = Account::class,
            parentColumns = ["id"],
            childColumns = ["transfer_account_id"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        androidx.room.Index(value = ["account_id"]),
        androidx.room.Index(value = ["related_income_id"]),
        androidx.room.Index(value = ["transfer_account_id"])
    ]
)
data class Transaction(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    @ColumnInfo(name = "account_id")
    val accountId: Int,
    val type: String, // "income" | "expense" | "transfer"
    val amount: Double,
    val date: Long = System.currentTimeMillis(), // Timestamp in milliseconds
    val description: String? = null,
    @ColumnInfo(name = "related_income_id")
    val relatedIncomeId: Int? = null, // For expenses linked to an income group
    @ColumnInfo(name = "transfer_account_id")
    val transferAccountId: Int? = null, // For transfers, the other account involved
    @ColumnInfo(name = "created_at", defaultValue = "CURRENT_TIMESTAMP")
    val createdAt: String? = null
)