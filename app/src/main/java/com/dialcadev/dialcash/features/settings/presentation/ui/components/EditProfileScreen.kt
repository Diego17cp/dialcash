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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dialcadev.dialcash.R
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileScreen(
    modifier: Modifier = Modifier,
    hazeState: HazeState,
    topBarHeight: Dp,
    photoUri: Uri?,
    name: String,
    onNameChanged: (String) -> Unit,
    nameError: String?,
    currency: String,
    onCurrencyChanged: (String) -> Unit,
    currencyOptions: List<String>,
    currencyError: String?,
    isSaveEnabled: Boolean,
    onSaveClick: () -> Unit,
    onPickPhotoClick: () -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .hazeSource(hazeState)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp)
    ) {
        Spacer(modifier = Modifier.height(topBarHeight + 32.dp))

        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            EditableAvatar(
                photoUri = photoUri,
                size = 150.dp,
                onClick = onPickPhotoClick
            )
        }

        Spacer(modifier = Modifier.height(36.dp))

        OutlinedTextField(
            value = name,
            onValueChange = onNameChanged,
            label = { Text(stringResource(R.string.hint_user_name)) },
            singleLine = true,
            isError = nameError != null,
            supportingText = { nameError?.let { Text(it) } },
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
            ),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        CurrencyDropdown(
            selected = currency,
            options = currencyOptions,
            onSelected = onCurrencyChanged,
            isError = currencyError != null,
            errorText = currencyError
        )

        Spacer(modifier = Modifier.height(32.dp))

        InfoCard()

        Spacer(modifier = Modifier.height(48.dp))

        Button(
            onClick = onSaveClick,
            enabled = isSaveEnabled,
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) {
            Text(
                text = stringResource(R.string.save),
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        }

        Spacer(modifier = Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun EditableAvatar(photoUri: Uri?, size: Dp, onClick: () -> Unit) {
    val context = LocalContext.current
    var bitmap by remember(photoUri) { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(photoUri) {
        bitmap = photoUri?.let { uri ->
            runCatching {
                context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) }
            }.getOrNull()
        }
    }

    val primary = colorResource(id = R.color.colorPrimary)

    Box(
        modifier = Modifier
            .size(size)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                onClick = onClick
            ),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(CircleShape)
                .background(primary.copy(alpha = 0.12f))
                .border(2.dp, primary.copy(alpha = 0.5f), CircleShape),
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
                    tint = primary.copy(alpha = 0.4f),
                    modifier = Modifier.size(size * 0.6f)
                )
            }
        }
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = 2.dp, y = 2.dp)
                    .size(size * 0.32f)
                    .clip(CircleShape)
                    .background(primary)
                    .border(2.dp, MaterialTheme.colorScheme.background, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_pen_filled),
                    contentDescription = stringResource(id = R.string.change_pfp),
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(size * 0.16f)
                )
            }
        }

}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CurrencyDropdown(
    selected: String,
    options: List<String>,
    onSelected: (String) -> Unit,
    isError: Boolean,
    errorText: String?
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it }
    ) {
        OutlinedTextField(
            value = selected,
            onValueChange = {},
            readOnly = true,
            label = { Text(stringResource(R.string.hint_currency_symbol)) },
            supportingText = {
                Text(errorText ?: stringResource(R.string.helper_currency))
            },
            isError = isError,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
            ),
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor()
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        onSelected(option)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun InfoCard() {
    val isDark = isSystemInDarkTheme()
    val primary = colorResource(id = R.color.colorPrimary)

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
            )
            .padding(20.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(24.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(primary)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = stringResource(R.string.aesthetic_changes),
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.aesthetic_changes_bullets),
            fontSize = 14.sp,
            lineHeight = 20.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}