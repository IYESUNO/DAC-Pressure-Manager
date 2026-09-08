package com.iyes.dacpressuremanager.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import com.iyes.dacpressuremanager.domain.PressureMode
import com.iyes.dacpressuremanager.domain.ThemePalette
import com.iyes.dacpressuremanager.domain.ThemeAppearance
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PaletteScalesTest {
    @Test
    fun appearanceSelectionResolvesSystemLightAndDarkModes() {
        assertEquals(false, ThemeAppearance.SYSTEM.useDarkTheme(false))
        assertEquals(true, ThemeAppearance.SYSTEM.useDarkTheme(true))
        assertEquals(false, ThemeAppearance.LIGHT.useDarkTheme(true))
        assertEquals(true, ThemeAppearance.DARK.useDarkTheme(false))
        assertEquals(ThemeAppearance.SYSTEM, ThemeAppearance.fromStorage("invalid"))
    }

    @Test
    fun allThirtyOneCustomPalettesContainTwelveLightAndDarkSteps() {
        assertEquals(32, ThemePalette.entries.size)
        ThemePalette.entries.drop(1).forEach { palette ->
            assertEquals("${palette.name} light", 12, palette.scale().light.size)
            assertEquals("${palette.name} dark", 12, palette.scale().dark.size)
        }
    }

    @Test
    fun everyActionColorChoosesReadableContentInBothSystemThemes() {
        PressureMode.entries.forEach { mode ->
            ThemePalette.entries.forEach { palette ->
                listOf(false, true).forEach { darkTheme ->
                    val accent = dacAccentColors(mode, palette, darkTheme)
                    assertTrue(
                        "$mode ${palette.name} dark=$darkTheme contrast",
                        contrast(accent.action, accent.onAction) >= 4.5f,
                    )
                    assertTrue(
                        "$mode ${palette.name} dark=$darkTheme strong contrast",
                        contrast(accent.actionStrong, accent.onAction) >= 4.5f,
                    )
                    assertTrue(
                        "$mode ${palette.name} dark=$darkTheme result start contrast",
                        contrast(accent.resultFrom, accent.onAction) >= 4.5f,
                    )
                    assertTrue(
                        "$mode ${palette.name} dark=$darkTheme result end contrast",
                        contrast(accent.resultTo, accent.onAction) >= 4.5f,
                    )
                }
            }
        }
    }

    @Test
    fun customThemeRolesComeFromTheMatchingRadixScale() {
        ThemePalette.entries.drop(1).forEach { palette ->
            listOf(false, true).forEach { darkTheme ->
                val scale = palette.scale()
                val colors = scale.colors(darkTheme)
                val accent = dacAccentColors(PressureMode.DIAMOND, palette, darkTheme)
                val usesBrightSolid = contrast(Color.Black, scale.light[8]) >= 8f
                val expectedAction = when {
                    !darkTheme && usesBrightSolid -> colors[8]
                    !darkTheme -> colors[10]
                    usesBrightSolid -> colors[3]
                    else -> colors[7]
                }
                val expectedStrong = when {
                    !darkTheme && usesBrightSolid -> colors[9]
                    !darkTheme -> lerp(expectedAction, Color.Black, 0.14f)
                    usesBrightSolid -> colors[2]
                    else -> lerp(expectedAction, Color.Black, 0.14f)
                }

                assertEquals("${palette.name} dark=$darkTheme action", expectedAction, accent.action)
                assertEquals("${palette.name} dark=$darkTheme strong", expectedStrong, accent.actionStrong)
                assertNotEquals("${palette.name} dark=$darkTheme gradient", accent.action, accent.actionStrong)
                assertEquals("${palette.name} dark=$darkTheme text", colors[10], accent.text)
                assertEquals("${palette.name} dark=$darkTheme selection", expectedAction, accent.selection)
                assertEquals("${palette.name} dark=$darkTheme status", colors[2], accent.statusBackground)
                assertEquals("${palette.name} dark=$darkTheme button", colors[2], accent.buttonBackground)
                assertEquals("${palette.name} dark=$darkTheme pressed", colors[4], accent.buttonBackgroundPressed)
                assertEquals("${palette.name} dark=$darkTheme border", colors[7], accent.buttonBorder)
            }
        }
    }

    @Test
    fun accentSelectionDoesNotTintNeutralMaterialOutlines() {
        listOf(false, true).forEach { darkTheme ->
            val default = dacColorScheme(PressureMode.DIAMOND, darkTheme, ThemePalette.DEFAULT)
            val yellow = dacColorScheme(PressureMode.DIAMOND, darkTheme, ThemePalette.YELLOW)

            assertEquals(default.outline, yellow.outline)
            assertEquals(default.outlineVariant, yellow.outlineVariant)
        }
    }

    private fun contrast(first: Color, second: Color): Float {
        val lighter = maxOf(first.luminance(), second.luminance())
        val darker = minOf(first.luminance(), second.luminance())
        return (lighter + 0.05f) / (darker + 0.05f)
    }
}
