package dev.randyapps.lapse

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import dev.randyapps.lapse.data.settings.ThemeMode
import dev.randyapps.lapse.notifications.Notifications
import dev.randyapps.lapse.ui.AppViewModel
import dev.randyapps.lapse.ui.nav.LapseNavHost
import dev.randyapps.lapse.ui.theme.LapseTheme

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val appViewModel: AppViewModel by viewModels()

    /**
     * Item id carried by a tapped notification. Held as state rather than read once, so a tap
     * while the app is already open (singleTop -> onNewIntent) still navigates.
     */
    private var pendingItemId by mutableStateOf<Long?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        pendingItemId = intent.itemIdExtra()
        setContent {
            val themeMode by appViewModel.themeMode.collectAsStateWithLifecycle()
            // The explicit choices win over the system; SYSTEM defers to it.
            val dark = when (themeMode) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }
            LapseTheme(darkTheme = dark) {
                LapseNavHost(
                    deepLinkItemId = pendingItemId,
                    onDeepLinkHandled = { pendingItemId = null },
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        pendingItemId = intent.itemIdExtra()
    }

    private fun Intent.itemIdExtra(): Long? =
        getLongExtra(Notifications.EXTRA_ITEM_ID, -1L).takeIf { it > 0L }
}
