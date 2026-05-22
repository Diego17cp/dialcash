package com.dialcadev.dialcash.core.ui.components

import android.app.DatePickerDialog
import android.content.Context
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.animation.OvershootInterpolator
import android.view.inputmethod.EditorInfo
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import androidx.core.widget.addTextChangedListener
import com.dialcadev.dialcash.R
import com.dialcadev.dialcash.core.utils.extensions.toReadableDate
import com.dialcadev.dialcash.databinding.BottomSheetFiltersBinding
import com.dialcadev.dialcash.features.accounts.domain.models.Account
import com.dialcadev.dialcash.features.transactions.presentation.viewmodels.TransactionFilters
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.card.MaterialCardView
import com.google.android.material.chip.Chip
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

fun Context.showTransactionFiltersBottomSheet(
    currentFilters: TransactionFilters,
    accounts: List<Account>,
    onApplyFilters: (TransactionFilters) -> Unit,
) {
    val dialog = BottomSheetDialog(this)
    val binding = BottomSheetFiltersBinding.inflate(LayoutInflater.from(this))
    dialog.setContentView(binding.root)

    var localFilters = currentFilters.copy()
    val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    binding.etSearch.setText(localFilters.searchQuery)
    binding.etSearch.setOnEditorActionListener { _, actionId, _ ->
        if (actionId == EditorInfo.IME_ACTION_SEARCH) {
            localFilters = localFilters.copy(searchQuery = binding.etSearch.text.toString())
            onApplyFilters(localFilters)
            dialog.dismiss()
            true
        } else false
    }

    val selectedTypes = localFilters.transactionTypes.toMutableSet()
    fun updateCardState(
        card: MaterialCardView,
        check: ImageView,
        title: TextView,
        isSelected: Boolean
    ) {
        if (isSelected) {
            val primaryColor = ContextCompat.getColor(this, R.color.colorPrimary)
            val transparentBg = ColorUtils.setAlphaComponent(primaryColor, (0.10f * 255).toInt())
            card.setCardBackgroundColor(transparentBg)
            card.strokeColor = primaryColor
            card.strokeWidth = 5
            check.visibility = View.VISIBLE
            title.setTextColor(primaryColor)
            check.scaleX = 0f
            check.scaleY = 0f
            check.animate().scaleX(1f).scaleY(1f).setDuration(140)
                .setInterpolator(OvershootInterpolator()).start()
        } else {
            card.setCardBackgroundColor(ContextCompat.getColor(this, R.color.background))
            val typedValue = TypedValue()
            theme.resolveAttribute(com.google.android.material.R.attr.colorOutline, typedValue, true)
            card.strokeColor = typedValue.data
            card.strokeWidth = 4
            check.visibility = View.GONE
            title.setTextColor(ContextCompat.getColor(this, R.color.text_primary))
        }
    }
    fun applyTypeState() {
        localFilters = localFilters.copy(transactionTypes = selectedTypes.toList())
        onApplyFilters(localFilters)
    }

    updateCardState(binding.incomeSelectorCard, binding.checkIncomeSelector, binding.titleIncomeSelector, selectedTypes.contains("INCOME"))
    updateCardState(binding.expenseSelectorCard, binding.checkExpenseSelector, binding.titleExpenseSelector, selectedTypes.contains("EXPENSE"))
    updateCardState(binding.transferSelectorCard, binding.checkTransferSelector, binding.titleTransferSelector, selectedTypes.contains("TRANSFER"))

    binding.incomeSelectorCard.setOnClickListener {
        val isSelected = selectedTypes.contains("INCOME")
        if (isSelected) selectedTypes.remove("INCOME") else selectedTypes.add("INCOME")
        updateCardState(binding.incomeSelectorCard, binding.checkIncomeSelector, binding.titleIncomeSelector, !isSelected)
        applyTypeState()
    }
    binding.expenseSelectorCard.setOnClickListener {
        val isSelected = selectedTypes.contains("EXPENSE")
        if (isSelected) selectedTypes.remove("EXPENSE") else selectedTypes.add("EXPENSE")
        updateCardState(binding.expenseSelectorCard, binding.checkExpenseSelector, binding.titleExpenseSelector, !isSelected)
        applyTypeState()
    }
    binding.transferSelectorCard.setOnClickListener {
        val isSelected = selectedTypes.contains("TRANSFER")
        if (isSelected) selectedTypes.remove("TRANSFER") else selectedTypes.add("TRANSFER")
        updateCardState(binding.transferSelectorCard, binding.checkTransferSelector, binding.titleTransferSelector, !isSelected)
        applyTypeState()
    }

    localFilters.startDate?.let { timestamp ->
        val dateStr = dateFormat.format(timestamp)
        binding.tvStartDateValue.text = dateStr.toReadableDate()
        binding.etStartDate.setText(dateStr)
        binding.chevronStartDate.visibility = View.GONE
    }
    localFilters.endDate?.let { timestamp ->
        val dateStr = dateFormat.format(timestamp)
        binding.tvEndDateValue.text = dateStr.toReadableDate()
        binding.etEndDate.setText(dateStr)
        binding.chevronEndDate.visibility = View.GONE
    }

    fun openDatePicker(isStart: Boolean) {
        val calendar = Calendar.getInstance()
        val datePickerDialog = DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->
                calendar.set(year, month, dayOfMonth)
                if (isStart) {
                    calendar.set(Calendar.HOUR_OF_DAY, 0)
                    calendar.set(Calendar.MINUTE, 0)
                    calendar.set(Calendar.SECOND, 0)
                    calendar.set(Calendar.MILLISECOND, 0)
                } else {
                    calendar.set(Calendar.HOUR_OF_DAY, 23)
                    calendar.set(Calendar.MINUTE, 59)
                    calendar.set(Calendar.SECOND, 59)
                    calendar.set(Calendar.MILLISECOND, 999)
                }
                val selectedTimestamp = calendar.timeInMillis
                val formattedDate = dateFormat.format(calendar.time)

                if (isStart) {
                    binding.tvStartDateValue.text = formattedDate.toReadableDate()
                    binding.chevronStartDate.visibility = View.GONE
                    binding.etStartDate.setText(formattedDate)
                    localFilters = localFilters.copy(startDate = selectedTimestamp)
                } else {
                    binding.tvEndDateValue.text = formattedDate.toReadableDate()
                    binding.chevronEndDate.visibility = View.GONE
                    binding.etEndDate.setText(formattedDate)
                    localFilters = localFilters.copy(endDate = selectedTimestamp)
                }
                onApplyFilters(localFilters)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
        datePickerDialog.show()
    }

    binding.startDateCard.setOnClickListener { openDatePicker(true) }
    binding.endDateCard.setOnClickListener { openDatePicker(false) }

    binding.etEndDate.addTextChangedListener { date ->
        if (date.isNullOrEmpty()) {
            binding.chevronEndDate.visibility = View.VISIBLE
            binding.tvEndDateValue.text = "DD/MM/YYYY"
        } else {
            binding.tvEndDateValue.text = date.toString().toReadableDate()
            binding.chevronEndDate.visibility = View.GONE
        }
    }
    binding.etStartDate.addTextChangedListener { date ->
        if (date.isNullOrEmpty()) {
            binding.chevronStartDate.visibility = View.VISIBLE
            binding.tvStartDateValue.text = "DD/MM/YYYY"
        } else {
            binding.tvStartDateValue.text = date.toString().toReadableDate()
            binding.chevronStartDate.visibility = View.GONE
        }
    }

    val allChipId = View.generateViewId()

    fun applyAccountsFilter() {
        val selected = mutableListOf<String>()
        for (j in 0 until binding.chipGroupAccounts.childCount) {
            val c = binding.chipGroupAccounts.getChildAt(j)
            if (c is Chip && c.id != allChipId && c.isChecked) selected.add(c.tag as String)
        }
        localFilters = localFilters.copy(accountNames = selected.toList())
        onApplyFilters(localFilters)
    }

    binding.chipGroupAccounts.removeAllViews()
    val allChip = Chip(this).apply {
        id = allChipId
        text = getString(R.string.all)
        isCheckable = true
        isChecked = localFilters.accountNames.isEmpty()
        setTextColor(ContextCompat.getColorStateList(this@showTransactionFiltersBottomSheet, R.color.chip_text_color))
        chipBackgroundColor = ContextCompat.getColorStateList(this@showTransactionFiltersBottomSheet, R.color.chip_background)
    }
    binding.chipGroupAccounts.addView(allChip)

    accounts.forEach { account ->
        val chip = Chip(this).apply {
            text = account.name
            isCheckable = true
            isChecked = localFilters.accountNames.contains(account.name)
            tag = account.name
            id = View.generateViewId()
            setTextColor(ContextCompat.getColorStateList(this@showTransactionFiltersBottomSheet, R.color.chip_text_color))
            chipBackgroundColor = ContextCompat.getColorStateList(this@showTransactionFiltersBottomSheet, R.color.chip_background)
        }
        binding.chipGroupAccounts.addView(chip)

        chip.setOnCheckedChangeListener { _, _ ->
            if (chip.isChecked) allChip.isChecked = false
            applyAccountsFilter()
        }
    }
    allChip.setOnCheckedChangeListener { _, isChecked ->
        if (isChecked) {
            for (i in 0 until binding.chipGroupAccounts.childCount) {
                val child = binding.chipGroupAccounts.getChildAt(i)
                if (child is Chip && child != allChip) child.isChecked = false
            }
            applyAccountsFilter()
        }
    }

    binding.btnApplyFilters.setOnClickListener {
        localFilters = localFilters.copy(searchQuery = binding.etSearch.text.toString())
        onApplyFilters(localFilters)
        dialog.dismiss()
    }

    dialog.show()
}