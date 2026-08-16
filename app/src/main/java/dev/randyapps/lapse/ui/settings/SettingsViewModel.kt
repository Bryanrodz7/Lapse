package dev.randyapps.lapse.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.randyapps.lapse.data.settings.LapseSettings
import dev.randyapps.lapse.data.settings.SettingsStore
import dev.randyapps.lapse.data.settings.ThemeMode
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val store: SettingsStore,
) : ViewModel() {

    val settings: StateFlow<LapseSettings> = store.settings.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = LapseSettings(),
    )

    /**
     * Offsets are toggles, matching the edit form: one tap on, one tap off.
     *
     * Sorted here rather than left to the store, so ordering doesn't depend on which
     * implementation is behind the interface.
     */
    fun onToggleReminderDay(days: Int) {
        val current = settings.value.defaultReminderDays
        val next = if (days in current) current - days else current + days
        viewModelScope.launch { store.setDefaultReminderDays(next.sortedDescending()) }
    }

    fun onThemeModeChange(mode: ThemeMode) {
        viewModelScope.launch { store.setThemeMode(mode) }
    }
}
