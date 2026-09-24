package com.dialcadev.dialcash.features.home.presentation.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dialcadev.dialcash.R

@Composable
fun MainAccountCard(
    modifier: Modifier = Modifier,
    accountName: String,
    balanceText: String,
    iconRes: Int = R.drawable.ic_accounts_outline,
    onClick: () -> Unit = {}
) {
    val shape = RoundedCornerShape(20.dp)

    val surfaceColor = colorResource(
        id = R.color.surface
    )

    val secondaryColor = colorResource(
        id = R.color.colorSecondary
    )

    val textPrimary = colorResource(
        id = R.color.text_primary
    )

    val textSecondary = colorResource(
        id = R.color.text_secondary
    )
    val backgroundGradient = Brush.verticalGradient(
        colors = listOf(
            secondaryColor.copy(alpha = 0.18f),
            secondaryColor.copy(alpha = 0.08f),
            surfaceColor.copy(alpha = 0.96f),
            surfaceColor
        )
    )

    Box(
        modifier = modifier
            .width(190.dp)
            .height(140.dp)
            .shadow(
                elevation = 4.dp,
                shape = shape,
                ambientColor = Color.Black.copy(alpha = 0.08f),
                spotColor = Color.Black.copy(alpha = 0.12f)
            )
            .clip(shape)
            .clickable { onClick() }
            .background(
                brush = backgroundGradient,
                shape = shape
            )
            .border(
                width = 0.5.dp,
                color = secondaryColor.copy(alpha = 0.12f),
                shape = shape
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(
                        RoundedCornerShape(15.dp)
                    )
                    .background(
                        secondaryColor.copy(alpha = 0.18f)
                    ),
                contentAlignment = Alignment.Center
            ) {

                Icon(
                    painter = painterResource(
                        id = iconRes
                    ),
                    contentDescription = null,
                    tint = secondaryColor,
                    modifier = Modifier.size(27.dp)
                )
            }

            Spacer(
                modifier = Modifier.weight(1f)
            )
            Text(
                text = balanceText,
                color = textPrimary,
                fontSize = 21.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.5).sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(
                modifier = Modifier.height(2.dp)
            )

            Text(
                text = accountName,
                color = textSecondary,
                style = MaterialTheme.typography.bodyMedium,
                fontSize = 14.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}