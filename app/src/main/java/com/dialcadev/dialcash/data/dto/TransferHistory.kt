package com.dialcadev.dialcash.data.dto

data class TransferHistory (
    val id: Int,
    val amount: Double,
    val date: Long,
    val fromAccount: String,
    val toAccount: String,
    val description: String? = null,
)