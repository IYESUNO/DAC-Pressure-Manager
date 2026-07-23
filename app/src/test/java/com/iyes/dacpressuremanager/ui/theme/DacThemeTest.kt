package com.iyes.dacpressuremanager.ui.theme

import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontFamily
import com.iyes.dacpressuremanager.domain.PressureMode
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DacThemeTest {
    @Test
    fun darkSchemes_useDarkSurfacesWithLightContent() {
        PressureMode.entries.forEach { mode ->
            val colors = dacColorScheme(mode = mode, darkTheme = true)

            assertTrue(colors.background.luminance() < 0.02f)
            assertTrue(colors.surface.luminance() < 0.03f)
            assertTrue(colors.surfaceVariant.luminance() < 0.05f)
            assertTrue(colors.onSurface.luminance() > 0.8f)
            assertTrue(
                colors.onSurface.luminance() -
                    colors.surface.luminance() > 0.75f,
            )
        }
    }

    @Test
    fun lightAndDarkSchemes_keepModeIdentity() {
        PressureMode.entries.forEach { mode ->
            val light = dacColorScheme(mode = mode, darkTheme = false)
            val dark = dacColorScheme(mode = mode, darkTheme = true)

            assertNotEquals(light.primary, dark.primary)
            assertNotEquals(light.surface, dark.surface)
        }
    }

    @Test
    fun typography_usesBundledFontInsteadOfDeviceSansAlias() {
        val family = DacTypography.bodyLarge.fontFamily

        assertNotNull(family)
        assertNotEquals(FontFamily.SansSerif, family)
    }
}
