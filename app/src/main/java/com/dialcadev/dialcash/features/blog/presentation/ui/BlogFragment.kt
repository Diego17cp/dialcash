package com.dialcadev.dialcash.features.blog.presentation.ui

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.dialcadev.dialcash.R
import com.dialcadev.dialcash.core.theme.AppTypography
import com.dialcadev.dialcash.core.ui.UiChromeViewModel
import com.dialcadev.dialcash.core.utils.extensions.fromISOToReadable
import com.dialcadev.dialcash.databinding.FragmentBlogBinding
import com.dialcadev.dialcash.features.blog.presentation.ui.components.BlogPostCard
import com.dialcadev.dialcash.features.blog.presentation.viewmodels.BlogViewModel
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

@AndroidEntryPoint
class BlogFragment : Fragment() {
    private var _binding: FragmentBlogBinding? = null
    private val binding get() = _binding!!
    private val viewModel: BlogViewModel by viewModels()
    private val chromeViewModel: UiChromeViewModel by activityViewModels()

    private val WEB_URL = "https://dialcash.vercel.app"
    private val BLOG_POST_URL = "$WEB_URL/blog/posts/"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBlogBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupPostsComposeList()
        observeViewModel()
    }

    @OptIn(ExperimentalMaterial3Api::class)
    private fun setupPostsComposeList() {
        binding.composePostsList.apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                MaterialTheme(typography = AppTypography) {
                    val state by viewModel.uiState.collectAsState()

                    val topPx by chromeViewModel.topBarHeightPx.collectAsState()
                    val bottomPx by chromeViewModel.bottomBarHeightPx.collectAsState()

                    val density = LocalDensity.current
                    val topDp = with(density) { topPx.toDp() }
                    val bottomDp = with(density) { bottomPx.toDp() }

                    PullToRefreshBox(
                        isRefreshing = state.isLoading && state.posts.isNotEmpty(),
                        onRefresh = { viewModel.loadBlogFeed() },
                        modifier = Modifier.fillMaxSize()
                    ) {
                        LazyColumn(
                            contentPadding = PaddingValues(
                                top = topDp + 16.dp,
                                bottom = bottomDp + 16.dp,
                                start = 16.dp,
                                end = 16.dp
                            ),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            item {
                                Column(modifier = Modifier.padding(bottom = 8.dp)) {
                                    Text(
                                        text = stringResource(id = R.string.blog_title),
                                        fontSize = 24.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = colorResource(id = R.color.text_primary)
                                    )
                                    Text(
                                        text = stringResource(id = R.string.blog_subtitle),
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Normal,
                                        color = colorResource(id = R.color.text_secondary),
                                        modifier = Modifier.padding(top = 4.dp)
                                    )
                                }
                            }
                            items(
                                items = state.posts, key = { it.slug }) { post ->
                                BlogPostCard(
                                    title = post.title,
                                    description = post.description,
                                    category = post.category,
                                    dateText = post.publishedAt.fromISOToReadable(),
                                    imageUrl = post.portrait,
                                    onClick = {
                                        val intent = Intent(
                                            Intent.ACTION_VIEW,
                                            ("$BLOG_POST_URL${post.slug}").toUri()
                                        )
                                        startActivity(intent)
                                    })
                            }
                        }
                    }
                }
            }
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    binding.progressBar.visibility =
                        if (state.isLoading && state.posts.isEmpty()) View.VISIBLE else View.GONE
                    if (state.isLoading && state.posts.isEmpty()) {
                        binding.composePostsList.visibility = View.GONE
                        binding.layoutNoPosts.visibility = View.GONE
                    } else if (state.posts.isEmpty() && state.errorMessage == null) {
                        binding.composePostsList.visibility = View.GONE
                        binding.layoutNoPosts.visibility = View.VISIBLE
                    } else if (state.errorMessage != null && state.posts.isEmpty()) {
                        binding.composePostsList.visibility = View.GONE
                        binding.layoutNoPosts.visibility = View.VISIBLE
                        binding.tvEmptyTitle.text = getString(R.string.connection_error)
                        binding.tvEmptySubtitle.text = getString(R.string.cannot_load_content)
                    } else {
                        binding.composePostsList.visibility = View.VISIBLE
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