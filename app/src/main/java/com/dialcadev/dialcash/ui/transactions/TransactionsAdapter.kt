package com.dialcadev.dialcash.ui.transactions

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.dialcadev.dialcash.R
import com.dialcadev.dialcash.data.dto.TransactionWithDetails
import com.dialcadev.dialcash.databinding.ItemRecentTransactionBinding

class TransactionsAdapter(
    private val onTransactionClick: (TransactionWithDetails) -> Unit,
    currencySymbol: String
) :
    ListAdapter<TransactionWithDetails, TransactionsAdapter.TransactionViewHolder>(
        TransactionDiffCallback()
    ) {
    private var currentCurrencySymbol = currencySymbol
    private val dateFormat = java.text.SimpleDateFormat("dd MMM", java.util.Locale.getDefault())

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TransactionViewHolder {
        val binding =
            ItemRecentTransactionBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return TransactionViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TransactionViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
    fun updateCurrencySymbol(newSymbol: String) {
        currentCurrencySymbol = newSymbol
        notifyDataSetChanged()
    }

    inner class TransactionViewHolder(private val binding: ItemRecentTransactionBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(transaction: TransactionWithDetails) {
            binding.apply {
                textTransactionDescription.text = transaction.description ?: "No Description"
                val date = dateFormat.format(transaction.date)
                val account = "${transaction.accountName} • "
                textTransactionMeta.text = account + date
                val amount = if (transaction.type == "income") "+$currentCurrencySymbol ${transaction.amount}"
                else "-$currentCurrencySymbol ${transaction.amount}"
                textTransactionAmount.text = amount
                val colorRes =
                    when (transaction.type) {
                        "income" -> R.color.positive_amount
                        "transfer" -> R.color.colorPrimary
                        else -> R.color.negative_amount
                    }
                textTransactionAmount.setTextColor(root.context.getColor(colorRes))
                val iconRes =
                    when (transaction.type) {
                        "income" -> R.drawable.ic_income
                        "expense" -> R.drawable.ic_expense
                        else -> R.drawable.ic_transactions_outline
                    }
                imageTransactionIcon.setImageResource(iconRes)
                val iconColor =
                    when (transaction.type) {
                        "income" -> R.color.positive_amount
                        "transfer" -> R.color.colorPrimary
                        else -> R.color.negative_amount
                    }
                imageTransactionIcon.setColorFilter(root.context.getColor(iconColor))

                val bgIcon =
                    when (transaction.type) {
                        "income" -> R.drawable.bg_income_btn
                        "expense" -> R.drawable.bg_expense_btn
                        else -> R.drawable.bg_transfer_btn
                    }
                bgIconTransaction.setBackgroundResource(bgIcon)

                root.setOnClickListener {
                    onTransactionClick(transaction)
                }
            }
        }
    }

    private class TransactionDiffCallback : DiffUtil.ItemCallback<TransactionWithDetails>() {
        override fun areItemsTheSame(
            oldItem: TransactionWithDetails,
            newItem: TransactionWithDetails
        ): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(
            oldItem: TransactionWithDetails,
            newItem: TransactionWithDetails
        ): Boolean {
            return oldItem == newItem
        }
    }
}