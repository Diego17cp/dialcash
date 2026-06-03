package com.dialcadev.dialcash.features.blog.presentation.ui

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.dialcadev.dialcash.R
import com.dialcadev.dialcash.databinding.FragmentBlogBinding
import com.dialcadev.dialcash.features.blog.presentation.adapters.PostsAdapter
import com.dialcadev.dialcash.features.blog.presentation.viewmodels.BlogViewModel
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class BlogFragment: Fragment() {
    private var _binding: FragmentBlogBinding? = null
    private val binding get() = _binding!!
    private val viewModel: BlogViewModel by viewModels()
    private lateinit var postsAdapter: PostsAdapter

    private val WEB_URL = "https://dialcash.vercel.app"
    private val BLOG_POST_URL = "$WEB_URL/blog/posts/"

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentBlogBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        observeViewModel()
        setupSwipeRefresh()
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefreshLayout.setOnRefreshListener {
            viewModel.loadBlogFeed()
        }
    }
    private fun setupRecyclerView() {
        postsAdapter = PostsAdapter { post ->
            val intent = Intent(Intent.ACTION_VIEW, (BLOG_POST_URL + post.slug).toUri())
            startActivity(intent)
        }
        binding.rvPosts.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = postsAdapter
        }
    }
    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    if (!state.isLoading) binding.swipeRefreshLayout.isRefreshing = false
                    binding.progressBar.visibility = if (state.isLoading && state.posts.isEmpty()) View.VISIBLE else View.GONE
                    postsAdapter.submitList(state.posts)
                    if (state.isLoading && state.posts.isEmpty()) {
                        binding.layoutBlog.visibility = View.GONE
                        binding.layoutNoPosts.visibility = View.GONE
                    } else if (state.posts.isEmpty() && state.errorMessage == null) {
                        binding.layoutBlog.visibility = View.GONE
                        binding.layoutNoPosts.visibility = View.VISIBLE
                    } else if (state.errorMessage != null && state.posts.isEmpty()) {
                        binding.layoutBlog.visibility = View.GONE
                        binding.layoutNoPosts.visibility = View.VISIBLE
                        binding.tvEmptyTitle.text = getString(R.string.connection_error)
                        binding.tvEmptySubtitle.text = getString(R.string.cannot_load_content)
                    } else {
                        binding.layoutBlog.visibility = View.VISIBLE
                        binding.layoutNoPosts.visibility = View.GONE
                    }
                    if (state.errorMessage != null && state.posts.isNotEmpty() && !state.isLoading) {
                        Snackbar.make(
                            binding.root,
                            "${getString(R.string.error_updating)}: ${state.errorMessage}",
                            Snackbar.LENGTH_LONG
                        ).setAction(getString(R.string.retry)) {
                            viewModel.loadBlogFeed()
                        }.show()
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}