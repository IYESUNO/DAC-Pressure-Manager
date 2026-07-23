package com.iyes.dacpressuremanager.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PressureCalculatorTest {
    @Test
    fun defaultValuesProduceZeroPressure() {
        PressureMode.entries.forEach { mode ->
            val computation = PressureCalculator.calculate(
                mode = mode,
                referenceCenti = mode.defaultValueCenti,
                measuredCenti = mode.defaultValueCenti,
            )
            assertEquals(0, computation.shiftCenti)
            assertEquals(0, (computation.result as PressureResult.Valid).pressureCenti)
        }
    }

    @Test
    fun diamondMatchesHtmlReferenceVectors() {
        assertValidPressure(
            mode = PressureMode.DIAMOND,
            referenceCenti = 133_300,
            measuredCenti = 140_000,
            expectedPressureCenti = 2_939,
        )
        assertValidPressure(
            mode = PressureMode.DIAMOND,
            referenceCenti = 133_300,
            measuredCenti = 180_000,
            expectedPressureCenti = 28_395,
        )
    }

    @Test
    fun rubyMatchesHtmlReferenceVectorsAndAllowsNegativePressure() {
        assertValidPressure(
            mode = PressureMode.RUBY,
            referenceCenti = 69_424,
            measuredCenti = 70_000,
            expectedPressureCenti = 1_624,
        )
        assertValidPressure(
            mode = PressureMode.RUBY,
            referenceCenti = 69_424,
            measuredCenti = 69_000,
            expectedPressureCenti = -1_139,
        )
    }

    @Test
    fun diamondRejectsNegativeAndAboveCalibrationPressures() {
        assertTrue(
            PressureCalculator.calculate(
                PressureMode.DIAMOND,
                referenceCenti = 133_300,
                measuredCenti = 100_000,
            ).result is PressureResult.OutOfRangeNegative,
        )
        assertTrue(
            PressureCalculator.calculate(
                PressureMode.DIAMOND,
                referenceCenti = 133_300,
                measuredCenti = 185_000,
            ).result is PressureResult.OutOfRangeHigh,
        )
    }

    @Test
    fun diamondHonorsRawZeroAndThreeHundredTenCalibrationEdges() {
        assertValidPressure(
            mode = PressureMode.DIAMOND,
            referenceCenti = 133_300,
            measuredCenti = 133_300,
            expectedPressureCenti = 0,
        )
        assertTrue(
            PressureCalculator.calculate(
                PressureMode.DIAMOND,
                referenceCenti = 133_300,
                measuredCenti = 133_299,
            ).result is PressureResult.OutOfRangeNegative,
        )

        val lastValid = PressureCalculator.calculate(
            PressureMode.DIAMOND,
            referenceCenti = 133_300,
            measuredCenti = 183_180,
        ).result
        assertTrue(lastValid is PressureResult.Valid)
        assertEquals(31_000, (lastValid as PressureResult.Valid).pressureCenti)
        assertTrue(lastValid.rawPressure <= 310.0)

        val firstHigh = PressureCalculator.calculate(
            PressureMode.DIAMOND,
            referenceCenti = 133_300,
            measuredCenti = 183_181,
        ).result
        assertTrue(firstHigh is PressureResult.OutOfRangeHigh)
        assertTrue(firstHigh.rawPressure > 310.0)
    }

    @Test
    fun oneHundredthStepsAndEveryInputLimitRemainExact() {
        val diamondStep = PressureCalculator.calculate(
            PressureMode.DIAMOND,
            referenceCenti = 133_300,
            measuredCenti = 133_301,
        )
        assertEquals(1, diamondStep.shiftCenti)

        val rubyStep = PressureCalculator.calculate(
            PressureMode.RUBY,
            referenceCenti = 69_424,
            measuredCenti = 69_425,
        )
        assertEquals(1, rubyStep.shiftCenti)

        listOf(
            Triple(PressureMode.DIAMOND, 100_000, 100_000),
            Triple(PressureMode.DIAMOND, 299_999, 299_999),
            Triple(PressureMode.RUBY, 60_000, 60_000),
            Triple(PressureMode.RUBY, 79_999, 79_999),
        ).forEach { (mode, reference, measured) ->
            val computation = PressureCalculator.calculate(mode, reference, measured)
            assertEquals(0, computation.shiftCenti)
            assertEquals(0, (computation.result as PressureResult.Valid).pressureCenti)
        }
    }

    @Test
    fun javascriptRoundingUsesTiesTowardPositiveInfinity() {
        assertEquals(2L, PressureCalculator.javaScriptRound(1.5))
        assertEquals(1L, PressureCalculator.javaScriptRound(1.49))
        assertEquals(-1L, PressureCalculator.javaScriptRound(-1.5))
        assertEquals(0L, PressureCalculator.javaScriptRound(-0.5))
    }

    @Test
    fun centiFormattingIsLocaleIndependentAndHandlesNegatives() {
        assertEquals("1333.00", formatCenti(133_300))
        assertEquals("0.01", formatCenti(1))
        assertEquals("-11.39", formatCenti(-1_139))
    }

    private fun assertValidPressure(
        mode: PressureMode,
        referenceCenti: Int,
        measuredCenti: Int,
        expectedPressureCenti: Int,
    ) {
        val result = PressureCalculator.calculate(
            mode,
            referenceCenti,
            measuredCenti,
        ).result as PressureResult.Valid
        assertEquals(expectedPressureCenti, result.pressureCenti)
    }
}
