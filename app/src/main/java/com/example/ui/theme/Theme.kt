package com.example.ui.theme

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
    primary = CaneGreenPrimary, // 0xFF2E8B57 Forest Cane Green
    onPrimary = Color.White,
    primaryContainer = CaneDarkGreenContainer, // 0xFF133C26 Deep Dark Green
    onPrimaryContainer = Color(0xFFD4F2E2),
    secondary = CaneBlueSecondary, // 0xFF3273A6 Vibrant Deep Blue
    onSecondary = Color.White,
    secondaryContainer = CaneDarkBlueContainer, // 0xFF0F2744 Deep Dark Blue
    onSecondaryContainer = TextOnDarkBlue, // 0xFFD4E7FA
    tertiary = CaneTealTertiary,
    tertiaryContainer = Color(0xFF0F2C33),
    onTertiaryContainer = Color(0xFFC8F0F7),
    background = DarkGreenBg, // 0xFF081810 Deep Dark Green background
    surface = DarkGreenSurface, // 0xFF0D2218 Rich Dark Green surface
    surfaceVariant = DarkBlueSurface, // 0xFF0D1E33 Rich Dark Blue card surface
    onBackground = TextOnDarkPrimary, // 0xFFF2F7F4 Crisp high contrast
    onSurface = Color.White,
    onSurfaceVariant = TextOnDarkSecondary, // 0xFFB5CCBF
  )

private val LightColorScheme =
  darkColorScheme(
    primary = CaneGreenPrimary,
    onPrimary = Color.White,
    primaryContainer = CaneDarkGreenContainer,
    onPrimaryContainer = Color(0xFFD4F2E2),
    secondary = CaneBlueSecondary,
    onSecondary = Color.White,
    secondaryContainer = CaneDarkBlueContainer,
    onSecondaryContainer = TextOnDarkBlue,
    tertiary = CaneTealTertiary,
    tertiaryContainer = Color(0xFF0F2C33),
    onTertiaryContainer = Color(0xFFC8F0F7),
    background = DarkGreenBg,
    surface = DarkGreenSurface,
    surfaceVariant = DarkBlueSurface,
    onBackground = TextOnDarkPrimary,
    onSurface = Color.White,
    onSurfaceVariant = TextOnDarkSecondary,
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  // Disable dynamic color so our branded agricultural identity remains consistent
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
