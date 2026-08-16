package dev.randyapps.lapse.ui.settings

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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

    SettingsScreen(
        settings = settings,
        onToggleReminderDay = viewModel::onToggleReminderDay,
        onThemeModeChange = viewModel::onThemeModeChange,
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
