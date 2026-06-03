package com.dialcadev.dialcash.features.accounts.presentation.provider

import com.dialcadev.dialcash.R
import com.dialcadev.dialcash.features.accounts.presentation.model.AccountTypeUI

object AccountTypeUIProvider {
    fun getItems(): List<AccountTypeUI> {
        return listOf(
            AccountTypeUI("bank", R.string.account_type_bank_account, R.drawable.ic_building_bank),
            AccountTypeUI("card", R.string.account_type_credit_card, R.drawable.ic_card),
            AccountTypeUI("cash", R.string.account_type_cash, R.drawable.ic_cash),
            AccountTypeUI("wallet", R.string.account_type_wallet, R.drawable.ic_accounts_filled),
            AccountTypeUI("savings", R.string.savings, R.drawable.ic_bank),
            AccountTypeUI("debt", R.string.account_type_debt, R.drawable.ic_debt_payment),
            AccountTypeUI("other", R.string.account_type_other, R.drawable.ic_account_default)
        )
    }
}