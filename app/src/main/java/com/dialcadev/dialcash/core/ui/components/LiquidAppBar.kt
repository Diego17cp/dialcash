package com.dialcadev.dialcash.core.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.dialcadev.dialcash.R
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect

data class AppBarAction(
    val iconRes: Int,
    val contentDescription: String,
    val onClick: () -> Unit = {},
    val subActions: List<AppBarAction>? = null
)

@Composable
fun LiquidAppBar(
    modifier: Modifier = Modifier,
    hazeState: HazeState,
    title: String,
    onBackClick: (() -> Unit)? = null,
    actions: List<AppBarAction> = emptyList()
) {
    val isDark = isSystemInDarkTheme()
    val tintColor = if (isDark) Color(0xFF121212) else MaterialTheme.colorScheme.surface
    val blurAlpha = if (isDark) 0.65f else 0.35f
    val textColor = if (isDark) Color.White else MaterialTheme.colorScheme.onSurface

    Box(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                shadowElevation = 8f
                spotShadowColor = Color.Black.copy(alpha = 0.06f)
            }
            .hazeEffect(
                state = hazeState,
                style = HazeStyle(
                    tint = HazeTint(tintColor.copy(alpha = blurAlpha)),
                    blurRadius = 20.dp,
                    noiseFactor = 0.02f
                )
            )
            .border(
                width = 0.5.dp,
                color = if (isDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.05f)
            )
            .statusBarsPadding()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) {
            if (onBackClick != null) {
                GlassIconButton(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .padding(start = 8.dp),
                    iconRes = R.drawable.ic_arrow_left,
                    contentDescription = stringResource(R.string.back),
                    standalone = false,
                    onClick = onBackClick
                )
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = textColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(horizontal = 64.dp)
            )
            Row(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                actions.forEach { action ->
                    if (action.subActions != null) {
                        var expanded by remember { mutableStateOf(false) }
                        Box {
                            GlassIconButton(
                                iconRes = action.iconRes,
                                contentDescription = action.contentDescription,
                                standalone = false,
                                onClick = { expanded = true }
                            )
                            GlassDropdownMenu(
                                expanded = expanded,
                                onDismissRequest = { expanded = false },
                                subActions = action.subActions,
                                hazeState = hazeState
                            )
                        }
                    } else {
                        GlassIconButton(
                            iconRes = action.iconRes,
                            contentDescription = action.contentDescription,
                            standalone = false,
                            onClick = action.onClick
                        )
                    }
                }
            }
        }
    }
}