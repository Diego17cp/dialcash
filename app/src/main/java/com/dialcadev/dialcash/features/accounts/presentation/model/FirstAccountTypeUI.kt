package com.dialcadev.dialcash.features.accounts.presentation.model

import com.dialcadev.dialcash.features.accounts.domain.models.FirstAccountType

data class FirstAccountTypeUI(
    val type: FirstAccountType,
    val labelRes: Int
)
