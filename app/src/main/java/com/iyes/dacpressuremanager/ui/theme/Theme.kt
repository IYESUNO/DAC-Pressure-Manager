package com.iyes.dacpressuremanager.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.graphics.Color
import com.iyes.dacpressuremanager.domain.PressureMode

private val DiamondLightColors = lightColorScheme(
    primary = Color(0xFF2980B9),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD4E9FF),
    onPrimaryContainer = Color(0xFF001D35),
    secondary = Color(0xFF2C3E50),
    background = Color(0xFFF0F2F5),
    surface = Color.White,
    surfaceContainer = Color(0xFFF0F2F5),
    surfaceVariant = Color(0xFFF0F2F5),
    onSurface = Color(0xFF333333),
    onSurfaceVariant = Color(0xFF7F8C8D),
    outline = Color(0xFFDDDDDD),
    outlineVariant = Color(0xFFEEEEEE),
    error = Color(0xFFC0392B),
)

private val DiamondDarkColors = darkColorScheme(
    primary = Color(0xFF78C5FF),
    onPrimary = Color(0xFF002C49),
    primaryContainer = Color(0xFF124F73),
    onPrimaryContainer = Color(0xFFD5ECFF),
    secondary = Color(0xFFB5C9D8),
    background = Color(0xFF080B0E),
    onBackground = Color(0xFFF2F5F7),
    surface = Color(0xFF0E1216),
    surfaceContainer = Color(0xFF151B21),
    surfaceVariant = Color(0xFF1B232B),
    onSurface = Color(0xFFF2F5F7),
    onSurfaceVariant = Color(0xFFAAB4BE),
    outline = Color(0xFF69737D),
    outlineVariant = Color(0xFF2B333B),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
)

private val RubyLightColors = lightColorScheme(
    primary = Color(0xFFE74C3C),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFDAD9),
    onPrimaryContainer = Color(0xFF410006),
    secondary = Color(0xFFC0392B),
    background = Color(0xFFF0F2F5),
    surface = Color.White,
    surfaceContainer = Color(0xFFF0F2F5),
    surfaceVariant = Color(0xFFF0F2F5),
    onSurface = Color(0xFF333333),
    onSurfaceVariant = Color(0xFF7F8C8D),
    outline = Color(0xFFDDDDDD),
    outlineVariant = Color(0xFFEEEEEE),
    error = Color(0xFFC0392B),
)

private val RubyDarkColors = darkColorScheme(
    primary = Color(0xFFFFAAA3),
    onPrimary = Color(0xFF5F0909),
    primaryContainer = Color(0xFF752420),
    onPrimaryContainer = Color(0xFFFFDAD7),
    secondary = Color(0xFFE4BFBC),
    background = Color(0xFF0D0909),
    onBackground = Color(0xFFF7F1F0),
    surface = Color(0xFF130F0F),
    surfaceContainer = Color(0xFF1C1616),
    surfaceVariant = Color(0xFF261D1D),
    onSurface = Color(0xFFF7F1F0),
    onSurfaceVariant = Color(0xFFC4B4B2),
    outline = Color(0xFF786A69),
    outlineVariant = Color(0xFF382D2C),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
)

@Composable
fun DacTheme(
    mode: PressureMode,
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colors = dacColorScheme(mode = mode, darkTheme = darkTheme)
    MaterialTheme(
        colorScheme = colors,
        typography = DacTypography,
    ) {
        ProvideTextStyle(
            value = DacTypography.bodyLarge,
            content = content,
        )
    }
}

internal fun dacColorScheme(
    mode: PressureMode,
    darkTheme: Boolean,
): ColorScheme = when {
    mode == PressureMode.DIAMOND && !darkTheme -> DiamondLightColors
    mode == PressureMode.DIAMOND -> DiamondDarkColors
    !darkTheme -> RubyLightColors
    else -> RubyDarkColors
}
