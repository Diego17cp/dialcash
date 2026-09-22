package com.dialcadev.dialcash.core.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dialcadev.dialcash.R
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect

sealed class NavigationItem(
    val routeId: Int,
    val iconRes: Int,
    val activeIconRes: Int,
    val titleRes: Int
) {
    object Home : NavigationItem(R.id.homeFragment, R.drawable.ic_home_outline, R.drawable.ic_home_filled, R.string.home)
    object Transactions : NavigationItem(R.id.transactionsFragment, R.drawable.ic_transactions_outline, R.drawable.ic_transactions_filled, R.string.transactions)
    object Accounts : NavigationItem(R.id.accountsFragment, R.drawable.ic_accounts_outline, R.drawable.ic_accounts_filled, R.string.accounts)
    object Incomes : NavigationItem(R.id.incomesFragment, R.drawable.ic_incomes_outline, R.drawable.ic_incomes_filled, R.string.incomes)
    object Blog : NavigationItem(R.id.blogFragment, R.drawable.ic_megaphone, R.drawable.ic_megaphone_filled, R.string.blog)
}

@Composable
fun FloatingBottomNav(
    modifier: Modifier = Modifier,
    hazeState: HazeState,
    currentDestinationId: Int?,
    onItemClick: (NavigationItem) -> Unit
) {
    val items = listOf(
        NavigationItem.Home,
        NavigationItem.Transactions,
        NavigationItem.Accounts,
        NavigationItem.Incomes,
        NavigationItem.Blog
    )
    val currentIndex = items.indexOfFirst { it.routeId == currentDestinationId }.coerceAtLeast(0)

    val config = LocalConfiguration.current
    val screenWidth = config.screenWidthDp.dp
    val barWidth = screenWidth - 48.dp
    val itemWidth = barWidth / items.size

    val pillLeftOffset by animateDpAsState(
        targetValue = (currentIndex * itemWidth.value).dp + 4.dp,
        animationSpec = tween(durationMillis = 350, easing = EaseOutCubic),
        label = "pillPosition"
    )

    val isDark = isSystemInDarkTheme()
    val backgroundColor = if (isDark) Color.Black else MaterialTheme.colorScheme.background

    val tintColor = if (isDark) Color(0xFF121212) else MaterialTheme.colorScheme.surface
    val blurAlpha = if (isDark) 0.65f else 0.35f

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(
                brush = Brush.verticalGradient(
                    0f to Color.Transparent,
                    0.4f to backgroundColor.copy(alpha = 0.6f),
                    1f to backgroundColor.copy(alpha = 0.98f)
                )
            )
            .navigationBarsPadding()
            .padding(bottom = 16.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(72.dp)
                .padding(horizontal = 24.dp)
                .graphicsLayer {
                    shadowElevation = 20f
                    spotShadowColor = Color.Black.copy(alpha = 0.08f)
                    ambientShadowColor = Color.Black.copy(alpha = 0.04f)
                }
                .clip(RoundedCornerShape(28.dp))
                .hazeEffect(
                    state = hazeState,
                    style = HazeStyle(
                        tint = HazeTint(tintColor.copy(alpha = blurAlpha)),
                        blurRadius = 25.dp,
                        noiseFactor = 0.02f
                    )
                )
                .border(
                    width = 0.5.dp,
                    color = if (isDark) Color.White.copy(alpha = 0.1f) else Color.Black.copy(alpha = 0.08f),
                    shape = RoundedCornerShape(28.dp)
                )
        ) {
            Box(
                modifier = Modifier
                    .padding(vertical = 6.dp)
                    .offset(x = pillLeftOffset)
                    .width(itemWidth - 10.dp)
                    .fillMaxHeight()
                    .background(
                        color = colorResource(id = R.color.colorPrimary).copy(alpha = 0.12f),
                        shape = RoundedCornerShape(22.dp)
                    )
                    .border(
                        width = 1.dp,
                        color = colorResource(id = R.color.colorPrimary).copy(alpha = 0.1f),
                        shape = RoundedCornerShape(22.dp)
                    )
            )

            Row(
                modifier = Modifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                items.forEachIndexed { index, item ->
                    val isActive = index == currentIndex

                    val iconScale by animateFloatAsState(
                        targetValue = if (isActive) 1.1f else 1.0f,
                        animationSpec = tween(durationMillis = 200),
                        label = "iconScale"
                    )
                    val tintColor by animateColorAsState(
                        targetValue = if (isActive) {
                            colorResource(id = R.color.colorPrimary)
                        } else {
                            if (isSystemInDarkTheme()) Color.White.copy(alpha = 0.5f) else Color.Black.copy(alpha = 0.45f)
                        },
                        animationSpec = tween(durationMillis = 200),
                        label = "textColor"
                    )

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { onItemClick(item) },
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            painter = painterResource(id = if (isActive) item.activeIconRes else item.iconRes),
                            contentDescription = stringResource(id = item.titleRes),
                            tint = tintColor,
                            modifier = Modifier
                                .padding(bottom = 4.dp)
                                .size(24.dp)
                                .graphicsLayer(scaleX = iconScale, scaleY = iconScale)
                        )
                        Text(
                            text = stringResource(id = item.titleRes),
                            fontSize = 11.sp,
                            fontWeight = if (isActive) FontWeight.W600 else FontWeight.W500,
                            letterSpacing = (-0.1).sp,
                            color = tintColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}