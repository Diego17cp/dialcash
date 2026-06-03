package com.dialcadev.dialcash.features.accounts.domain.models

enum class FirstAccountType(
    val code: String,
) {
    CHECKING("bank"),
    SAVINGS("savings"),
    CREDIT("card"),
    CASH("cash"),
    INVESTMENT("wallet");

    companion object {
        fun fromCode(code: String): FirstAccountType {
            return entries.firstOrNull {
                it.code == code
            } ?: CHECKING
        }
    }
}