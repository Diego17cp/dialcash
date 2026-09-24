package com.dialcadev.dialcash.features.blog.presentation.ui.components

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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.dialcadev.dialcash.R

@Composable
fun BlogPostCard(
    modifier: Modifier = Modifier,
    title: String,
    description: String,
    category: String,
    dateText: String,
    imageUrl: String,
    onClick: () -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val shape = RoundedCornerShape(22.dp)

    val surfaceColor = colorResource(id = R.color.surface)
    val secondaryColor = colorResource(id = R.color.colorSecondary)
    val tertiaryColor = colorResource(id = R.color.colorPrimaryLight)
    val textPrimary = colorResource(id = R.color.text_primary)
    val textSecondary = colorResource(id = R.color.text_secondary)

    val borderColor = if (isDark) Color.White.copy(alpha = 0.10f) else secondaryColor.copy(alpha = 0.14f)

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

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = if (isPressed) 0.985f else 1f,
        label = "blogPostCardScale"
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
            .border(width = 0.6.dp, color = borderColor, shape = shape)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .padding(12.dp)
    ) {
        Column {
            AsyncImage(
                model = imageUrl,
                contentDescription = title,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .clip(RoundedCornerShape(16.dp)),
                contentScale = ContentScale.Crop
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                GlassInfoChip(text = category.uppercase())
                GlassInfoChip(text = dateText)
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = if (isDark) Color.White else textPrimary,
                fontSize = 16.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = if (isDark) Color.White.copy(alpha = 0.66f) else textSecondary,
                fontSize = 13.sp,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun GlassInfoChip(text: String) {
    val isDark = isSystemInDarkTheme()
    val chipBg = if (isDark) Color.White.copy(alpha = 0.10f) else Color.Black.copy(alpha = 0.05f)
    val chipText = if (isDark) Color.White else MaterialTheme.colorScheme.onSurface

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(chipBg)
            .padding(horizontal = 10.dp, vertical = 5.dp)
    ) {
        Text(
            text = text,
            color = chipText.copy(alpha = 0.90f),
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1
        )
    }
}