package com.dialcadev.dialcash.data.dto

data class AccountBalance (
    val id: Int,
    val name: String,
    val type: String, // "bank" | "card" | "cash" |
    val balance: Double,
)