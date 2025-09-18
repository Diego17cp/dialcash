package com.dialcadev.dialcash.domain

import com.dialcadev.dialcash.R

enum class AccountType(val code: String, val labelRes: Int) {
    BANK("bank", R.string.account_type_bank_account),
    CARD("card", R.string.account_type_credit_card),
    CASH("cash", R.string.account_type_cash),
    WALLET("wallet", R.string.account_type_wallet),
    DEBT("debt", R.string.account_type_debt),
    SAVINGS("savings", R.string.account_type_savings_account),
    OTHER("other", R.string.account_type_other);

    companion object {
        fun byCode(code: String) = AccountType.entries.firstOrNull { it.code == code } ?: BANK
    }
}