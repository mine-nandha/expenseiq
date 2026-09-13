package com.mine.expenseiq.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
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

// Set of Material typography styles to start with
val Typography =
  Typography(
    bodyLarge =
      TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp,
      )
    /* Other default text styles to override
    titleLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.sp
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    )
    */
  )