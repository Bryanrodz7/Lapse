package dev.randyapps.lapse.widget

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.glance.GlanceTheme
import androidx.glance.material3.ColorProviders
import androidx.glance.unit.ColorProvider
import dev.randyapps.lapse.data.model.ItemStatus
import dev.randyapps.lapse.ui.theme.DarkStatusPalette
import dev.randyapps.lapse.ui.theme.LightStatusPalette
import dev.randyapps.lapse.ui.theme.NightBackground
import dev.randyapps.lapse.ui.theme.NightInk
import dev.randyapps.lapse.ui.theme.NightInkMuted
import dev.randyapps.lapse.ui.theme.PaperBackground
import dev.randyapps.lapse.ui.theme.PaperInk
import dev.randyapps.lapse.ui.theme.PaperInkMuted

/**
 * The app's palette, carried into the widget.
 *
 * Glance expresses day/night through a ColorProviders built from two Material3 schemes — the
 * only public way to do it, since both DayNightColorProvider and the resource-based
 * ColorProvider are @RestrictTo the Glance library. Passing colours explicitly also stops
 * GlanceTheme's default from repainting the widget in the system dynamic palette; the warm
 * neutrals and muted status hues are the app's identity and the widget keeps them.
 *
 * The five status colours ride on scheme slots because ColorProviders has no room for custom
 * ones. The mapping is arbitrary but fixed, and only [statusColor] reads it back.
 */
private val WidgetLightScheme = lightColorScheme(
    background = PaperBackground,
    onBackground = PaperInk,
    onSurfaceVariant = PaperInkMuted,
    primary = LightStatusPalette.active,
    secondary = LightStatusPalette.soon,
    tertiary = LightStatusPalette.urgent,
    error = LightStatusPalette.expiresToday,
    outline = LightStatusPalette.expired,
)

private val WidgetDarkScheme = darkColorScheme(
    background = NightBackground,
    onBackground = NightInk,
    onSurfaceVariant = NightInkMuted,
    primary = DarkStatusPalette.active,
    secondary = DarkStatusPalette.soon,
    tertiary = DarkStatusPalette.urgent,
    error = DarkStatusPalette.expiresToday,
    outline = DarkStatusPalette.expired,
)

/** Wraps widget content so every colour below resolves for the current light/dark mode. */
@Composable
fun LapseWidgetTheme(content: @Composable () -> Unit) {
    GlanceTheme(
        colors = ColorProviders(light = WidgetLightScheme, dark = WidgetDarkScheme),
        content = content,
    )
}

@Composable
fun statusColor(status: ItemStatus): ColorProvider = when (status) {
    ItemStatus.ACTIVE -> GlanceTheme.colors.primary
    ItemStatus.SOON -> GlanceTheme.colors.secondary
    ItemStatus.URGENT -> GlanceTheme.colors.tertiary
    ItemStatus.EXPIRES_TODAY -> GlanceTheme.colors.error
    ItemStatus.EXPIRED -> GlanceTheme.colors.outline
}
