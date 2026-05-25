package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val LightColorScheme = lightColorScheme(
    primary = PrimaryTeal,
    onPrimary = PureWhite,
    secondary = SecondaryIndigo,
    onSecondary = PureWhite,
    tertiary = WarningAmber,
    onTertiary = PureWhite,
    background = PureWhite,
    onBackground = TextPrimary,
    surface = LightGreyCard,
    onSurface = TextPrimary,
    surfaceVariant = LightGreyCard,
    onSurfaceVariant = TextSecondary,
    outline = SubtleBorder,
    error = DangerRed,
    onError = PureWhite
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Disable dynamic colors to enforce the professional branding
    content: @Composable () -> Unit,
) {
    // Force light scheme for clean white professional theme
    val colorScheme = LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
