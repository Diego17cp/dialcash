package com.dialcadev.dialcash.data.dto

data class TransactionWithDetails (
    val id: Int,
    val amount: Double,
    val type: String, // "income" | "expense" | "transfer"
    val date: Long, // Timestamp in milliseconds
    val description: String? = null,
    val accountName: String,
    val incomeGroupName: String? = null,
    val accountToName: String? = null
)