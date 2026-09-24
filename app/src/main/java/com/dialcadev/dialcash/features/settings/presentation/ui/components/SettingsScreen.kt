package com.dialcadev.dialcash.features.settings.presentation.ui.components

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.dialcadev.dialcash.R
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource

@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    hazeState: HazeState,
    topBarHeight: Dp,
    userName: String,
    photoUri: Uri?,
    themeSummary: String,
    onEditProfileClick: () -> Unit,
    onThemeClick: () -> Unit,
    onExportDataClick: () -> Unit,
    onImportDataClick: () -> Unit,
    onDeleteAccountClick: () -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .hazeSource(hazeState)
            .verticalScroll(rememberScrollState())
    ) {
        ProfileHero(
            topBarHeight = topBarHeight,
            userName = userName,
            photoUri = photoUri,
            onEditProfileClick = onEditProfileClick
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 24.dp)
        ) {
            SettingsGroup(title = stringResource(R.string.appearance)) {
                SettingsRow(
                    iconRes = R.drawable.ic_shadow_fill,
                    title = stringResource(R.string.theme),
                    subtitle = themeSummary,
                    onClick = onThemeClick
                )
            }
            Spacer(modifier = Modifier.height(24.dp))

            SettingsGroup(title = stringResource(R.string.data)) {
                SettingsRow(
                    iconRes = R.drawable.ic_download_fill,
                    title = stringResource(R.string.export_data),
                    onClick = onExportDataClick
                )
                RowDivider()
                SettingsRow(
                    iconRes = R.drawable.ic_upload_fill,
                    title = stringResource(R.string.import_data),
                    onClick = onImportDataClick
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            SettingsGroup(title = stringResource(R.string.hint_account)) {
                SettingsRow(
                    iconRes = R.drawable.ic_delete,
                    title = stringResource(R.string.delete_account),
                    tint = MaterialTheme.colorScheme.error,
                    onClick = onDeleteAccountClick
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
        }

        Spacer(modifier = Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
    }
}

@Composable
private fun ProfileHero(
    topBarHeight: Dp,
    userName: String,
    photoUri: Uri?,
    onEditProfileClick: () -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val primary = colorResource(id = R.color.colorPrimary)
    val tertiary = colorResource(id = R.color.colorPrimaryLight)
    val tintColor = if (isDark) Color(0xFF121212) else MaterialTheme.colorScheme.surface
    val blurAlpha = if (isDark) 0.65f else 0.35f
    val textColor = if (isDark) Color.White else MaterialTheme.colorScheme.onSurface
    val background = colorResource(id = R.color.background)
    val borderColor = if (isDark) Color.White.copy(alpha = 0.1f) else Color.Black.copy(alpha = 0.3f)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(
                        primary.copy(alpha = if (isDark) 0.9f else 0.85f),
                        tertiary.copy(alpha = if (isDark) 0.55f else 0.45f),
                        background
                    ),
                    start = Offset(0f, 0f),
                    end = Offset(0f, Float.POSITIVE_INFINITY)
                )
            )
            .padding(top = topBarHeight + 12.dp, bottom = 28.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            ProfileAvatar(photoUri = photoUri, size = 150.dp)

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = userName,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = textColor
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = stringResource(R.string.edit_profile),
                style = MaterialTheme.typography.labelLarge,
                color = textColor,
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(tintColor.copy(alpha = blurAlpha))
                    .border(0.5.dp, borderColor, RoundedCornerShape(50))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        onClick = onEditProfileClick
                    )
                    .padding(horizontal = 18.dp, vertical = 8.dp)
            )
        }
    }
}

@Composable
private fun ProfileAvatar(photoUri: Uri?, size: Dp) {
    val context = LocalContext.current
    var bitmap by remember(photoUri) { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(photoUri) {
        bitmap = photoUri?.let { uri ->
            runCatching {
                context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) }
            }.getOrNull()
        }
    }

    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.15f))
            .border(1.5.dp, Color.White.copy(alpha = if (isSystemInDarkTheme()) 0.3f else 0.5f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        val bmp = bitmap
        if (bmp != null) {
            Image(
                bitmap = bmp.asImageBitmap(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Icon(
                painter = painterResource(id = R.drawable.ic_account_circle),
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(size * 0.6f)
            )
        }
    }
}

@Composable
private fun SettingsGroup(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    val isDark = isSystemInDarkTheme()

    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 4.dp, bottom = 10.dp)
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(
                MaterialTheme.colorScheme.surface.copy(alpha = if (isDark) 0.6f else 0.9f)
            )
            .border(
                width = 0.5.dp,
                color = if (isDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.05f),
                shape = RoundedCornerShape(20.dp)
            ),
        content = content
    )
}

@Composable
private fun SettingsRow(
    iconRes: Int,
    title: String,
    subtitle: String? = null,
    tint: Color = MaterialTheme.colorScheme.onSurface,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                onClick = onClick
            )
            .padding(horizontal = 18.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(id = iconRes),
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = tint
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Icon(
            painter = painterResource(id = R.drawable.ic_chevron_right),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
private fun RowDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(horizontal = 18.dp),
        thickness = 0.5.dp,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f)
    )
}