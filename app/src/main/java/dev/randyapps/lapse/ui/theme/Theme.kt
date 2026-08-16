package dev.randyapps.lapse.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider

/**
 * Surfaces sit barely above the background on purpose. Separation in this app comes from
 * spacing and hairlines, not from elevation, so surface and background stay close enough that
 * Material's tonal elevation never draws a visible box.
 */
private val LightColors = lightColorScheme(
    primary = PaperInk,
    onPrimary = PaperBackground,
    secondary = PaperInkMuted,
    onSecondary = PaperBackground,
    background = PaperBackground,
    onBackground = PaperInk,
    surface = PaperBackground,
    onSurface = PaperInk,
    surfaceVariant = PaperSurface,
    onSurfaceVariant = PaperInkMuted,
    surfaceContainer = PaperSurface,
    surfaceContainerHigh = PaperSurface,
    outline = PaperInkMuted,
    outlineVariant = PaperHairline,
    error = LightStatusPalette.expiresToday,
    onError = PaperBackground,
    // Snackbars use the inverse trio. Left at Material defaults they render lavender with a
    // purple action, which is the one place stock colour would leak into the app.
    inverseSurface = PaperInk,
    inverseOnSurface = PaperBackground,
    inversePrimary = DarkStatusPalette.soon,
)

private val DarkColors = darkColorScheme(
    primary = NightInk,
    onPrimary = NightBackground,
    secondary = NightInkMuted,
    onSecondary = NightBackground,
    background = NightBackground,
    onBackground = NightInk,
    surface = NightBackground,
    onSurface = NightInk,
    surfaceVariant = NightSurface,
    onSurfaceVariant = NightInkMuted,
    surfaceContainer = NightSurface,
    surfaceContainerHigh = NightSurface,
    outline = NightInkMuted,
    outlineVariant = NightHairline,
    error = DarkStatusPalette.expiresToday,
    onError = NightBackground,
    inverseSurface = NightInk,
    inverseOnSurface = NightBackground,
    inversePrimary = LightStatusPalette.urgent,
)

/**
 * No dynamic color. The warm neutral base and the muted status hues are the app's identity;
 * letting the system repaint them in the user's wallpaper colors would undo the one thing that
 * makes it not look templated.
 */
@Composable
fun LapseTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColors else LightColors
    val statusPalette = if (darkTheme) DarkStatusPalette else LightStatusPalette

    CompositionLocalProvider(LocalStatusPalette provides statusPalette) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = LapseTypography,
            content = content,
        )
    }
}
