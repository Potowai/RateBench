package com.example.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColorScheme =
  lightColorScheme(
    primary = Slate800,
    secondary = Slate700,
    tertiary = AmberRating,
    background = Slate50,
    surface = Color.White,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = Slate900,
    onSurface = Slate900,
  )

/**
 * RateBench impose le thème clair (fond blanc, texte noir) pour garantir le
 * contraste, quel que soit le mode sombre du système.
 */
@Composable
fun MyApplicationTheme(
  content: @Composable () -> Unit,
) {
  val view = LocalView.current
  SideEffect {
    val window = (view.context as Activity).window
    // Barres système transparentes (edge-to-edge) avec icônes sombres lisibles
    WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = true
  }

  MaterialTheme(colorScheme = LightColorScheme, typography = Typography, content = content)
}
