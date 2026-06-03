package com.dialcadev.dialcash.core.ui.components

import android.content.Context
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Shader
import android.view.LayoutInflater
import android.view.View
import androidx.core.widget.addTextChangedListener
import com.dialcadev.dialcash.R
import com.dialcadev.dialcash.core.utils.extensions.toCurrencyFormat
import com.dialcadev.dialcash.core.utils.extensions.toReadableDate
import com.dialcadev.dialcash.databinding.RecycleIncomeGroupItemBinding
import com.dialcadev.dialcash.features.incomegroups.domain.dtos.IncomeGroupRemaining
import com.google.android.material.bottomsheet.BottomSheetDialog

fun Context.showIncomeGroupDetailsBottomSheet(
    incomeGroup: IncomeGroupRemaining,
    currencySymbol: String,
    onUpdate: (IncomeGroupRemaining) -> Unit,
    onDelete: (IncomeGroupRemaining) -> Unit
) {
    val dialog = BottomSheetDialog(this)
    val binding = RecycleIncomeGroupItemBinding.inflate(LayoutInflater.from(this))
    fun validateForm() {
        val name = binding.etEditAccountName.text.toString().trim()
        val amountText = binding.etInitialBalance.text.toString().trim()
        val isNameValid = name.isNotEmpty()
        val isAmountValid = amountText.isNotEmpty() && amountText.toDoubleOrNull() != null && amountText.toDouble() >= 0.0
        binding.btnSave.isEnabled = isNameValid && isAmountValid
        if (!isNameValid) binding.tilIncomeGroupName.error = getString(R.string.name_cannot_be_empty)
        else binding.tilIncomeGroupName.error = null
        if (!isAmountValid) binding.tilInitialBalance.error = getString(R.string.amount_required)
        else binding.tilInitialBalance.error = null
    }
    fun resetForm() {
        binding.tvIncomeGroupName.visibility = View.VISIBLE
        binding.tilIncomeGroupName.visibility = View.GONE
        binding.tvIncomeGroupBalance.visibility = View.VISIBLE
        binding.tilInitialBalance.visibility = View.GONE
        binding.actionsRow.visibility = View.VISIBLE
        binding.editFooter.visibility = View.GONE
        binding.deleteConfirmFooter.visibility = View.GONE
        binding.etEditAccountName.error = null
        binding.etInitialBalance.error = null
    }
    binding.apply {
        tvIncomeGroupRemaining.text = incomeGroup.name
        etEditAccountName.setText(incomeGroup.name)
        "$currencySymbol ${incomeGroup.amount.toCurrencyFormat()}".also {
            tvIncomeGroupBalance.text = it
        }
        etInitialBalance.setText(incomeGroup.amount.toString())
        "$currencySymbol ${incomeGroup.remaining.toCurrencyFormat()}".also {
            tvIncomeGroupRemaining.text = it
        }
        val remainingText = binding.tvIncomeGroupRemaining
        remainingText.post {
            val paint = remainingText.paint
            val width = paint.measureText(remainingText.text.toString())
            val textShader = LinearGradient(
                0f, 0f, width, 0f,
                intArrayOf(
                    Color.parseColor("#f7b777"),
                    Color.parseColor("#cbd5e4")
                ),
                null,
                Shader.TileMode.CLAMP
            )
            remainingText.paint.shader = textShader
            remainingText.invalidate()
        }
        tvCreatedAt.text = getString(
            com.dialcadev.dialcash.R.string.created_at,
            incomeGroup.createdAt.toReadableDate()
        )
    }
    binding.etEditAccountName.addTextChangedListener { validateForm() }
    binding.etInitialBalance.addTextChangedListener { validateForm() }

    binding.btnEdit.setOnClickListener {
        binding.tvIncomeGroupName.visibility = View.GONE
        binding.tilIncomeGroupName.visibility = View.VISIBLE
        binding.tvIncomeGroupBalance.visibility = View.GONE
        binding.tilInitialBalance.visibility = View.VISIBLE
        binding.actionsRow.visibility = View.GONE
        binding.editFooter.visibility = View.VISIBLE
        validateForm()
    }
    binding.btnSave.setOnClickListener {
        val updatedIncomeGroup = incomeGroup.copy(
            name = binding.etEditAccountName.text.toString().trim(),
            amount = binding.etInitialBalance.text.toString().trim().toDouble()
        )
        onUpdate(updatedIncomeGroup)
        resetForm()
        dialog.dismiss()
    }
    binding.btnCancel.setOnClickListener { resetForm() }
    binding.btnDelete.setOnClickListener {
        binding.actionsRow.visibility = View.GONE
        binding.editFooter.visibility = View.GONE
        binding.deleteConfirmFooter.visibility = View.VISIBLE
    }
    binding.btnConfirmDelete.setOnClickListener {
        onDelete(incomeGroup)
        dialog.dismiss()
    }
    binding.btnCancelDelete.setOnClickListener { resetForm() }
    dialog.setContentView(binding.root)
    dialog.show()
}