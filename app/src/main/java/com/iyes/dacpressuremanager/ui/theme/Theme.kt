package com.iyes.dacpressuremanager.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
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
    primary = Color(0xFF9CCAFF),
    onPrimary = Color(0xFF003258),
    primaryContainer = Color(0xFF004A7E),
    onPrimaryContainer = Color(0xFFD1E8FF),
    secondary = Color(0xFFAAC8E1),
    surface = Color(0xFF101418),
    surfaceContainer = Color(0xFF1C2024),
    error = Color(0xFFFFB4AB),
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
    primary = Color(0xFFFFB3B3),
    onPrimary = Color(0xFF68000E),
    primaryContainer = Color(0xFF910C1E),
    onPrimaryContainer = Color(0xFFFFDAD9),
    secondary = Color(0xFFE7BDBC),
    surface = Color(0xFF181212),
    surfaceContainer = Color(0xFF251D1D),
    error = Color(0xFFFFB4AB),
)

@Composable
fun DacTheme(
    mode: PressureMode,
    darkTheme: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colors = when {
        mode == PressureMode.DIAMOND && !darkTheme -> DiamondLightColors
        mode == PressureMode.DIAMOND -> DiamondDarkColors
        !darkTheme -> RubyLightColors
        else -> RubyDarkColors
    }
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
