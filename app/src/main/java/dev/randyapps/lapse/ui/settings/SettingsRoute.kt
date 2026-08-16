package dev.randyapps.lapse.ui.settings

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.randyapps.lapse.data.settings.LapseSettings
import dev.randyapps.lapse.data.settings.ThemeMode
import dev.randyapps.lapse.ui.theme.LapseTheme

@Composable
fun SettingsRoute(
    onClose: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val privacyOptionsRequired by viewModel.privacyOptionsRequired.collectAsStateWithLifecycle()
    val activity = LocalContext.current.findActivity()

    SettingsScreen(
        settings = settings,
        onToggleReminderDay = viewModel::onToggleReminderDay,
        onThemeModeChange = viewModel::onThemeModeChange,
        // Only offered when the consent framework requires it, which is what the form itself
        // tells the user to look for.
        onPrivacyOptions = if (privacyOptionsRequired && activity != null) {
            { viewModel.onShowPrivacyOptions(activity) }
        } else {
            null
        },
        onClose = onClose,
    )
}

@Composable
private fun PreviewSettings(dark: Boolean) {
    LapseTheme(darkTheme = dark) {
        SettingsScreen(
            settings = LapseSettings(
                defaultReminderDays = listOf(30, 7, 1),
                themeMode = ThemeMode.SYSTEM,
            ),
            onToggleReminderDay = {},
            onThemeModeChange = {},
            onPrivacyOptions = {},
            onClose = {},
        )
    }
}

@Preview(name = "Settings - light", showBackground = true, heightDp = 800)
@Composable
private fun SettingsLightPreview() = PreviewSettings(dark = false)

@Preview(
    name = "Settings - dark",
    showBackground = true,
    heightDp = 800,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun SettingsDarkPreview() = PreviewSettings(dark = true)

@Preview(name = "Settings - 200% font", showBackground = true, heightDp = 1400, fontScale = 2.0f)
@Composable
private fun SettingsLargeFontPreview() = PreviewSettings(dark = false)

/** Compose gives a ContextWrapper; UMP needs the Activity underneath it. */
private fun Context.findActivity(): Activity? {
    var current = this
    while (current is ContextWrapper) {
        if (current is Activity) return current
        current = current.baseContext
    }
    return null
}
