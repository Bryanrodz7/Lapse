package dev.randyapps.lapse

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import javax.inject.Inject
import dagger.hilt.android.AndroidEntryPoint
import dev.randyapps.lapse.data.settings.ThemeMode
import dev.randyapps.lapse.ads.ConsentManager
import dev.randyapps.lapse.notifications.Notifications
import dev.randyapps.lapse.ui.AppViewModel
import dev.randyapps.lapse.ui.nav.LapseNavHost
import dev.randyapps.lapse.ui.theme.LapseTheme

/**
 * androidx's own defaults for [enableEdgeToEdge]'s navigation bar, which it keeps private.
 * They only apply below API 29, where a three-button navigation bar cannot be fully transparent
 * and needs a scrim behind it — which minSdk 24 still has to support.
 */
private val NavigationBarLightScrim = Color.argb(0xe6, 0xFF, 0xFF, 0xFF)
private val NavigationBarDarkScrim = Color.argb(0x80, 0x1b, 0x1b, 0x1b)

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val appViewModel: AppViewModel by viewModels()

    // UMP needs an Activity to present the consent form.
    @Inject lateinit var consentManager: ConsentManager

    /**
     * Item id carried by a tapped notification. Held as state rather than read once, so a tap
     * while the app is already open (singleTop -> onNewIntent) still navigates.
     */
    private var pendingItemId by mutableStateOf<Long?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Lays the window out edge to edge before the first frame. The bar icon colours it picks
        // here are provisional — they follow the system, not this app's theme — and are corrected
        // in setContent below, which is the only place the resolved theme is known.
        enableEdgeToEdge()
        pendingItemId = intent.itemIdExtra()
        // Resolve consent before any ad request; the Ads SDK is initialised inside.
        consentManager.gatherConsent(this)
        setContent {
            val themeMode by appViewModel.themeMode.collectAsStateWithLifecycle()
            // The explicit choices win over the system; SYSTEM defers to it.
            val dark = when (themeMode) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }
            // The status and navigation bar icons have to follow *this app's* resolved theme,
            // not the system's. enableEdgeToEdge() with no arguments decides light-or-dark icons
            // from the system uiMode, so ThemeMode.DARK on a light system left dark icons on the
            // dark background — measured at zero contrast, the clock and battery invisible rather
            // than merely dim. Re-applying keyed on `dark` also makes the bars follow a theme
            // change made in Settings, which a one-shot call in onCreate cannot do.
            DisposableEffect(dark) {
                enableEdgeToEdge(
                    statusBarStyle = SystemBarStyle.auto(
                        Color.TRANSPARENT,
                        Color.TRANSPARENT,
                    ) { dark },
                    navigationBarStyle = SystemBarStyle.auto(
                        NavigationBarLightScrim,
                        NavigationBarDarkScrim,
                    ) { dark },
                )
                onDispose {}
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
