package com.iyes.dacpressuremanager.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.sp
import com.iyes.dacpressuremanager.R

private val PlatformTypography = Typography()
private val DacFontFamily = FontFamily(
    Font(R.font.dac_roboto),
)

private fun TextStyle.withDacFont(
    tracking: androidx.compose.ui.unit.TextUnit = 0.sp,
): TextStyle = copy(
    fontFamily = DacFontFamily,
    letterSpacing = tracking,
)

val DacTypography = Typography(
    displayLarge = PlatformTypography.displayLarge.withDacFont((-0.5).sp),
    displayMedium = PlatformTypography.displayMedium.withDacFont((-0.4).sp),
    displaySmall = PlatformTypography.displaySmall.withDacFont((-0.3).sp),
    headlineLarge = PlatformTypography.headlineLarge.withDacFont((-0.25).sp),
    headlineMedium = PlatformTypography.headlineMedium.withDacFont((-0.2).sp),
    headlineSmall = PlatformTypography.headlineSmall.withDacFont((-0.15).sp),
    titleLarge = PlatformTypography.titleLarge.withDacFont((-0.1).sp),
    titleMedium = PlatformTypography.titleMedium.withDacFont(),
    titleSmall = PlatformTypography.titleSmall.withDacFont(),
    bodyLarge = PlatformTypography.bodyLarge.withDacFont(),
    bodyMedium = PlatformTypography.bodyMedium.withDacFont(),
    bodySmall = PlatformTypography.bodySmall.withDacFont(0.1.sp),
    labelLarge = PlatformTypography.labelLarge.withDacFont(0.1.sp),
    labelMedium = PlatformTypography.labelMedium.withDacFont(0.15.sp),
    labelSmall = PlatformTypography.labelSmall.withDacFont(0.2.sp),
)
