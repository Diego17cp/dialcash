package com.dialcadev.dialcash.features.transactions.domain.models

enum class TransactionType(val value: String) {
    INCOME("income"),
    EXPENSE("expense"),
    TRANSFER("transfer");

    companion object {
        fun fromString(type: String?): TransactionType {
            return entries.find { it.value == type?.lowercase() } ?: EXPENSE
        }
    }
}