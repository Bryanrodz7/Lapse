package dev.randyapps.lapse.data.settings

import dev.randyapps.lapse.data.model.DEFAULT_REMINDER_DAYS
import kotlinx.coroutines.flow.Flow

enum class ThemeMode { SYSTEM, LIGHT, DARK }

data class LapseSettings(
    /** Pre-selected on the form for a new item; existing items keep whatever they were saved with. */
    val defaultReminderDays: List<Int> = DEFAULT_REMINDER_DAYS,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
)

/**
 * An interface so ViewModels stay plain JVM test subjects — the DataStore implementation needs a
 * Context and a real file.
 */
interface SettingsStore {
    val settings: Flow<LapseSettings>
    suspend fun setDefaultReminderDays(days: List<Int>)
    suspend fun setThemeMode(mode: ThemeMode)
}
