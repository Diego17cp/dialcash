package com.dialcadev.dialcash.data.dto

data class IncomeGroupRemaining (
    val id: Int,
    val name: String,
    val restante: Double, // Remaining amount in the income group
)