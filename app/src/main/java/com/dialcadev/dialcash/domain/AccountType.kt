package com.dialcadev.dialcash.domain

import com.dialcadev.dialcash.R
import com.dialcadev.dialcash.data.dto.AccountTypeUI

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

val accountTypes = listOf<AccountTypeUI>(
    AccountTypeUI("bank", R.string.account_type_bank_account, R.drawable.ic_building_bank),
    AccountTypeUI("card", R.string.account_type_credit_card, R.drawable.ic_card),
    AccountTypeUI("cash", R.string.account_type_cash, R.drawable.ic_cash),
    AccountTypeUI("wallet", R.string.account_type_wallet, R.drawable.ic_accounts_filled),
    AccountTypeUI("savings", R.string.savings, R.drawable.ic_bank),
    AccountTypeUI("debt", R.string.account_type_debt, R.drawable.ic_debt_payment),
    AccountTypeUI("other", R.string.account_type_other, R.drawable.ic_account_default)
)
