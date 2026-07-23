package com.iyes.dacpressuremanager.ui

import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Immutable
internal data class DacLayoutMetrics(
    val compact: Boolean,
    val short: Boolean,
    val outerPadding: Dp,
    val sectionGap: Dp,
    val modeHeight: Dp,
    val profileHeight: Dp,
    val dashboardMaxHeight: Dp,
    val recordsWidth: Dp,
    val resultHeight: Dp,
)

internal fun dacLayoutMetrics(
    maxWidth: Dp,
    maxHeight: Dp,
    fontScale: Float,
): DacLayoutMetrics {
    val short = maxHeight < 540.dp
    val compact = short ||
        maxHeight < 700.dp ||
        maxWidth < 390.dp ||
        fontScale > 1.2f

    return DacLayoutMetrics(
        compact = compact,
        short = short,
        outerPadding = if (compact) 8.dp else 10.dp,
        sectionGap = when {
            short -> 4.dp
            compact -> 6.dp
            else -> 8.dp
        },
        modeHeight = 48.dp,
        profileHeight = 48.dp,
        dashboardMaxHeight = when {
            short -> 280.dp
            compact -> 350.dp
            else -> 370.dp
        },
        recordsWidth = when {
            short -> 62.dp
            compact -> 68.dp
            else -> 76.dp
        },
        resultHeight = when {
            short -> 72.dp
            compact -> 104.dp
            else -> 112.dp
        },
    )
}
