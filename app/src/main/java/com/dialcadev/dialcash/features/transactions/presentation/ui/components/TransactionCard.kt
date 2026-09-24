package com.dialcadev.dialcash.features.transactions.presentation.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dialcadev.dialcash.R

@Composable
fun TransactionCard(
    modifier: Modifier = Modifier,
    title: String,
    meta: String,
    amountText: String,
    amountColor: Color,
    iconRes: Int,
    onClick: () -> Unit = {}
) {
    val isDark = isSystemInDarkTheme()
    val shape = RoundedCornerShape(22.dp)

    val surfaceColor = colorResource(id = R.color.surface)
    val primaryColor = colorResource(id = R.color.colorPrimary)
    val secondaryColor = colorResource(id = R.color.colorSecondary)
    val tertiaryColor = colorResource(id = R.color.colorPrimaryLight)
    val textPrimary = colorResource(id = R.color.text_primary)
    val textSecondary = colorResource(id = R.color.text_secondary)

    val borderColor = if (isDark) {
        Color.White.copy(alpha = 0.10f)
    } else {
        secondaryColor.copy(alpha = 0.14f)
    }

    val cardGradient = if (isDark) {
        Brush.verticalGradient(
            colors = listOf(
                Color.Gray.copy(alpha = 0.06f),
                surfaceColor.copy(alpha = 0.14f),
                surfaceColor.copy(alpha = 0.22f),
                Color.DarkGray.copy(alpha = 0.18f)
            )
        )
    } else {
        Brush.verticalGradient(
            colors = listOf(
                Color.White,
                surfaceColor.copy(alpha = 0.98f),
                tertiaryColor.copy(alpha = 0.05f),
                tertiaryColor.copy(alpha = 0.10f)
            )
        )
    }

    val iconBg = if (isDark) {
        primaryColor.copy(alpha = 0.18f)
    } else {
        primaryColor.copy(alpha = 0.12f)
    }

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = if (isPressed) 0.985f else 1.0f,
        label = "transactionCardScale"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .graphicsLayer {
                scaleX = pressScale
                scaleY = pressScale
            }
            .shadow(
                elevation = if (isDark) 10.dp else 7.dp,
                shape = shape,
                ambientColor = Color.Black.copy(alpha = if (isDark) 0.20f else 0.06f),
                spotColor = Color.Black.copy(alpha = if (isDark) 0.28f else 0.10f)
            )
            .clip(shape)
            .background(brush = cardGradient, shape = shape)
            .border(
                width = 0.6.dp,
                color = borderColor,
                shape = shape
            )
            .clickable(
                interactionSource = interactionSource,
                onClick = onClick
            )
            .padding(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(iconBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = iconRes),
                    contentDescription = null,
                    tint = if (isDark) Color.White else primaryColor,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isDark) Color.White else textPrimary,
                    fontSize = 15.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = meta,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isDark) Color.White.copy(alpha = 0.62f) else textSecondary,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Text(
                text = amountText,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = amountColor,
                fontSize = 15.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}