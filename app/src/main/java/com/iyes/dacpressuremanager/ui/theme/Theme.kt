package com.iyes.dacpressuremanager.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import com.iyes.dacpressuremanager.domain.PressureMode
import com.iyes.dacpressuremanager.domain.ThemePalette

internal data class DacAccentColors(
    val action: Color,
    val actionStrong: Color,
    val resultFrom: Color,
    val resultTo: Color,
    val onAction: Color,
    val text: Color,
    val selection: Color,
    val onSelection: Color,
    val statusBackground: Color,
    val buttonBackground: Color,
    val buttonBackgroundPressed: Color,
    val buttonBorder: Color,
    val textStrong: Color = bestBlackOrWhite(listOf(buttonBackground, buttonBackgroundPressed)),
) {
    // Text and non-text indicators have different contrast requirements.
    val buttonText: Color get() = readableColor(
        text, listOf(buttonBackground, buttonBackgroundPressed), alternative = textStrong,
    )
    val statusMarker: Color get() = readableColor(text, listOf(statusBackground), 3f, textStrong)
    val focusBorder: Color get() = readableColor(
        buttonBorder, listOf(buttonBackground, buttonBackgroundPressed), 3f, textStrong,
    )
}

@Composable
internal fun dacFilledButtonColors() = LocalDacAccentColors.current.let { accent ->
    ButtonDefaults.buttonColors(containerColor = accent.action, contentColor = accent.onAction)
}

internal val LocalDacAccentColors = staticCompositionLocalOf {
    DacAccentColors(
        action = Color(0xFF2980B9),
        actionStrong = Color(0xFF3498DB),
        resultFrom = Color(0xFF236F9D),
        resultTo = Color(0xFF2676AA),
        onAction = Color.White,
        text = Color(0xFF2980B9),
        selection = Color(0xFF2C3E50),
        onSelection = Color.White,
        statusBackground = Color(0xFFE6F4FE),
        buttonBackground = Color(0xFFE6F4FE),
        buttonBackgroundPressed = Color(0xFFC2E5FF),
        buttonBorder = Color(0xFF5EB1EF),
    )
}

private val DiamondLightColors = lightColorScheme(
    primary = Color(0xFF236F9D),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD4E9FF),
    onPrimaryContainer = Color(0xFF001D35),
    secondary = Color(0xFF2C3E50),
    background = Color(0xFFF0F2F5),
    surface = Color.White,
    surfaceContainer = Color(0xFFF0F2F5),
    surfaceVariant = Color(0xFFF0F2F5),
    onSurface = Color(0xFF333333),
    onSurfaceVariant = Color(0xFF60646C),
    outline = Color(0xFF80838D),
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
    primary = Color(0xFFC0392B),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFDAD9),
    onPrimaryContainer = Color(0xFF410006),
    secondary = Color(0xFFC0392B),
    background = Color(0xFFF0F2F5),
    surface = Color.White,
    surfaceContainer = Color(0xFFF0F2F5),
    surfaceVariant = Color(0xFFF0F2F5),
    onSurface = Color(0xFF333333),
    onSurfaceVariant = Color(0xFF60646C),
    outline = Color(0xFF80838D),
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
    palette: ThemePalette? = null,
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val selectedPalette = palette ?: ThemePalette.DEFAULT
    val accentColors = dacAccentColors(mode, selectedPalette, darkTheme)
    val colors = dacColorScheme(
        mode = mode,
        darkTheme = darkTheme,
        palette = selectedPalette,
    )
    CompositionLocalProvider(LocalDacAccentColors provides accentColors) {
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
}

internal fun dacColorScheme(
    mode: PressureMode,
    darkTheme: Boolean,
    palette: ThemePalette = ThemePalette.DEFAULT,
): ColorScheme {
    val baseColors = when {
    mode == PressureMode.DIAMOND && !darkTheme -> DiamondLightColors
    mode == PressureMode.DIAMOND -> DiamondDarkColors
    !darkTheme -> RubyLightColors
    else -> RubyDarkColors
    }
    // Material dialogs/menus also consume the container ladder. Override every
    // level so they cannot fall back to the stock purple Material palette.
    val base = baseColors.copy(
        surfaceDim = if (darkTheme) baseColors.surface else Color(0xFFE0E1E6),
        surfaceBright = if (darkTheme) Color(0xFF30353A) else Color.White,
        surfaceContainerLowest = if (darkTheme) baseColors.background else Color.White,
        surfaceContainerLow = if (darkTheme) baseColors.surface else Color(0xFFF9F9FB),
        surfaceContainerHigh = baseColors.surfaceContainer,
        surfaceContainerHighest = if (darkTheme) baseColors.surfaceVariant else Color(0xFFE8E8EC),
        surfaceTint = Color.Transparent,
    )
    if (palette == ThemePalette.DEFAULT) return base

    val scaleColors = palette.scale().colors(darkTheme)
    val accent = dacAccentColors(mode, palette, darkTheme)
    val primary = readableColor(
        accent.text, listOf(base.surface, base.surfaceContainerHigh), alternative = scaleColors[11],
    )
    return base.copy(
        // Material uses primary for text, cursors and radio indicators too.
        // Bright solid fills belong to DacAccentColors, not this text role.
        primary = primary,
        onPrimary = bestBlackOrWhite(primary),
        primaryContainer = scaleColors[2],
        onPrimaryContainer = scaleColors[11],
        secondary = accent.text,
        onSecondary = bestBlackOrWhite(accent.text),
        secondaryContainer = scaleColors[4],
        onSecondaryContainer = scaleColors[11],
        tertiary = accent.text,
        onTertiary = bestBlackOrWhite(accent.text),
        tertiaryContainer = scaleColors[3],
        onTertiaryContainer = scaleColors[11],
    )
}

internal fun dacAccentColors(
    mode: PressureMode,
    palette: ThemePalette,
    darkTheme: Boolean,
): DacAccentColors {
    if (palette == ThemePalette.DEFAULT) {
        return when {
            mode == PressureMode.DIAMOND && !darkTheme -> DacAccentColors(
                action = Color(0xFF2676AA),
                actionStrong = Color(0xFF236F9D),
                resultFrom = Color(0xFF236F9D),
                resultTo = Color(0xFF2676AA),
                onAction = Color.White,
                text = Color(0xFF236F9D),
                selection = Color(0xFF2C3E50),
                onSelection = Color.White,
                statusBackground = Color(0xFFE6F4FE),
                buttonBackground = Color(0xFFE6F4FE),
                buttonBackgroundPressed = Color(0xFFC2E5FF),
                buttonBorder = Color(0xFF5EB1EF),
                textStrong = ThemePalette.BLUE.scale().light[11],
            )
            mode == PressureMode.DIAMOND -> DacAccentColors(
                action = Color(0xFF124F73),
                actionStrong = Color(0xFF0B3E5D),
                resultFrom = Color(0xFF0B3E5D),
                resultTo = Color(0xFF124F73),
                onAction = Color(0xFFD5ECFF),
                text = Color(0xFF78C5FF),
                selection = Color(0xFF2C3E50),
                onSelection = Color.White,
                statusBackground = Color(0xFF0D2847),
                buttonBackground = Color(0xFF0D2847),
                buttonBackgroundPressed = Color(0xFF004074),
                buttonBorder = Color(0xFF2870BD),
                textStrong = ThemePalette.BLUE.scale().dark[11],
            )
            !darkTheme -> DacAccentColors(
                action = Color(0xFFC63E32),
                actionStrong = Color(0xFFA83229),
                resultFrom = Color(0xFFA83229),
                resultTo = Color(0xFFC63E32),
                onAction = Color.White,
                text = Color(0xFFC0392B),
                selection = Color(0xFFB9382E),
                onSelection = Color.White,
                statusBackground = Color(0xFFFFEBEC),
                buttonBackground = Color(0xFFFFEBEC),
                buttonBackgroundPressed = Color(0xFFFFCDCE),
                buttonBorder = Color(0xFFEB8E90),
                textStrong = ThemePalette.RED.scale().light[11],
            )
            else -> DacAccentColors(
                action = Color(0xFF752420),
                actionStrong = Color(0xFF5F1917),
                resultFrom = Color(0xFF5F1917),
                resultTo = Color(0xFF752420),
                onAction = Color(0xFFFFDAD7),
                text = Color(0xFFFFAAA3),
                selection = Color(0xFFB9382E),
                onSelection = Color.White,
                statusBackground = Color(0xFF3B1219),
                buttonBackground = Color(0xFF3B1219),
                buttonBackgroundPressed = Color(0xFF611623),
                buttonBorder = Color(0xFFB54548),
                textStrong = ThemePalette.RED.scale().dark[11],
            )
        }
    }

    val scale = palette.scale()
    val lightAction = scale.light[8]
    val usesBrightSolid = contrastRatio(Color.Black, lightAction) >= 8f
    val colors = scale.colors(darkTheme)
    val action = when {
        !darkTheme && usesBrightSolid -> colors[8]
        !darkTheme -> colors[10]
        usesBrightSolid -> colors[3]
        else -> colors[7]
    }
    val actionStrong = when {
        !darkTheme && usesBrightSolid -> colors[9]
        !darkTheme -> lerp(action, Color.Black, 0.14f)
        usesBrightSolid -> colors[2]
        else -> lerp(action, Color.Black, 0.14f)
    }
    val resultFrom = if (darkTheme && usesBrightSolid) actionStrong else action
    val resultTo = when {
        darkTheme && usesBrightSolid -> action
        usesBrightSolid -> actionStrong
        else -> lerp(action, Color.Black, 0.18f)
    }
    val onAction = if (usesBrightSolid) {
        if (darkTheme) colors[10] else colors[11]
    } else {
        bestBlackOrWhite(listOf(action, actionStrong, resultFrom, resultTo))
    }
    return DacAccentColors(
        action = action,
        actionStrong = actionStrong,
        resultFrom = resultFrom,
        resultTo = resultTo,
        onAction = onAction,
        text = readableColor(
            colors[10],
            if (darkTheme) listOf(Color(0xFF0E1216), Color(0xFF261D1D))
            else listOf(Color.White, Color(0xFFF0F2F5)),
            alternative = colors[11],
        ),
        selection = action,
        onSelection = onAction,
        statusBackground = colors[2],
        buttonBackground = colors[2],
        buttonBackgroundPressed = colors[4],
        buttonBorder = colors[7],
        textStrong = colors[11],
    )
}

// Prefer the palette's high-contrast text step before falling back to a neutral.
internal fun readableColor(
    preferred: Color,
    backgrounds: List<Color>,
    minimum: Float = 4.5f,
    alternative: Color = bestBlackOrWhite(backgrounds),
): Color = if (backgrounds.all { contrastRatio(preferred, it) >= minimum }) preferred else alternative

private fun bestBlackOrWhite(background: Color): Color =
    bestBlackOrWhite(listOf(background))

private fun bestBlackOrWhite(backgrounds: List<Color>): Color =
    if (
        backgrounds.minOf { contrastRatio(Color.Black, it) } >=
        backgrounds.minOf { contrastRatio(Color.White, it) }
    ) {
        Color.Black
    } else {
        Color.White
    }

private fun contrastRatio(first: Color, second: Color): Float {
    val lighter = maxOf(first.luminance(), second.luminance())
    val darker = minOf(first.luminance(), second.luminance())
    return (lighter + 0.05f) / (darker + 0.05f)
}
