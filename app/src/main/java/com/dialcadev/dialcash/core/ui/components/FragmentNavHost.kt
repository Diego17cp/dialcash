package com.dialcadev.dialcash.core.ui.components

import android.view.View
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.fragment.app.FragmentContainerView
import androidx.fragment.app.FragmentManager
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment

@Composable
fun FragmentNavHost(
    modifier: Modifier = Modifier,
    fragmentManager: FragmentManager,
    navGraphId: Int,
    onNavControllerReady: (NavController) -> Unit
) {
    val containerId = rememberSaveable() { View.generateViewId() }

    AndroidView(
        modifier = modifier.fillMaxSize(),
        factory = { context -> FragmentContainerView(context).apply { id = containerId } }
    )

    DisposableEffect(Unit) {
        val existing = fragmentManager.findFragmentById(containerId) as? NavHostFragment
        val navHostFragment = existing ?: NavHostFragment.create(navGraphId).also {
            fragmentManager.beginTransaction()
                .replace(containerId, it, "nav_host_fragment")
                .setPrimaryNavigationFragment(it)
                .commitNow()
        }
        onNavControllerReady(navHostFragment.navController)
        onDispose { }
    }
}