package com.mine.expenseiq.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme =
  darkColorScheme(
    primary = Color(0xFF818CF8), // indigo-400
    onPrimary = Color(0xFF0F172A),
    primaryContainer = Color(0xFF312E81), // indigo-900
    onPrimaryContainer = Color(0xFFE0E7FF),
    secondary = Color(0xFF94A3B8), // slate-400
    onSecondary = Color(0xFF0F172A),
    background = Color(0xFF0F172A), // slate-900
    onBackground = Color(0xFFF8FAFC),
    surface = Color(0xFF1E293B), // slate-800
    onSurface = Color(0xFFF8FAFC),
    surfaceVariant = Color(0xFF334155),
    onSurfaceVariant = Color(0xFFCBD5E1),
  )

private val LightColorScheme =
  lightColorScheme(
    primary = BentoPrimary,
    onPrimary = BentoWhite,
    primaryContainer = BentoPrimaryLight,
    onPrimaryContainer = BentoPrimaryDark,
    secondary = BentoSlate500,
    onSecondary = BentoWhite,
    tertiary = BentoAccentOrange,
    onTertiary = BentoWhite,
    background = BentoBg,
    onBackground = BentoSlate900,
    surface = BentoWhite,
    onSurface = BentoSlate900,
    surfaceVariant = BentoBg,
    onSurfaceVariant = BentoSlate500,
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  // Disable dynamic color to enforce our beautiful custom Bento theme design
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  val colorScheme =
    when {
      dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
        val context = LocalContext.current
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
      }

      darkTheme -> DarkColorScheme
      else -> LightColorScheme
    }

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
