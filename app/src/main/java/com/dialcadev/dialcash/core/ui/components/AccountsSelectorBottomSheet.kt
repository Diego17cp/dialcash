package com.dialcadev.dialcash.core.ui.components

import android.content.Context
import android.view.LayoutInflater
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.dialcadev.dialcash.R
import com.dialcadev.dialcash.features.accounts.domain.dtos.AccountBalanceWithOriginal
import com.dialcadev.dialcash.features.accounts.presentation.adapter.SelectorAccountAdapter
import com.google.android.material.bottomsheet.BottomSheetDialog

fun Context.showAccountSelector(
    accountsList: List<AccountBalanceWithOriginal>,
    currencySymbol: String,
    onAccountSelected: (AccountBalanceWithOriginal) -> Unit
) {
    val dialog = BottomSheetDialog(this)
    val view = LayoutInflater.from(this).inflate(
        R.layout.accounts_selector_bottomsheet,
        null
    )
    val rv = view.findViewById<RecyclerView>(R.id.rvAccounts)
    rv.layoutManager = LinearLayoutManager(this)

    val adapter = SelectorAccountAdapter(
        onClick = { selected ->
            onAccountSelected(selected)
            dialog.dismiss()
        },
        currencySymbol = currencySymbol
    )

    rv.adapter = adapter
    adapter.submitList(accountsList)

    dialog.setContentView(view)
    dialog.show()
}