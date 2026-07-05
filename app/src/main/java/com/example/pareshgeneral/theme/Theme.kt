package com.example.pareshgeneral.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFD4AF37),       // Gold
    secondary = Color(0xFFE57373),     // Soft Coral/Red
    tertiary = Color(0xFFF7E7C4),       // Cream
    background = Color(0xFF160305),     // Deep Dark Burgundy
    surface = Color(0xFF2A0A0E),        // Dark Burgundy Card/Sheet Surface
    onPrimary = Color(0xFF160305),      // Dark text on gold button
    onSecondary = Color.White,
    onTertiary = Color.Black,
    onBackground = Color(0xFFECE0E2),   // Rose White Text
    onSurface = Color(0xFFECE0E2),
    outline = Color(0xFF531E24)         // Rich Burgundy Border
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF4A0E17),        // Deep Burgundy
    secondary = Color(0xFFD4AF37),      // Gold
    tertiary = Color(0xFF7D5260),
    background = Color(0xFFFFFDFE),     // Soft Off-White Background
    surface = Color.White,
    onPrimary = Color.White,
    onSecondary = Color.Black,
    onTertiary = Color.White,
    onBackground = Color(0xFF1C1B1F),   // Dark Slate Text
    onSurface = Color(0xFF1C1B1F),
    outline = Color.LightGray
)

@Composable
fun PareshGeneralTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
