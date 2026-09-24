package com.dialcadev.dialcash.core.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.dialcadev.dialcash.R
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
    iconSize: Dp = size * 0.48f,
    iconTint: Color? = null,
    hazeState: HazeState? = null,
    standalone: Boolean = true,
    onClick: () -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val tintColor = if (isDark) Color(0xFF121212) else MaterialTheme.colorScheme.surface
    val blurAlpha = if (isDark) 0.65f else 0.35f
    val fallbackBgColor = tintColor.copy(alpha = if (isDark) 0.70f else 0.55f)

    val defaultIconColor = if (isDark) Color.White else MaterialTheme.colorScheme.onSurface
    val finalIconTint = iconTint ?: defaultIconColor

    val borderColor =
        if (isDark) Color.White.copy(alpha = 0.10f) else Color.Black.copy(alpha = 0.08f)

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.92f else 1.0f,
        label = "glassIconButtonScale"
    )

    Box(
        modifier = modifier
            .size(size)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                if (standalone) {
                    shadowElevation = 14f
                    spotShadowColor = Color.Black.copy(alpha = if (isDark) 0.30f else 0.10f)
                    ambientShadowColor = Color.Black.copy(alpha = if (isDark) 0.15f else 0.04f)
                    shape = CircleShape
                    clip = true
                }
            }
            .clip(CircleShape)
            .then(
                if (standalone) {
                    if (hazeState != null) {
                        Modifier.hazeEffect(
                            state = hazeState,
                            style = HazeStyle(
                                tint = HazeTint(tintColor.copy(alpha = blurAlpha)),
                                blurRadius = 20.dp,
                                noiseFactor = 0.02f
                            )
                        )
                    } else {
                        Modifier.background(fallbackBgColor, CircleShape)
                    }
                } else {
                    Modifier.background(
                        if (isDark) Color.White.copy(alpha = 0.10f)
                        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f),
                        CircleShape
                    )
                }
            )
            .then(
                if (standalone) {
                    Modifier.border(width = 0.5.dp, color = borderColor, shape = CircleShape)
                } else Modifier
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(id = iconRes),
            contentDescription = contentDescription,
            tint = finalIconTint,
            modifier = Modifier.size(iconSize)
        )
    }
}