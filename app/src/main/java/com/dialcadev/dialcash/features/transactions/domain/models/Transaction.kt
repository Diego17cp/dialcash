package com.dialcadev.dialcash.features.transactions.domain.models

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.dialcadev.dialcash.features.accounts.domain.models.Account
import com.dialcadev.dialcash.features.incomegroups.domain.models.IncomeGroup

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
        Index(value = ["account_id"]),
        Index(value = ["related_income_id"]),
        Index(value = ["transfer_account_id"])
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