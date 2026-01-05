package com.dialcadev.dialcash.ui.incomegroup

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.dialcadev.dialcash.R
import com.dialcadev.dialcash.data.dto.IncomeGroupRemaining
import com.dialcadev.dialcash.databinding.ItemIncomeGroupBinding

class IncomesAdapter(
    private val onIncomeClick: (IncomeGroupRemaining) -> Unit,
    currencySymbol: String
) :
    ListAdapter<IncomeGroupRemaining, IncomesAdapter.IncomesViewHolder>(IncomeDiffCallback()) {
    private var currentCurrencySymbol = currencySymbol
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): IncomesViewHolder {
        val binding =
            ItemIncomeGroupBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return IncomesViewHolder(binding)
    }

    override fun onBindViewHolder(holder: IncomesAdapter.IncomesViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    fun updateCurrencySymbol(newSymbol: String) {
        currentCurrencySymbol = newSymbol
        notifyDataSetChanged()
    }

    inner class IncomesViewHolder(private val binding: ItemIncomeGroupBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(income: IncomeGroupRemaining) {
            binding.apply {
                textIncomeName.text = income.name
                "$currentCurrencySymbol ${"%.2f".format(income.remaining)}"
                    .also { textIncomeRemaining.text = it }
                val formattedBalance = "$currentCurrencySymbol ${"%.2f".format(income.amount)}"
                textIncomeBalance.text = root.context.getString(
                    R.string.original_balance_with_value,
                    formattedBalance
                )
                root.setOnClickListener { onIncomeClick(income) }
            }
        }
    }

    private class IncomeDiffCallback : DiffUtil.ItemCallback<IncomeGroupRemaining>() {
        override fun areContentsTheSame(oldItem: IncomeGroupRemaining, newItem: IncomeGroupRemaining): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areItemsTheSame(oldItem: IncomeGroupRemaining, newItem: IncomeGroupRemaining): Boolean {
            return oldItem == newItem
        }
    }
}