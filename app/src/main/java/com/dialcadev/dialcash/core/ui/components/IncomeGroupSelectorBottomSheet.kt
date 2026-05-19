package com.dialcadev.dialcash.core.ui.components

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.dialcadev.dialcash.R
import com.dialcadev.dialcash.features.incomegroups.domain.dtos.IncomeGroupRemaining
import com.dialcadev.dialcash.features.incomegroups.presentation.adapters.SelectorIncomeGroupAdapter
import com.google.android.material.bottomsheet.BottomSheetDialog

fun Context.showIncomeGroupsSelector(
    incomeGroupsList: List<IncomeGroupRemaining>,
    currencySymbol: String,
    onIncomeGroupSelected: (IncomeGroupRemaining) -> Unit,
    onClearBtnClicked: () -> Unit
) {
    val dialog = BottomSheetDialog(this)
    val view = LayoutInflater.from(this).inflate(
        R.layout.income_groups_selector_bottomsheet, null
    )
    val clearBtn = view.findViewById<View>(R.id.btnClearSelection)
    clearBtn.setOnClickListener {
        onClearBtnClicked()
        dialog.dismiss()
    }
    val rv = view.findViewById<RecyclerView>(R.id.rvIncomeGroups)
    rv.layoutManager = LinearLayoutManager(this)
    val adapter = SelectorIncomeGroupAdapter(
        onClick = { selected ->
            onIncomeGroupSelected(selected)
            dialog.dismiss()
        }, currencySymbol = currencySymbol
    )
    rv.adapter = adapter
    adapter.submitList(incomeGroupsList)
    dialog.setContentView(view)
    dialog.show()
}