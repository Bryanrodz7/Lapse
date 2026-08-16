package dev.randyapps.lapse.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dagger.hilt.android.AndroidEntryPoint
import dev.randyapps.lapse.data.ItemRepository
import dev.randyapps.lapse.widget.WidgetRefreshScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Rebuilds the reminder schedule after a reboot.
 *
 * A device restart clears pending alarms, so without this every reminder would silently stop
 * firing — the failure mode is invisible until the day someone misses a renewal.
 */
@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {

    @Inject lateinit var repository: ItemRepository

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED &&
            intent.action != Intent.ACTION_MY_PACKAGE_REPLACED
        ) {
            return
        }

        // goAsync keeps the receiver alive past onReceive; the work is a handful of DB reads
        // and enqueues, well inside the allowance.
        val pending = goAsync()
        scope.launch {
            try {
                repository.rescheduleAllReminders()
                // A reboot also clears the widget's periodic work and leaves it showing a day
                // count from before the restart.
                WidgetRefreshScheduler.scheduleDailyRefresh(context)
                WidgetRefreshScheduler.notifyDataChanged(context)
            } finally {
                pending.finish()
            }
        }
    }
}
