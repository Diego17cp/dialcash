package com.dialcadev.dialcash.core.ui.components

import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.fragment.app.FragmentContainerView

@Composable
fun FragmentNavHost(
    modifier: Modifier = Modifier,
    container: FragmentContainerView
) {
    AndroidView(
        modifier = modifier.fillMaxSize(),
        factory = { context ->
            FrameLayout(context).apply {
                val wrapper = this
                wrapper.post {
                    (container.parent as? ViewGroup)?.removeView(container)
                    wrapper.addView(
                        container,
                        ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                    )
                }
            }
        }
    )
}