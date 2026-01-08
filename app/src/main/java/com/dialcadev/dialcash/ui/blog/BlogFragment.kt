package com.dialcadev.dialcash.ui.blog

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.dialcadev.dialcash.databinding.FragmentBlogBinding
import dagger.hilt.android.AndroidEntryPoint
import androidx.core.net.toUri
import androidx.recyclerview.widget.LinearLayoutManager
import com.dialcadev.dialcash.R
import com.google.android.material.snackbar.Snackbar

@AndroidEntryPoint
class BlogFragment : Fragment() {
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
    ): View {
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
            viewModel.refreshBlogFeed()
        }
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            if (!isLoading) {
                binding.swipeRefreshLayout.isRefreshing = false
            }
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

    private fun updateEmptyState(isEmpty: Boolean) {
        binding.layoutBlog.visibility = if (isEmpty) View.GONE else View.VISIBLE
        binding.layoutNoPosts.visibility = if (isEmpty) View.VISIBLE else View.GONE
    }

    private fun observeViewModel() {
        viewModel.posts.observe(viewLifecycleOwner) { posts ->
            binding.swipeRefreshLayout.isRefreshing = false
            postsAdapter.submitList(posts)
            updateEmptyState(posts.isEmpty())
        }
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
            if (isLoading && postsAdapter.itemCount == 0) {
                binding.layoutBlog.visibility = View.GONE
                binding.layoutNoPosts.visibility = View.GONE
            } else binding.layoutBlog.visibility = View.VISIBLE
        }
        viewModel.error.observe(viewLifecycleOwner) { errorMessage ->
            errorMessage?.let {
                if (postsAdapter.itemCount == 0) {
                    binding.layoutBlog.visibility = View.GONE
                    binding.layoutNoPosts.visibility = View.VISIBLE
                    binding.tvEmptyTitle.text = getString(R.string.connection_error)
                    binding.tvEmptySubtitle.text = getString(R.string.cannot_load_content)
                } else {
                    Snackbar.make(
                        binding.root,
                        "${getString(R.string.error_updating)}: $it",
                        Snackbar.LENGTH_LONG
                    ).setAction(getString(R.string.retry)) {
                        viewModel.refreshBlogFeed()
                    }.show()
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshBlogFeed()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}