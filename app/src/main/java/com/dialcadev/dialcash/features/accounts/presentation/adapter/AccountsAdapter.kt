package com.dialcadev.dialcash.features.accounts.presentation.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import com.dialcadev.dialcash.R
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.dialcadev.dialcash.core.utils.extensions.toCurrencyFormat
import com.dialcadev.dialcash.databinding.ItemAccountBinding
import com.dialcadev.dialcash.features.accounts.domain.dtos.AccountBalanceWithOriginal

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
            val isMainAccount = account.type == "bank" || account.type == "cash" || account.type == "wallet" || account.type == "card"
            binding.apply {
                textAccountName.text = account.name
                "$currentCurrencySymbol ${account.balance.toCurrencyFormat()}".also { textCurrentBalance.text = it }
                val formattedBalance = "$currentCurrencySymbol ${account.originalBalance.toCurrencyFormat()}"
                textOriginalBalance.text = root.context.getString(
                    R.string.original_balance_with_value,
                    formattedBalance
                )
                val iconRes = when(account.type){
                    "cash" -> R.drawable.ic_cash
                    "bank" -> R.drawable.ic_building_bank
                    "card" -> R.drawable.ic_card
                    "wallet" -> R.drawable.ic_accounts_filled
                    "debt" -> R.drawable.ic_debt_payment
                    "savings" -> R.drawable.ic_bank
                    else -> R.drawable.ic_account_default
                }
                imageAccountIcon.setImageResource(iconRes)
                if (isMainAccount) mainAccountBadge.visibility = View.VISIBLE
                root.setOnClickListener { onAccountClick(account) }
            }
        }
    }
    private class AccountDiffCallback :
        DiffUtil.ItemCallback<AccountBalanceWithOriginal>() {
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