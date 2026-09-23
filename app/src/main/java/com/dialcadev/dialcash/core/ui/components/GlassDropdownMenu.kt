package com.dialcadev.dialcash.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.PopupProperties
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect
import com.dialcadev.dialcash.R

@Composable
fun GlassDropdownMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    subActions: List<AppBarAction>,
    modifier: Modifier = Modifier,
    hazeState: HazeState? = null
) {
    val isDark = isSystemInDarkTheme()
    val shape = RoundedCornerShape(22.dp)
    val surfaceColor = colorResource(id = R.color.surface)

    val tintColor = if (isDark) Color(0xFF121212) else MaterialTheme.colorScheme.surface
    val blurAlpha = if (isDark) 0.65f else 0.40f
    val containerBgColor = surfaceColor.copy(alpha = if (isDark) 0.75f else 0.65f)

    val borderColor =
        if (isDark) Color.White.copy(alpha = 0.12f) else Color.Black.copy(alpha = 0.08f)
    val textColor = if (isDark) Color.White else MaterialTheme.colorScheme.onSurface
    val iconColor =
        if (isDark) Color.White.copy(alpha = 0.85f) else MaterialTheme.colorScheme.onSurfaceVariant
    val dividerColor =
        if (isDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.06f)

    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest,
        shape = shape,
        containerColor = Color.Transparent,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
        properties = PopupProperties(
            focusable = true,
            clippingEnabled = false
        ),
        modifier = modifier.widthIn(min = 200.dp, max = 280.dp)
    ) {
        Box(
            modifier = Modifier
                .graphicsLayer {
                    shadowElevation = 16f
                    spotShadowColor = Color.Black.copy(alpha = if (isDark) 0.35f else 0.10f)
                    ambientShadowColor = Color.Black.copy(alpha = if (isDark) 0.20f else 0.05f)
                    clip = true
                    this.shape = shape
                }
                .clip(shape)
                .then(
                    if (hazeState != null) {
                        Modifier.hazeEffect(
                            state = hazeState,
                            style = HazeStyle(
                                tint = HazeTint(tintColor.copy(alpha = blurAlpha)),
                                blurRadius = 24.dp,
                                noiseFactor = 0.02f
                            )
                        )
                    } else {
                        Modifier
                    }
                )
                .background(
                    color = containerBgColor,
                    shape = shape
                )
                .border(
                    width = 0.5.dp,
                    color = borderColor,
                    shape = shape
                )
                .padding(vertical = 6.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                subActions.forEachIndexed { index, sub ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = sub.contentDescription,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.W500,
                                    color = textColor
                                )
                            )
                        },
                        leadingIcon = {
                            Icon(
                                painter = painterResource(id = sub.iconRes),
                                contentDescription = null,
                                tint = iconColor,
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        onClick = {
                            onDismissRequest()
                            sub.onClick()
                        },
                        colors = MenuDefaults.itemColors(
                            textColor = textColor,
                            leadingIconColor = iconColor
                        ),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                        modifier = Modifier
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                            .clip(RoundedCornerShape(14.dp))
                    )

                    if (index < subActions.lastIndex) {
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 2.dp),
                            thickness = 0.5.dp,
                            color = dividerColor
                        )
                    }
                }
            }
        }
    }
}