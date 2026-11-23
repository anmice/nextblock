package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val LightColorScheme = lightColorScheme(
    primary = ForestGreen,
    onPrimary = Color.White,
    primaryContainer = SageTint,
    onPrimaryContainer = DeepPine,
    secondary = TerracottaAmber,
    onSecondary = Color.White,
    secondaryContainer = WarmSand,
    onSecondaryContainer = Color(0xFF5A2B05),
    tertiary = TealFound,
    onTertiary = Color.White,
    tertiaryContainer = TealTint,
    onTertiaryContainer = Color(0xFF0A3A44),
    error = CrimsonAlert,
    onError = Color.White,
    errorContainer = CrimsonTint,
    onErrorContainer = Color(0xFF6B140C),
    background = CreamCanvas,
    onBackground = PineInk,
    surface = CrispPaper,
    onSurface = PineInk,
    surfaceVariant = SoftLinen,
    onSurfaceVariant = SlateMuted,
    outline = WarmDivider,
    outlineVariant = Color(0xFFECE7DD)
)

private val DarkColorScheme = darkColorScheme(
    primary = MintLuminous,
    onPrimary = Color(0xFF042D1E),
    primaryContainer = Color(0xFF0D4933),
    onPrimaryContainer = SageTint,
    secondary = AmberBright,
    onSecondary = Color(0xFF3E1C02),
    secondaryContainer = Color(0xFF5B310F),
    onSecondaryContainer = WarmSand,
    tertiary = Color(0xFF6FD3E6),
    onTertiary = Color(0xFF042A32),
    tertiaryContainer = Color(0xFF104954),
    onTertiaryContainer = TealTint,
    error = Color(0xFFF28B82),
    onError = Color(0xFF4F0E07),
    errorContainer = Color(0xFF691A12),
    onErrorContainer = CrimsonTint,
    background = NightPineBg,
    onBackground = NightCreamText,
    surface = NightPineSurface,
    onSurface = NightCreamText,
    surfaceVariant = NightPineElevated,
    onSurfaceVariant = NightMutedText,
    outline = Color(0xFF33473E),
    outlineVariant = Color(0xFF25352E)
)

val NextBlockShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun NextBlockTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = NextBlockTypography,
        shapes = NextBlockShapes,
        content = content
    )
}
