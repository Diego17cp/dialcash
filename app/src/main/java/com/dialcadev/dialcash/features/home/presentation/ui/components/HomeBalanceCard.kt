package com.dialcadev.dialcash.features.home.presentation.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dialcadev.dialcash.R
import com.dialcadev.dialcash.core.ui.components.GlassIconButton
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect

@Composable
fun HomeBalanceCard(
    modifier: Modifier = Modifier,
    totalBalanceText: String,
    isBalanceVisible: Boolean,
    hazeState: HazeState? = null,
    actionsEnabled: Boolean = true,
    onToggleVisibility: () -> Unit,
    onQuickIncomeClick: () -> Unit,
    onQuickExpenseClick: () -> Unit,
    onQuickTransferClick: () -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val shape = RoundedCornerShape(28.dp)

    val primaryColor = colorResource(id = R.color.colorPrimary)
    val tertiaryColor = colorResource(id = R.color.colorPrimaryLight)
    val surfaceColor = colorResource(id = R.color.surface)
    val textPrimary = colorResource(id = R.color.text_primary)
    val textSecondary = colorResource(id = R.color.text_secondary)

    val tintColor = if (isDark) Color(0xFF121212) else surfaceColor
    val blurAlpha = if (isDark) 0.65f else 0.40f

    val borderColor = if (isDark) {
        primaryColor.copy(alpha = 0.25f)
    } else {
        primaryColor.copy(alpha = 0.18f)
    }

    val cardGradient = if (isDark) {
        Brush.verticalGradient(
            colors = listOf(
                tertiaryColor.copy(alpha = 0.06f),
                tertiaryColor.copy(alpha = 0.10f),
                tertiaryColor.copy(alpha = 0.18f),
                primaryColor.copy(alpha = 0.28f)
            )
        )
    } else {
        Brush.verticalGradient(
            colors = listOf(
                surfaceColor,
                tertiaryColor.copy(alpha = 0.04f),
                tertiaryColor.copy(alpha = 0.10f),
                primaryColor.copy(alpha = 0.22f)
            )
        )
    }

    Box(modifier = modifier
        .fillMaxWidth()
        .graphicsLayer {
            shadowElevation = 16f
            spotShadowColor = Color.Black.copy(
                alpha = if (isDark) 0.35f else 0.08f
            )
            ambientShadowColor = Color.Black.copy(
                alpha = if (isDark) 0.20f else 0.04f
            )
        }
        .clip(shape)
        .then(
            if (hazeState != null) {
                Modifier.hazeEffect(
                    state = hazeState, style = HazeStyle(
                        tint = HazeTint(
                            tintColor.copy(alpha = blurAlpha)
                        ), blurRadius = 24.dp, noiseFactor = 0.02f
                    )
                )
            } else {
                Modifier
            }
        )
        .background(
            brush = cardGradient, shape = shape
        )
        .border(
            width = 0.5.dp, color = borderColor, shape = shape
        )

        .padding(
            start = 20.dp, end = 20.dp, top = 18.dp, bottom = 18.dp
        )) {

        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {

                Text(
                    text = stringResource(id = R.string.total_balance),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    color = textSecondary,
                    fontSize = 15.sp
                )
                Spacer(
                    modifier = Modifier.size(8.dp)
                )
                GlassIconButton(
                    size = 32.dp, iconRes = if (isBalanceVisible) {
                        R.drawable.ic_eye_closed
                    } else {
                        R.drawable.ic_eye
                    }, contentDescription = stringResource(
                        id = R.string.total_balance
                    ), hazeState = hazeState, standalone = true, onClick = onToggleVisibility
                )
            }
            Spacer(
                modifier = Modifier.height(6.dp)
            )
            Text(
                text = totalBalanceText,
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                fontSize = 36.sp,
                color = textPrimary,
                letterSpacing = (-1).sp
            )
            Spacer(
                modifier = Modifier.height(28.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                QuickActionButton(
                    modifier = Modifier.weight(1f),
                    iconRes = R.drawable.ic_income,
                    label = stringResource(id = R.string.income),
                    enabled = actionsEnabled,
                    hazeState = hazeState,
                    onClick = onQuickIncomeClick
                )

                QuickActionButton(
                    modifier = Modifier.weight(1f),
                    iconRes = R.drawable.ic_expense,
                    label = stringResource(id = R.string.expense),
                    enabled = actionsEnabled,
                    hazeState = hazeState,
                    onClick = onQuickExpenseClick
                )

                QuickActionButton(
                    modifier = Modifier.weight(1f),
                    iconRes = R.drawable.ic_transactions_outline,
                    label = stringResource(id = R.string.transfer),
                    enabled = actionsEnabled,
                    hazeState = hazeState,
                    onClick = onQuickTransferClick
                )
            }
        }
    }
}

@Composable
private fun QuickActionButton(
    modifier: Modifier = Modifier,
    iconRes: Int,
    label: String,
    enabled: Boolean,
    hazeState: HazeState? = null,
    onClick: () -> Unit
) {
    val isDark = isSystemInDarkTheme()

    val primaryColor = colorResource(
        id = R.color.colorPrimary
    )

    val textPrimary = colorResource(
        id = R.color.text_primary
    )

    val surfaceColor = colorResource(
        id = R.color.surface
    )

    val shape = RoundedCornerShape(20.dp)

    Column(
        modifier = modifier.graphicsLayer {
            alpha = if (enabled) {
                1f
            } else {
                0.45f
            }
        }, horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .clip(shape)
                .then(
                    if (hazeState != null) {
                        Modifier.hazeEffect(
                            state = hazeState, style = HazeStyle(
                                tint = HazeTint(
                                    surfaceColor.copy(
                                        alpha = if (isDark) {
                                            0.18f
                                        } else {
                                            0.22f
                                        }
                                    )
                                ), blurRadius = 16.dp, noiseFactor = 0.015f
                            )
                        )
                    } else {
                        Modifier
                    }
                )
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.18f), Color.White.copy(alpha = 0.06f)
                        )
                    ), shape = shape
                )
                .border(
                    width = 0.5.dp, color = Color.White.copy(alpha = 0.20f), shape = shape
                )
                .clickable(
                    enabled = enabled, onClick = onClick
                ),

            contentAlignment = Alignment.Center
        ) {

            Icon(
                painter = painterResource(
                    id = iconRes
                ), contentDescription = label, tint = primaryColor, modifier = Modifier.size(24.dp)
            )
        }
        Spacer(
            modifier = Modifier.height(7.dp)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = textPrimary,
            maxLines = 1,
            softWrap = false
        )
    }
}