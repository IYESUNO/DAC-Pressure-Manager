package com.iyes.dacpressuremanager.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily

private val PlatformTypography = Typography()

private fun TextStyle.withDacFont(): TextStyle =
    copy(fontFamily = FontFamily.SansSerif)

val DacTypography = Typography(
    displayLarge = PlatformTypography.displayLarge.withDacFont(),
    displayMedium = PlatformTypography.displayMedium.withDacFont(),
    displaySmall = PlatformTypography.displaySmall.withDacFont(),
    headlineLarge = PlatformTypography.headlineLarge.withDacFont(),
    headlineMedium = PlatformTypography.headlineMedium.withDacFont(),
    headlineSmall = PlatformTypography.headlineSmall.withDacFont(),
    titleLarge = PlatformTypography.titleLarge.withDacFont(),
    titleMedium = PlatformTypography.titleMedium.withDacFont(),
    titleSmall = PlatformTypography.titleSmall.withDacFont(),
    bodyLarge = PlatformTypography.bodyLarge.withDacFont(),
    bodyMedium = PlatformTypography.bodyMedium.withDacFont(),
    bodySmall = PlatformTypography.bodySmall.withDacFont(),
    labelLarge = PlatformTypography.labelLarge.withDacFont(),
    labelMedium = PlatformTypography.labelMedium.withDacFont(),
    labelSmall = PlatformTypography.labelSmall.withDacFont(),
)
