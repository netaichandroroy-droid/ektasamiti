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
    primary = DarkGreenPrimary,
    onPrimary = DarkGreenOnPrimary,
    primaryContainer = DarkGreenPrimaryContainer,
    onPrimaryContainer = DarkGreenOnPrimaryContainer,
    secondary = DarkTealSecondary,
    onSecondary = DarkTealOnSecondary,
    secondaryContainer = DarkTealSecondaryContainer,
    tertiary = DarkAmberTertiary,
    onTertiary = DarkAmberOnTertiary,
    background = DarkBackground,
    surface = DarkSurface,
    surfaceVariant = DarkSurfaceVariant,
    onBackground = Color(0xFFE1E5E1),
    onSurface = Color(0xFFE1E5E1),
  )

private val LightColorScheme =
  lightColorScheme(
    primary = GreenPrimary,
    onPrimary = GreenOnPrimary,
    primaryContainer = GreenPrimaryContainer,
    onPrimaryContainer = GreenOnPrimaryContainer,
    secondary = TealSecondary,
    onSecondary = TealOnSecondary,
    secondaryContainer = TealSecondaryContainer,
    onSecondaryContainer = TealOnSecondaryContainer,
    tertiary = AmberTertiary,
    onTertiary = AmberOnTertiary,
    tertiaryContainer = AmberTertiaryContainer,
    onTertiaryContainer = AmberOnTertiaryContainer,
    background = NeutralBackground,
    surface = NeutralSurface,
    surfaceVariant = NeutralSurfaceVariant,
    outline = NeutralOutline,
    outlineVariant = NeutralOutlineVariant,
    onBackground = NeutralOnBackground,
    onSurface = NeutralOnBackground,
    onSurfaceVariant = NeutralOnSurfaceVariant,
    error = Color(0xFFC62828),
    onError = Color.White,
  )

@Composable
fun EktaSamitiTheme(
  darkTheme: Boolean = false, // Always use high-contrast Grey Silver theme for clear readability
  dynamicColor: Boolean = false, // Use our signature brand green for identity
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

// Backward compatibility alias
@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) = EktaSamitiTheme(darkTheme, dynamicColor, content)
