package com.iyes.dacpressuremanager.domain

import kotlin.math.floor
import kotlin.math.pow

sealed interface PressureResult {
    val rawPressure: Double

    data class Valid(
        override val rawPressure: Double,
        val pressureCenti: Int,
    ) : PressureResult

    data class OutOfRangeNegative(
        override val rawPressure: Double,
    ) : PressureResult

    data class OutOfRangeHigh(
        override val rawPressure: Double,
    ) : PressureResult
}

data class PressureComputation(
    val shiftCenti: Int,
    val result: PressureResult,
)

object PressureCalculator {
    private const val DIAMOND_K0 = 547.0
    private const val DIAMOND_K_PRIME = 3.75
    private const val DIAMOND_MAX_GPA = 310.0
    private const val RUBY_A = 1904.0
    private const val RUBY_B = 7.665

    fun calculate(
        mode: PressureMode,
        referenceCenti: Int,
        measuredCenti: Int,
    ): PressureComputation {
        val shiftCenti = measuredCenti - referenceCenti
        val reference = referenceCenti / 100.0
        val measured = measuredCenti / 100.0
        val rawPressure = when {
            referenceCenti == 0 -> 0.0
            mode == PressureMode.DIAMOND -> {
                val delta = shiftCenti / 100.0
                val ratio = delta / reference
                val term = 1.0 + 0.5 * (DIAMOND_K_PRIME - 1.0) * ratio
                DIAMOND_K0 * ratio * term
            }
            else -> {
                val ratio = measured / reference
                (RUBY_A / RUBY_B) * (ratio.pow(RUBY_B) - 1.0)
            }
        }

        val result = when {
            mode == PressureMode.DIAMOND && rawPressure < 0.0 ->
                PressureResult.OutOfRangeNegative(rawPressure)
            mode == PressureMode.DIAMOND && rawPressure > DIAMOND_MAX_GPA ->
                PressureResult.OutOfRangeHigh(rawPressure)
            else -> PressureResult.Valid(
                rawPressure = rawPressure,
                pressureCenti = javaScriptRound(rawPressure * 100.0).toInt(),
            )
        }

        return PressureComputation(
            shiftCenti = shiftCenti,
            result = result,
        )
    }

    /**
     * Mirrors JavaScript Math.round: nearest integer with exact ties toward +∞.
     */
    internal fun javaScriptRound(value: Double): Long = floor(value + 0.5).toLong()
}

fun formatCenti(value: Int): String {
    val absolute = kotlin.math.abs(value.toLong())
    val sign = if (value < 0) "-" else ""
    return "$sign${absolute / 100}.${(absolute % 100).toString().padStart(2, '0')}"
}

