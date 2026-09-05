package com.example.presentation.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.R

// Main Header font: Bricolage Grotesque (Bundled local fonts)
val headerFontFamily = FontFamily(
    Font(R.font.bricolage_grotesque_bold, FontWeight.W700),
    Font(R.font.bricolage_grotesque_extrabold, FontWeight.W800),
    Font(R.font.bricolage_grotesque_bold, FontWeight.Normal)
)

// Subheader font: Syne (Bundled local fonts)
val subheaderFontFamily = FontFamily(
    Font(R.font.syne_medium, FontWeight.W500),
    Font(R.font.syne_semibold, FontWeight.W600),
    Font(R.font.syne_bold, FontWeight.W700),
    Font(R.font.syne_medium, FontWeight.Normal)
)

// Everything else: Plus Jakarta Sans (Bundled local fonts)
val bodyFontFamily = FontFamily(
    Font(R.font.plus_jakarta_sans_regular, FontWeight.W400),
    Font(R.font.plus_jakarta_sans_medium, FontWeight.W500),
    Font(R.font.plus_jakarta_sans_semibold, FontWeight.W600),
    Font(R.font.plus_jakarta_sans_bold, FontWeight.W700)
)

// Backward compatibility alias for any component referencing displayFontFamily
val displayFontFamily = headerFontFamily

val Typography = Typography(
    // Main Headers (Bricolage Grotesque)
    displayLarge = TextStyle(
        fontFamily = headerFontFamily,
        fontWeight = FontWeight.W800,
        fontSize = 32.sp,
        lineHeight = 38.sp,
        letterSpacing = (-0.5).sp
    ),
    displayMedium = TextStyle(
        fontFamily = headerFontFamily,
        fontWeight = FontWeight.W800,
        fontSize = 28.sp,
        lineHeight = 34.sp,
        letterSpacing = (-0.5).sp
    ),
    displaySmall = TextStyle(
        fontFamily = headerFontFamily,
        fontWeight = FontWeight.W700,
        fontSize = 24.sp,
        lineHeight = 30.sp,
        letterSpacing = (-0.25).sp
    ),
    headlineLarge = TextStyle(
        fontFamily = headerFontFamily,
        fontWeight = FontWeight.W800,
        fontSize = 24.sp,
        lineHeight = 30.sp,
        letterSpacing = (-0.25).sp
    ),
    headlineMedium = TextStyle(
        fontFamily = headerFontFamily,
        fontWeight = FontWeight.W700,
        fontSize = 20.sp,
        lineHeight = 26.sp,
        letterSpacing = 0.sp
    ),
    headlineSmall = TextStyle(
        fontFamily = headerFontFamily,
        fontWeight = FontWeight.W700,
        fontSize = 18.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.sp
    ),

    // Subheaders (Syne)
    titleLarge = TextStyle(
        fontFamily = subheaderFontFamily,
        fontWeight = FontWeight.W600,
        fontSize = 20.sp,
        lineHeight = 26.sp,
        letterSpacing = (-0.2).sp
    ),
    titleMedium = TextStyle(
        fontFamily = subheaderFontFamily,
        fontWeight = FontWeight.W500,
        fontSize = 15.sp,
        lineHeight = 20.sp,
        letterSpacing = (-0.1).sp
    ),
    titleSmall = TextStyle(
        fontFamily = subheaderFontFamily,
        fontWeight = FontWeight.W500,
        fontSize = 13.sp,
        lineHeight = 18.sp,
        letterSpacing = 0.sp
    ),

    // Everything else (Plus Jakarta Sans)
    bodyLarge = TextStyle(
        fontFamily = bodyFontFamily,
        fontWeight = FontWeight.W400,
        fontSize = 14.sp,
        lineHeight = 20.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = bodyFontFamily,
        fontWeight = FontWeight.W400,
        fontSize = 13.sp,
        lineHeight = 18.sp
    ),
    bodySmall = TextStyle(
        fontFamily = bodyFontFamily,
        fontWeight = FontWeight.W400,
        fontSize = 12.sp,
        lineHeight = 16.sp
    ),
    labelLarge = TextStyle(
        fontFamily = bodyFontFamily,
        fontWeight = FontWeight.W600,
        fontSize = 13.sp,
        lineHeight = 18.sp,
        letterSpacing = 0.1.sp
    ),
    labelMedium = TextStyle(
        fontFamily = bodyFontFamily,
        fontWeight = FontWeight.W500,
        fontSize = 11.sp,
        lineHeight = 15.sp
    ),
    labelSmall = TextStyle(
        fontFamily = bodyFontFamily,
        fontWeight = FontWeight.W600,
        fontSize = 10.sp,
        lineHeight = 14.sp,
        letterSpacing = 0.08.sp
    )
)
