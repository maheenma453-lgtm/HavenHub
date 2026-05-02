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
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// ─── Dark Color Scheme ────────────────────────────────────────────
private val DarkColorScheme = darkColorScheme(
    primary            = PrimaryNavyLight,       // lighter navy on dark
    onPrimary          = BackgroundDark,
    primaryContainer   = PrimaryNavyDark,

    secondary          = SecondaryBlue,
    onSecondary        = BackgroundDark,
    secondaryContainer = SecondaryBlueDark,

    tertiary           = GoldAccentLight,        // lighter gold on dark
    onTertiary         = BackgroundDark,
    tertiaryContainer  = GoldAccentDark,

    background         = BackgroundDark,
    onBackground       = TextPrimaryDark,

    surface            = SurfaceDark,
    onSurface          = TextPrimaryDark,
    surfaceVariant     = SurfaceVariantDark,

    error              = ErrorRedDark,
    onError            = OnErrorRedDark
)

// ─── Light Color Scheme ───────────────────────────────────────────
private val LightColorScheme = lightColorScheme(
    primary            = PrimaryNavy,            // #1B2B5B — HAVEN navy
    onPrimary          = TextOnPrimary,
    primaryContainer   = PrimaryNavyLight,

    secondary          = SecondaryBlue,
    onSecondary        = OnSecondary,
    secondaryContainer = SecondaryBlueLight,

    tertiary           = GoldAccent,             // #C9A84C — HUB gold
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

// ─── Theme Entry Point ────────────────────────────────────────────
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
            // Status bar — HavenHub navy
            window.statusBarColor = PrimaryNavy.toArgb()
            WindowCompat.getInsetsController(window, view)
                .isAppearanceLightStatusBars = false
        }
    }


    MaterialTheme(
      colorScheme = colorScheme,
      typography  = HavenTypography,
        content     = content
  )
}