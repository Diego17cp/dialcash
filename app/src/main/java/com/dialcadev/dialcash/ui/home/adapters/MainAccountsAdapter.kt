package com.dialcadev.dialcash.ui.home.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.dialcadev.dialcash.R
import com.dialcadev.dialcash.data.dto.AccountBalance
import com.dialcadev.dialcash.databinding.ItemMainAccountBinding

class MainAccountsAdapter(private val onAccountClick: (AccountBalance) -> Unit) : ListAdapter<AccountBalance, MainAccountsAdapter.AccountViewHolder>(AccountDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AccountViewHolder {
        val binding = ItemMainAccountBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return AccountViewHolder(binding)
    }

    override fun onBindViewHolder(holder: AccountViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class AccountViewHolder(private val binding: ItemMainAccountBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(account: AccountBalance) {
            binding.apply {
                textAccountName.text = account.name
                textAccountBalance.text = root.context.getString(R.string.currency_format, account.balance)
                val iconRes = when (account.type) {
                    "bank" -> R.drawable.ic_bank
                    "cash" -> R.drawable.ic_cash
                    "card" -> R.drawable.ic_card
                    "wallet" -> R.drawable.ic_accounts_outline
                    else -> R.drawable.ic_account_default
                }
                imageAccountIcon.setImageResource(iconRes)
                val colorRes = if (account.balance >= 0) R.color.colorPositive else R.color.negative_balance
                textAccountBalance.setTextColor(root.context.getColor(colorRes))
                root.setOnClickListener { onAccountClick(account) }
            }
        }
    }
    private class AccountDiffCallback: DiffUtil.ItemCallback<AccountBalance>() {
        override fun areItemsTheSame(oldItem: AccountBalance, newItem: AccountBalance): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: AccountBalance, newItem: AccountBalance): Boolean {
            return oldItem == newItem
        }
    }
}