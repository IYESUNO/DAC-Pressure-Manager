package com.iyes.dacpressuremanager.ui

import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DacLayoutMetricsTest {
    @Test
    fun tallPhone_keepsInstrumentControlsAtStableHeight() {
        val layout = dacLayoutMetrics(
            maxWidth = 393.dp,
            maxHeight = 820.dp,
            fontScale = 1f,
        )

        assertFalse(layout.compact)
        assertFalse(layout.short)
        assertEquals(370.dp, layout.dashboardMaxHeight)
        assertEquals(112.dp, layout.resultHeight)
    }

    @Test
    fun narrowPhone_usesCompactMetricsWithoutBecomingShortLayout() {
        val layout = dacLayoutMetrics(
            maxWidth = 360.dp,
            maxHeight = 780.dp,
            fontScale = 1f,
        )

        assertTrue(layout.compact)
        assertFalse(layout.short)
        assertEquals(350.dp, layout.dashboardMaxHeight)
        assertEquals(104.dp, layout.resultHeight)
    }

    @Test
    fun shortLandscape_usesSideBySideCounterMetrics() {
        val layout = dacLayoutMetrics(
            maxWidth = 700.dp,
            maxHeight = 500.dp,
            fontScale = 1f,
        )

        assertTrue(layout.compact)
        assertTrue(layout.short)
        assertEquals(280.dp, layout.dashboardMaxHeight)
        assertEquals(72.dp, layout.resultHeight)
    }

    @Test
    fun enlargedText_usesCompactSpacing() {
        val layout = dacLayoutMetrics(
            maxWidth = 420.dp,
            maxHeight = 800.dp,
            fontScale = 1.3f,
        )

        assertTrue(layout.compact)
        assertEquals(8.dp, layout.outerPadding)
        assertEquals(6.dp, layout.sectionGap)
    }
}
