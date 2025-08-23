package com.dialcadev.dialcash.data.dto

data class IncomeGroupRemaining (
    val id: Int,
    val name: String,
    val amount: Double, // Total amount allocated to the income group
    val remaining: Double, // Remaining amount in the income group
)