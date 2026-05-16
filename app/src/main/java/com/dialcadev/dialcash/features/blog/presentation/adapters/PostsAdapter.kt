package com.dialcadev.dialcash.features.blog.presentation.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.dialcadev.dialcash.R
import com.dialcadev.dialcash.core.utils.extensions.fromISOToReadable
import com.dialcadev.dialcash.features.blog.domain.models.BlogPost
import com.dialcadev.dialcash.utils.capitalize
import com.google.android.material.chip.Chip

class PostsAdapter(
    private val onPostClick: (BlogPost) -> Unit
) : ListAdapter<BlogPost, PostsAdapter.PostViewHolder>(PostDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_post, parent, false)
        return PostViewHolder(view, onPostClick)
    }

    override fun onBindViewHolder(holder: PostViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class PostViewHolder(
        itemView: View,
        private val onPostClick: (BlogPost) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {

        private val ivPostImage: ImageView = itemView.findViewById(R.id.ivPostImage)
        private val tvPostTitle: TextView = itemView.findViewById(R.id.tvPostTitle)
        private val tvPostDescription: TextView = itemView.findViewById(R.id.tvPostDescription)
        private val chipDate: Chip = itemView.findViewById(R.id.chipDate)
        private val chipCategory: Chip = itemView.findViewById(R.id.chipCategory)

        fun bind(post: BlogPost) {
            val categoryBg = when (post.category.lowercase()) {
                "release" -> R.color.category_release
                "fix" -> R.color.category_fix
                // More categories if I think of any xd
                else -> R.color.category_other
            }
            tvPostTitle.text = post.title
            tvPostDescription.text = post.description
            chipCategory.text = post.category.capitalize()
            chipCategory.setChipBackgroundColorResource(categoryBg)
            chipDate.text = post.publishedAt.fromISOToReadable()
            Glide.with(itemView.context)
                .load(post.portrait)
                .placeholder(R.drawable.ic_error_account)
                .error(R.drawable.ic_error_account)
                .transition(DrawableTransitionOptions.withCrossFade())
                .centerCrop()
                .into(ivPostImage)
            itemView.setOnClickListener {
                onPostClick(post)
            }
        }
    }
    private class PostDiffCallback : DiffUtil.ItemCallback<BlogPost>() {
        override fun areItemsTheSame(oldItem: BlogPost, newItem: BlogPost): Boolean {
            return oldItem.slug == newItem.slug
        }
        override fun areContentsTheSame(oldItem: BlogPost, newItem: BlogPost): Boolean {
            return oldItem == newItem
        }
    }
}