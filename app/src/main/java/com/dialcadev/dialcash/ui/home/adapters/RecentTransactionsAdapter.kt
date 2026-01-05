package com.dialcadev.dialcash.ui.home.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.dialcadev.dialcash.R
import com.dialcadev.dialcash.data.dto.TransactionWithDetails
import com.dialcadev.dialcash.databinding.ItemRecentTransactionBinding
import java.text.SimpleDateFormat
import java.util.Locale

class RecentTransactionsAdapter(
    private val onTransactionClick: (TransactionWithDetails) -> Unit,
    currencySymbol: String
) :
    ListAdapter<TransactionWithDetails, RecentTransactionsAdapter.TransactionViewHolder>(
        TransactionDiffCallback()
    ) {
    private var currentCurrencySymbol = currencySymbol
    private val dateFormat = SimpleDateFormat("dd MMM", Locale.getDefault())

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): RecentTransactionsAdapter.TransactionViewHolder {
        val binding = ItemRecentTransactionBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return TransactionViewHolder(binding)
    }

    override fun onBindViewHolder(
        holder: RecentTransactionsAdapter.TransactionViewHolder,
        position: Int
    ) {
        holder.bind(getItem(position))
    }
    fun updateCurrencySymbol(newSymbol: String) {
        currentCurrencySymbol = newSymbol
        notifyDataSetChanged()
    }

    inner class TransactionViewHolder(
        private val binding: ItemRecentTransactionBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(transaction: TransactionWithDetails) {
            binding.apply {
                textTransactionDate.text = dateFormat.format(transaction.date)
                textTransactionDescription.text = transaction.description ?: "No Description"
                textTransactionAccount.text = "${transaction.accountName} -"
                val amount = if (transaction.type == "income") "+ $currentCurrencySymbol${transaction.amount}"
                else "- $currentCurrencySymbol${transaction.amount}"
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