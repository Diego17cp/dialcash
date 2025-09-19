// kotlin
package com.dialcadev.dialcash.onboarding

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.dialcadev.dialcash.R
import com.dialcadev.dialcash.data.dto.OnboardingItem

class OnboardingAdapter(private val items: List<OnboardingItem>) : RecyclerView.Adapter<OnboardingAdapter.PageVH>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PageVH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_onboarding, parent, false)
        return PageVH(v)
    }

    override fun onBindViewHolder(holder: PageVH, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    class PageVH(view: View) : RecyclerView.ViewHolder(view) {
        private val iv = view.findViewById<ImageView>(R.id.imageOnboarding)
        private val tvTitle = view.findViewById<TextView>(R.id.titleOnboarding)
        private val tvDesc = view.findViewById<TextView>(R.id.descriptionOnboarding)

        fun bind(page: OnboardingItem) {
            iv.setImageResource(page.imageRes)
            tvTitle.text = page.title
            tvDesc.text = page.description
        }
    }
}
