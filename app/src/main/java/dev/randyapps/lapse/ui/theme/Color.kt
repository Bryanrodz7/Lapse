package dev.randyapps.lapse.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import dev.randyapps.lapse.data.model.ItemStatus

// Warm neutrals. Nothing here is a pure grey: every value carries a little yellow/red so the
// app reads as paper and ink rather than as a system dialog.

// Light — a paper tone, deliberately not #FFFFFF.
val PaperBackground = Color(0xFFFAF7F2)
val PaperSurface = Color(0xFFF4F0E8)
val PaperInk = Color(0xFF1E1A15)
val PaperInkMuted = Color(0xFF6E6559)
val PaperHairline = Color(0xFFE4DED2)

// Dark — a warm near-black, not pure black and not blue-grey.
val NightBackground = Color(0xFF14120F)
val NightSurface = Color(0xFF1D1A16)
val NightInk = Color(0xFFF1ECE3)
val NightInkMuted = Color(0xFF9E9487)
val NightHairline = Color(0xFF2B2620)

/**
 * The only saturated colors in the app. Muted and earthy on purpose — these carry meaning, so
 * they must not have to compete with decorative color anywhere else.
 */
@Immutable
data class StatusPalette(
    val active: Color,
    val soon: Color,
    val urgent: Color,
    val expiresToday: Color,
    val expired: Color,
) {
    fun colorFor(status: ItemStatus): Color = when (status) {
        ItemStatus.ACTIVE -> active
        ItemStatus.SOON -> soon
        ItemStatus.URGENT -> urgent
        ItemStatus.EXPIRES_TODAY -> expiresToday
        ItemStatus.EXPIRED -> expired
    }
}

val LightStatusPalette = StatusPalette(
    active = Color(0xFF55705A),      // muted sage
    soon = Color(0xFFA9762B),        // warm amber
    urgent = Color(0xFFB85529),      // burnt orange
    expiresToday = Color(0xFF97291B), // deepest red: today is the last day
    expired = Color(0xFF8B7B74),     // desaturated grey-red, deliberately quiet
)

// Hand-tuned rather than inverted: each hue is lifted in lightness and pulled back in
// saturation so it sits on a near-black ground without glowing.
val DarkStatusPalette = StatusPalette(
    active = Color(0xFF8CAA8E),
    soon = Color(0xFFD8A65A),
    urgent = Color(0xFFE08154),
    expiresToday = Color(0xFFD95C4C),
    expired = Color(0xFF8A7C75),
)

val LocalStatusPalette = staticCompositionLocalOf { LightStatusPalette }
