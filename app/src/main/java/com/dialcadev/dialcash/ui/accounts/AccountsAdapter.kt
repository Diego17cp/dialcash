package com.dialcadev.dialcash.ui.accounts

import android.view.LayoutInflater
import android.view.ViewGroup
import com.dialcadev.dialcash.R
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.dialcadev.dialcash.data.dao.AccountBalanceWithOriginal
import com.dialcadev.dialcash.databinding.ItemAccountBinding
class AccountsAdapter(
    private val onAccountClick: (AccountBalanceWithOriginal) -> Unit,
    currencySymbol: String
) :
    ListAdapter<AccountBalanceWithOriginal, AccountsAdapter.AccountsViewHolder>(
        AccountDiffCallback()
    ) {
    private var currentCurrencySymbol = currencySymbol
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AccountsViewHolder {
        val binding = ItemAccountBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return AccountsViewHolder(binding)
    }

    override fun onBindViewHolder(holder: AccountsAdapter.AccountsViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    fun updateCurrencySymbol(newSymbol: String) {
        currentCurrencySymbol = newSymbol
        notifyDataSetChanged()
    }

    inner class AccountsViewHolder(private val binding: ItemAccountBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(account: AccountBalanceWithOriginal) {
            binding.apply {
                textAccountName.text = account.name
                "$currentCurrencySymbol ${"%.2f".format(account.balance)}".also { textCurrentBalance.text = it }
                val formattedBalance = "$currentCurrencySymbol ${"%.2f".format(account.originalBalance)}"
                textOriginalBalance.text = root.context.getString(
                    R.string.original_balance_with_value,
                    formattedBalance
                )
                val iconRes = when(account.type){
                    "cash" -> R.drawable.ic_cash
                    "bank" -> R.drawable.ic_bank
                    "card" -> R.drawable.ic_card
                    "wallet" -> R.drawable.ic_accounts_filled
                    else -> R.drawable.ic_account_default
                }
                imageAccountIcon.setImageResource(iconRes)
                root.setOnClickListener { onAccountClick(account) }
            }
        }
    }
    private class AccountDiffCallback :
        androidx.recyclerview.widget.DiffUtil.ItemCallback<AccountBalanceWithOriginal>() {
        override fun areItemsTheSame(
            oldItem: AccountBalanceWithOriginal,
            newItem: AccountBalanceWithOriginal
        ): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(
            oldItem: AccountBalanceWithOriginal,
            newItem: AccountBalanceWithOriginal
        ): Boolean {
            return oldItem == newItem
        }
    }
}