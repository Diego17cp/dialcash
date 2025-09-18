package com.dialcadev.dialcash.domain

import android.content.Context
import com.dialcadev.dialcash.R

enum class FirstAccountType(val code: String, val labelRes: Int) {
    CHECKING("bank", R.string.account_type_bank_account),
    SAVINGS("savings", R.string.account_type_savings_account),
    CREDIT("card", R.string.account_type_credit_card),
    CASH("cash", R.string.account_type_cash),
    INVESTMENT("wallet", R.string.account_type_wallet);

    companion object {
        fun fromCode(code: String) = values().firstOrNull { it.code == code } ?: CHECKING

        fun fromLabel(context: Context, label: String?): FirstAccountType? {
            if (label == null) return null
            return values().firstOrNull { context.getString(it.labelRes) == label }
        }

        fun labels(context: Context) = values().map { context.getString(it.labelRes) }
    }
}