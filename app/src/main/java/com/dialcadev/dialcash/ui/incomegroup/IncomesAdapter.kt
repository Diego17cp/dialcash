package com.dialcadev.dialcash.ui.incomegroup

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.dialcadev.dialcash.R
import com.dialcadev.dialcash.data.dto.IncomeGroupRemaining
import com.dialcadev.dialcash.data.entities.IncomeGroup
import com.dialcadev.dialcash.databinding.ItemIncomeGroupBinding

class IncomesAdapter(private val onIncomeClick: (IncomeGroupRemaining) -> Unit) :
    ListAdapter<IncomeGroupRemaining, IncomesAdapter.IncomesViewHolder>(IncomeDiffCallback()) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): IncomesViewHolder {
        val binding =
            ItemIncomeGroupBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return IncomesViewHolder(binding)
    }

    override fun onBindViewHolder(holder: IncomesAdapter.IncomesViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class IncomesViewHolder(private val binding: ItemIncomeGroupBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(income: IncomeGroupRemaining) {
            binding.apply {
                val currencyFormat = root.context.getString(R.string.currency_format)
                textIncomeName.text = income.name
                textIncomeBalance.text = "Original balance: ${String.format(currencyFormat, income.amount)}"
                textIncomeRemaining.text = String.format(currencyFormat, income.remaining)
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