package com.dialcadev.dialcash.ui.accounts

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.OvershootInterpolator
import androidx.recyclerview.widget.RecyclerView
import com.dialcadev.dialcash.R
import com.dialcadev.dialcash.data.dto.AccountTypeUI
import com.dialcadev.dialcash.databinding.ItemAccountTypeBinding

class AccountTypeAdapter(
    private val items: List<AccountTypeUI>,
    private val onSelected: (AccountTypeUI) -> Unit
) : RecyclerView.Adapter<AccountTypeAdapter.ViewHolder>() {

    private var selectedId: String? = null
    private var previousSelectedPosition: Int = RecyclerView.NO_POSITION


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemAccountTypeBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position])
    }

    inner class ViewHolder(
        private val binding: ItemAccountTypeBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: AccountTypeUI) = with(binding) {
            val isMainAccount = item.id == "bank" || item.id == "cash" || item.id == "wallet" || item.id == "card"
            title.text = itemView.context.getString(item.titleRes)
            subtitle.visibility = if (isMainAccount) View.VISIBLE else View.GONE
            icon.setImageResource(item.iconRes)

            val isSelected = item.id == selectedId
            updateSelectionState(isSelected, item)
            root.setOnClickListener {
                if (selectedId == item.id) return@setOnClickListener
                val currentPosition = adapterPosition
                selectedId = item.id
                if (previousSelectedPosition != RecyclerView.NO_POSITION) {
                    notifyItemChanged(previousSelectedPosition)
                }
                notifyItemChanged(currentPosition)
                previousSelectedPosition = currentPosition
                onSelected(item)
            }
        }
        private fun updateSelectionState(isSelected: Boolean, item: AccountTypeUI) = with(binding) {
            root.isSelected = isSelected

            if (isSelected) {
                title.setTextColor(itemView.context.getColor(R.color.colorPrimary))
                check.visibility = View.VISIBLE
                if (check.scaleX < 1f || check.visibility != View.VISIBLE) {
                    root.animate()
                        .scaleX(0.98f)
                        .scaleY(0.98f)
                        .setDuration(80)
                        .withEndAction {
                            root.animate()
                                .scaleX(1f)
                                .scaleY(1f)
                                .setDuration(80)
                                .start()
                        }
                        .start()
                    check.scaleX = 0f
                    check.scaleY = 0f
                    check.animate()
                        .scaleX(1f)
                        .scaleY(1f)
                        .setDuration(140)
                        .setInterpolator(OvershootInterpolator())
                        .start()
                }
            } else {
                title.setTextColor(itemView.context.getColor(android.R.color.tab_indicator_text))
                check.visibility = View.GONE
                check.scaleX = 0f
                check.scaleY = 0f
                root.scaleX = 1f
                root.scaleY = 1f
            }
        }
    }
}
