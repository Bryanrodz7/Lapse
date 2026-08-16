package dev.randyapps.lapse.data.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import dev.randyapps.lapse.data.model.DEFAULT_REMINDER_DAYS
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DataStoreSettingsStore @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) : SettingsStore {

    override val settings: Flow<LapseSettings> = dataStore.data.map { prefs ->
        LapseSettings(
            defaultReminderDays = prefs[KEY_REMINDERS]?.toDayList() ?: DEFAULT_REMINDER_DAYS,
            // An unknown or corrupt value falls back to SYSTEM rather than throwing; a settings
            // read must never be able to stop the app from starting.
            themeMode = prefs[KEY_THEME]?.let { name ->
                ThemeMode.entries.firstOrNull { it.name == name }
            } ?: ThemeMode.SYSTEM,
        )
    }

    override suspend fun setDefaultReminderDays(days: List<Int>) {
        dataStore.edit { it[KEY_REMINDERS] = days.sortedDescending().joinToString(",") }
    }

    override suspend fun setThemeMode(mode: ThemeMode) {
        dataStore.edit { it[KEY_THEME] = mode.name }
    }

    /** Same csv shape the entity converter uses; an empty string means "no reminders". */
    private fun String.toDayList(): List<Int> =
        if (isBlank()) emptyList() else split(",").mapNotNull { it.trim().toIntOrNull() }

    private companion object {
        val KEY_REMINDERS = stringPreferencesKey("default_reminder_days")
        val KEY_THEME = stringPreferencesKey("theme_mode")
    }
}
