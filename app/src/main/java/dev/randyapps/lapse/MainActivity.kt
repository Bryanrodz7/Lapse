package dev.randyapps.lapse

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import dagger.hilt.android.AndroidEntryPoint
import dev.randyapps.lapse.notifications.Notifications
import dev.randyapps.lapse.ui.nav.LapseNavHost
import dev.randyapps.lapse.ui.theme.LapseTheme

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

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
            LapseTheme {
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
