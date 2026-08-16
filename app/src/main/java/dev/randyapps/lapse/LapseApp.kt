package dev.randyapps.lapse

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import dagger.hilt.android.HiltAndroidApp
import dev.randyapps.lapse.data.ItemRepository
import dev.randyapps.lapse.debug.DemoSeed
import dev.randyapps.lapse.notifications.Notifications
import com.google.android.gms.ads.MobileAds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class LapseApp : Application(), Configuration.Provider {

    @Inject lateinit var repository: ItemRepository

    // WorkManager's default initialiser is removed in the manifest so workers can be
    // constructor-injected; this supplies the factory in its place.
    @Inject lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder().setWorkerFactory(workerFactory).build()

    // Outlives any one screen; used only for work that must not be tied to a ViewModel.
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        Notifications.ensureChannel(this)
        // No-op in release builds — see the two DemoSeed variants.
        appScope.launch { DemoSeed.seedIfNeeded(this@LapseApp, repository) }
        // Initialisation does disk and network I/O, so it must not run on the main thread.
        appScope.launch { MobileAds.initialize(this@LapseApp) }
    }
}
