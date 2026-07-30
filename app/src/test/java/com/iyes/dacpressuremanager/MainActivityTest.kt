package com.iyes.dacpressuremanager

import org.junit.Assert.assertEquals
import org.junit.Test

class MainActivityTest {
    @Test
    fun extendedScreenTimeoutDoublesSystemValueWithinThreeToTenMinutes() {
        assertEquals(3 * 60 * 1_000L, calculateExtendedScreenTimeoutMs(30_000L))
        assertEquals(4 * 60 * 1_000L, calculateExtendedScreenTimeoutMs(2 * 60 * 1_000L))
        assertEquals(10 * 60 * 1_000L, calculateExtendedScreenTimeoutMs(30 * 60 * 1_000L))
    }

    @Test
    fun extendedScreenTimeoutUsesSafeFallbackForInvalidSystemValue() {
        assertEquals(4 * 60 * 1_000L, calculateExtendedScreenTimeoutMs(-1L))
    }
}
