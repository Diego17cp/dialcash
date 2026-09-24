package com.dialcadev.dialcash.features.home.presentation.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.colorResource
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

    Box(modifier = modifier
        .fillMaxWidth()
        .graphicsLayer() {
            shadowElevation = 16f
            spotShadowColor = Color.Black.copy(alpha = if (isDark) 0.35f else 0.08f)
            ambientShadowColor = Color.Black.copy(alpha = if (isDark) 0.20f else 0.04f)
            clip = true
            this.shape = shape
        }
        .clip(shape)
        .then(
            if (hazeState != null) {
                Modifier.hazeEffect(
                    state = hazeState, style = HazeStyle(
                        tint = HazeTint(tintColor.copy(alpha = blurAlpha)),
                        blurRadius = 24.dp,
                        noiseFactor = 0.02f
                    )
                )
            } else Modifier
        )
        .background(
            brush = Brush.radialGradient(
                colors = if (isDark) {
                    listOf(
                        primaryColor.copy(alpha = 0.30f),
                        tertiaryColor.copy(alpha = 0.10f),
                        Color(0xFF12121A).copy(alpha = 0.85f)
                    )
                } else {
                    listOf(
                        primaryColor.copy(alpha = 0.22f),
                        tertiaryColor.copy(alpha = 0.08f),
                        surfaceColor.copy(alpha = 0.65f)
                    )
                }, center = Offset(0f, 0f), radius = 900f
            ), shape = shape
        )
        .border(width = 0.5.dp, color = borderColor, shape = shape)
        .padding(20.dp)) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(id = R.string.total_balance),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    color = textSecondary,
                    fontSize = 15.sp
                )
                Spacer(modifier = Modifier.width(8.dp))
                GlassIconButton(
                    size = 32.dp,
                    iconRes = if (isBalanceVisible) R.drawable.ic_eye_closed else R.drawable.ic_eye,
                    contentDescription = stringResource(id = R.string.total_balance),
                    hazeState = hazeState,
                    standalone = true,
                    onClick = onToggleVisibility
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = totalBalanceText,
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                fontSize = 38.sp,
                color = textPrimary,
                letterSpacing = (-0.5).sp
            )
            Spacer(modifier = Modifier.height(24.dp))
            Row(
                modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround
            ) {
                QuickActionButton(
                    iconRes = R.drawable.ic_income,
                    label = stringResource(id = R.string.income),
                    enabled = actionsEnabled,
                    hazeState = hazeState,
                    onClick = onQuickIncomeClick
                )

                QuickActionButton(
                    iconRes = R.drawable.ic_expense,
                    label = stringResource(id = R.string.expense),
                    enabled = actionsEnabled,
                    hazeState = hazeState,
                    onClick = onQuickExpenseClick
                )

                QuickActionButton(
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
    iconRes: Int, label: String, enabled: Boolean, hazeState: HazeState? = null, onClick: () -> Unit
) {
    val textPrimary = colorResource(id = R.color.text_primary)
    val primaryColor = colorResource(id = R.color.colorPrimary)

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.graphicsLayer { alpha = if (enabled) 1f else 0.45f }) {
        GlassIconButton(
            size = 56.dp,
            iconRes = iconRes,
            contentDescription = label,
            iconTint = primaryColor,
            hazeState = hazeState,
            standalone = true,
            onClick = { if (enabled) onClick() })

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            style = MaterialTheme.typography.labelMedium, text = label, color = textPrimary
        )
    }
}