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
    val resultBottomLift: Dp,
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
    val outerPadding = if (compact) 8.dp else 10.dp
    val sectionGap = when {
        short -> 4.dp
        compact -> 6.dp
        else -> 8.dp
    }
    val modeHeight = 48.dp
    val profileHeight = 48.dp
    val dashboardMaxHeight = when {
        short -> 280.dp
        compact -> 350.dp
        else -> 370.dp
    }
    val resultHeight = when {
        short -> 72.dp
        compact -> 104.dp
        else -> 112.dp
    }
    val resultBottomLift = if (maxWidth < 390.dp && maxHeight > maxWidth) {
        val fixedContentHeight =
            modeHeight +
                profileHeight +
                48.dp +
                1.dp +
                resultHeight +
                sectionGap * 5f
        val flexibleHeight =
            maxHeight - outerPadding * 2f - fixedContentHeight
        val extraSpace = flexibleHeight - dashboardMaxHeight
        (extraSpace / 3f).coerceIn(0.dp, 28.dp)
    } else {
        0.dp
    }

    return DacLayoutMetrics(
        compact = compact,
        short = short,
        outerPadding = outerPadding,
        sectionGap = sectionGap,
        modeHeight = modeHeight,
        profileHeight = profileHeight,
        dashboardMaxHeight = dashboardMaxHeight,
        recordsWidth = when {
            short -> 62.dp
            compact -> 68.dp
            else -> 76.dp
        },
        resultHeight = resultHeight,
        resultBottomLift = resultBottomLift,
    )
}
