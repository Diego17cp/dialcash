package com.dialcadev.dialcash.features.accounts.domain.dtos

data class AccountBalanceWithOriginal(
    val id: Int,
    val name: String,
    val type: String,
    val balance: Double,
    val originalBalance: Double,
    val createdAt: String?
)
