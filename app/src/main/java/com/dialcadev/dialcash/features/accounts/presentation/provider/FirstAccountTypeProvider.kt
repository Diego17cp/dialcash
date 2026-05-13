package com.dialcadev.dialcash.features.accounts.presentation.provider

import com.dialcadev.dialcash.R
import com.dialcadev.dialcash.features.accounts.domain.models.FirstAccountType
import com.dialcadev.dialcash.features.accounts.presentation.model.FirstAccountTypeUI

object FirstAccountTypeProvider {
    val items = listOf(
        FirstAccountTypeUI(
            FirstAccountType.CHECKING,
            R.string.account_type_bank_account
        ),
        FirstAccountTypeUI(
            FirstAccountType.SAVINGS,
            R.string.account_type_savings_account
        ),
        FirstAccountTypeUI(
            FirstAccountType.CREDIT,
            R.string.account_type_credit_card
        ),
        FirstAccountTypeUI(
            FirstAccountType.CASH,
            R.string.account_type_cash
        ),
        FirstAccountTypeUI(
            FirstAccountType.INVESTMENT,
            R.string.account_type_wallet
        )
    )
}