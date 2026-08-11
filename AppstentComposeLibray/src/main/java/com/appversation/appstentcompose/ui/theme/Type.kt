package com.appversation.appstentcompose.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private val DefaultFamily = FontFamily.Default

// Renderer compatibility scale for authored SwiftUI-style font names and legacy body text.
val LegacyTypography = androidx.compose.material.Typography(
    body1 = TextStyle(
        fontFamily = DefaultFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
    ),
)

// Preserve the previous Material 2 type scale while exposing it through Material 3 slots.
val Typography = Typography(
    displayLarge = TextStyle(
        fontFamily = DefaultFamily,
        fontWeight = FontWeight.Light,
        fontSize = 96.sp,
        letterSpacing = (-1.5).sp,
    ),
    displayMedium = TextStyle(
        fontFamily = DefaultFamily,
        fontWeight = FontWeight.Light,
        fontSize = 60.sp,
        letterSpacing = (-0.5).sp,
    ),
    displaySmall = TextStyle(
        fontFamily = DefaultFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 48.sp,
    ),
    headlineLarge = TextStyle(
        fontFamily = DefaultFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 34.sp,
        letterSpacing = 0.25.sp,
    ),
    headlineMedium = TextStyle(
        fontFamily = DefaultFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 24.sp,
    ),
    headlineSmall = TextStyle(
        fontFamily = DefaultFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 20.sp,
        letterSpacing = 0.15.sp,
    ),
    titleMedium = TextStyle(
        fontFamily = DefaultFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        letterSpacing = 0.15.sp,
    ),
    titleSmall = TextStyle(
        fontFamily = DefaultFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        letterSpacing = 0.1.sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = DefaultFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        letterSpacing = 0.5.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = DefaultFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        letterSpacing = 0.25.sp,
    ),
    bodySmall = TextStyle(
        fontFamily = DefaultFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        letterSpacing = 0.4.sp,
    ),
    labelLarge = TextStyle(
        fontFamily = DefaultFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        letterSpacing = 1.25.sp,
    ),
    labelMedium = TextStyle(
        fontFamily = DefaultFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        letterSpacing = 0.4.sp,
    ),
    labelSmall = TextStyle(
        fontFamily = DefaultFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 10.sp,
        letterSpacing = 1.5.sp,
    ),
)
