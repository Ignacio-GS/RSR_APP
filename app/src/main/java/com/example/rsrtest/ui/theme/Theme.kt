package com.example.rsrtest.ui.theme

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

private val BrandDarkColorScheme = darkColorScheme(
    primary = PrimaryDark,
    onPrimary = OnPrimaryDark,
    primaryContainer = Color(0xFF003258),
    onPrimaryContainer = Color(0xFFD1E4FF),
    secondary = SecondaryDark,
    onSecondary = OnSecondaryDark,
    secondaryContainer = Color(0xFF003046),
    onSecondaryContainer = Color(0xFFD2E4FF),
    tertiary = TertiaryDark,
    onTertiary = OnTertiaryDark,
    tertiaryContainer = Color(0xFF561F00),
    onTertiaryContainer = Color(0xFFFFDDD6),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    background = BackgroundDark,
    onBackground = OnBackgroundDark,
    surface = SurfaceDark,
    onSurface = OnSurfaceDark,
    surfaceVariant = Color(0xFF43474E),
    onSurfaceVariant = Color(0xFFC3C7CF),
    outline = OutlineDark,
    outlineVariant = Color(0xFF43474E)
)

private val BrandLightColorScheme = lightColorScheme(
    primary = PrimaryLight,
    onPrimary = OnPrimaryLight,
    primaryContainer = Color(0xFFD1E4FF),
    onPrimaryContainer = Color(0xFF001C3B),
    secondary = SecondaryLight,
    onSecondary = OnSecondaryLight,
    secondaryContainer = Color(0xFFD2E4FF),
    onSecondaryContainer = Color(0xFF001A2E),
    tertiary = TertiaryLight,
    onTertiary = OnTertiaryLight,
    tertiaryContainer = Color(0xFFFFDDD6),
    onTertiaryContainer = Color(0xFF3A0A00),
    error = PepsiRed,
    onError = PepsiWhite,
    errorContainer = Color(0xFFFFDADA),
    onErrorContainer = Color(0xFF410E0B),
    background = BackgroundLight,
    onBackground = OnBackgroundLight,
    surface = SurfaceLight,
    onSurface = OnSurfaceLight,
    surfaceVariant = Color(0xFFDFE2EB),
    onSurfaceVariant = Color(0xFF43474E),
    outline = OutlineLight,
    outlineVariant = Color(0xFFC3C7CF)
)

@Composable
fun RSRTESTTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Mantener opción de dynamic color, pero OFF por coherencia de marca
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> BrandDarkColorScheme
        else -> BrandLightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
