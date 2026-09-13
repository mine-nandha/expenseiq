package com.mine.expenseiq.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import com.mine.expenseiq.R

// Space Grotesk is variable-weight (TTF); weights map through FontVariation (API 26+).
@OptIn(ExperimentalTextApi::class)
val SpaceGrotesk = FontFamily(
  Font(
    R.font.space_grotesk,
    weight = FontWeight.Normal,
    variationSettings = FontVariation.Settings(FontVariation.weight(400)),
  ),
  Font(
    R.font.space_grotesk,
    weight = FontWeight.Medium,
    variationSettings = FontVariation.Settings(FontVariation.weight(500)),
  ),
  Font(
    R.font.space_grotesk,
    weight = FontWeight.SemiBold,
    variationSettings = FontVariation.Settings(FontVariation.weight(600)),
  ),
  Font(
    R.font.space_grotesk,
    weight = FontWeight.Bold,
    variationSettings = FontVariation.Settings(FontVariation.weight(700)),
  ),
)

// The stamped-total figure on the add-transaction slip: big, tight, unmistakable.
val AmountDisplay = TextStyle(
  fontFamily = SpaceGrotesk,
  fontWeight = FontWeight.Bold,
  fontSize = 64.sp,
  lineHeight = 68.sp,
  letterSpacing = (-1).sp,
)

// One family product-wide: Space Grotesk carries both the ledger numerals and the UI text.
private fun sg(
  size: TextUnit,
  lineHeight: TextUnit,
  weight: FontWeight,
  letterSpacing: TextUnit = 0.sp,
) =
  TextStyle(
    fontFamily = SpaceGrotesk,
    fontWeight = weight,
    fontSize = size,
    lineHeight = lineHeight,
    letterSpacing = letterSpacing,
  )

val Typography =
  Typography(
    displayLarge = sg(57.sp, 64.sp, FontWeight.Normal),
    displayMedium = sg(45.sp, 52.sp, FontWeight.Normal),
    displaySmall = sg(36.sp, 44.sp, FontWeight.Normal),
    headlineLarge = sg(32.sp, 40.sp, FontWeight.Normal),
    headlineMedium = sg(28.sp, 36.sp, FontWeight.Normal),
    headlineSmall = sg(24.sp, 32.sp, FontWeight.Normal),
    titleLarge = sg(22.sp, 28.sp, FontWeight.Normal),
    titleMedium = sg(16.sp, 24.sp, FontWeight.Medium, 0.15.sp),
    titleSmall = sg(14.sp, 20.sp, FontWeight.Medium, 0.1.sp),
    bodyLarge = sg(16.sp, 24.sp, FontWeight.Normal, 0.5.sp),
    bodyMedium = sg(14.sp, 20.sp, FontWeight.Normal, 0.25.sp),
    bodySmall = sg(12.sp, 16.sp, FontWeight.Normal, 0.4.sp),
    labelLarge = sg(14.sp, 20.sp, FontWeight.Medium, 0.1.sp),
    labelMedium = sg(12.sp, 16.sp, FontWeight.Medium, 0.5.sp),
    labelSmall = sg(11.sp, 16.sp, FontWeight.Medium, 0.5.sp),
  )