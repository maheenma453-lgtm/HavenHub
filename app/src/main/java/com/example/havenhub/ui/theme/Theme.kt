package com.example.havenhub.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// ════════════════════════════════════════════════════════════════════
// DARK COLOR SCHEME — deep navy + logo gold
// ════════════════════════════════════════════════════════════════════
private val DarkColorScheme = darkColorScheme(
    // Primary — navy tones
    primary            = DarkGoldPrimary,       // gold as primary action color
    onPrimary          = DarkBg,
    primaryContainer   = DarkBgTertiary,
    onPrimaryContainer = DarkGoldLight,

    // Secondary — muted navy
    secondary          = DarkBgElevated,
    onSecondary        = DarkTextPrimary,
    secondaryContainer = DarkBgSecondary,
    onSecondaryContainer = DarkTextSecondary,

    // Tertiary — gold accent
    tertiary           = DarkGoldLight,
    onTertiary         = DarkBg,
    tertiaryContainer  = DarkGoldFaint,
    onTertiaryContainer = DarkGoldPrimary,

    // Backgrounds
    background         = DarkBg,
    onBackground       = DarkTextPrimary,

    // Surfaces
    surface            = DarkSurface,
    onSurface          = DarkTextPrimary,
    surfaceVariant     = DarkSurfaceVariant,
    onSurfaceVariant   = DarkTextSecondary,

    // Outline
    outline            = DarkBorder,
    outlineVariant     = DarkBorderGold,

    // Error
    error              = DarkError,
    onError            = DarkOnError,
    errorContainer     = Color(0xFF3A0A12),
    onErrorContainer   = DarkError,

    // Inverse
    inverseSurface     = DarkTextPrimary,
    inverseOnSurface   = DarkBg,
    inversePrimary     = PrimaryNavy,

    // Scrim
    scrim              = DarkBg.copy(alpha = 0.8f)
)

// ════════════════════════════════════════════════════════════════════
// LIGHT COLOR SCHEME — unchanged, do not modify
// ════════════════════════════════════════════════════════════════════
private val LightColorScheme = lightColorScheme(
    primary            = PrimaryNavy,
    onPrimary          = TextOnPrimary,
    primaryContainer   = PrimaryNavyLight,

    secondary          = SecondaryBlue,
    onSecondary        = OnSecondary,
    secondaryContainer = SecondaryBlueLight,

    tertiary           = GoldAccent,
    onTertiary         = OnAccentGold,
    tertiaryContainer  = GoldAccentLight,

    background         = BackgroundLight,
    onBackground       = TextPrimary,

    surface            = SurfaceWhite,
    onSurface          = TextPrimary,
    surfaceVariant     = SurfaceVariantLight,

    error              = ErrorRed,
    onError            = OnErrorRed
)

// ════════════════════════════════════════════════════════════════════
// THEME ENTRY POINT
// ════════════════════════════════════════════════════════════════════
@Composable
fun HavenHubTheme(
    darkTheme   : Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content     : @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else      -> LightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window

            // Status bar color — dark: deep navy, light: primary navy
            window.statusBarColor = if (darkTheme)
                DarkBg.toArgb()
            else
                PrimaryNavy.toArgb()

            // Status bar icons — dark: light icons, light: dark icons
            WindowCompat.getInsetsController(window, view)
                .isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography  = HavenTypography,
        content     = content
    )
}

























