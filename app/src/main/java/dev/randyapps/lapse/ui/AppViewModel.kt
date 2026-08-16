package dev.randyapps.lapse.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.randyapps.lapse.data.settings.SettingsStore
import dev.randyapps.lapse.data.settings.ThemeMode
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/**
 * Holds only what the whole app needs: the chosen theme. Kept separate from SettingsViewModel so
 * the theme survives leaving the settings screen.
 */
@HiltViewModel
class AppViewModel @Inject constructor(
    store: SettingsStore,
) : ViewModel() {

    val themeMode: StateFlow<ThemeMode> = store.settings
        .map { it.themeMode }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = ThemeMode.SYSTEM,
        )
}
