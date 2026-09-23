package com.dialcadev.dialcash.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect

@Composable
fun GlassIconButton(
    modifier: Modifier = Modifier,
    iconRes: Int,
    contentDescription: String?,
    size: Dp = 44.dp,
    hazeState: HazeState? = null,
    standalone: Boolean = true,
    onClick: () -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val iconColor = if (isDark) Color.White else MaterialTheme.colorScheme.onSurface
    val tintColor = if (isDark) Color(0xFF121212) else MaterialTheme.colorScheme.surface
    val blurAlpha = if (isDark) 0.65f else 0.35f

    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .let { base ->
                if (standalone && hazeState != null) {
                    base.hazeEffect(
                        state = hazeState,
                        style = HazeStyle(
                            tint = HazeTint(tintColor.copy(alpha = blurAlpha)),
                            blurRadius = 16.dp,
                            noiseFactor = 0.05f
                        )
                    )
                } else {
                    base
                }
            }
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        if (!standalone) {
            Box(
                modifier = Modifier
                    .size(size)
                    .clip(CircleShape)
                    .background(
                        if (isDark) Color.White.copy(alpha = 0.1f)
                        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f)
                    )
            )
        }
        Icon(
            painter = painterResource(id = iconRes),
            contentDescription = contentDescription,
            tint = iconColor,
            modifier = Modifier.size(size * 0.5f)
        )
    }
}