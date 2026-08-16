package dev.randyapps.lapse.widget

import android.content.Context
import androidx.glance.appwidget.updateAll
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.randyapps.lapse.data.ItemChangeNotifier
import javax.inject.Inject
import javax.inject.Singleton
import java.time.Clock
import java.time.Duration
import java.time.LocalTime
import java.time.ZonedDateTime
import java.util.concurrent.TimeUnit

/**
 * Keeps the widget's day count honest.
 *
 * Item changes push an update immediately (see [notifyDataChanged]); this covers the other case,
 * where nothing changes but midnight passes and "4 days left" silently becomes wrong. One run a
 * day, just after midnight — not polling.
 */
object WidgetRefreshScheduler {

    private const val WORK_NAME = "widget-daily-refresh"
    private val REFRESH_AT: LocalTime = LocalTime.of(0, 5)

    fun scheduleDailyRefresh(context: Context, clock: Clock = Clock.systemDefaultZone()) {
        val now = ZonedDateTime.now(clock)
        val nextRun = now.toLocalDate().atTime(REFRESH_AT).atZone(clock.zone).let {
            if (it.isAfter(now)) it else it.plusDays(1)
        }
        val initialDelay = Duration.between(now, nextRun)

        val request = PeriodicWorkRequestBuilder<DailyWidgetRefreshWorker>(1, TimeUnit.DAYS)
            // Millis overload: the Duration overloads are @RequiresApi(26) and minSdk is 24.
            .setInitialDelay(initialDelay.toMillis(), TimeUnit.MILLISECONDS)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WORK_NAME,
            // KEEP, so re-delivered widget updates don't reset the schedule every time.
            ExistingPeriodicWorkPolicy.KEEP,
            request,
        )
    }

    fun cancelDailyRefresh(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
    }

    /** Pushes a redraw after any item change. Safe to call when no widget is placed. */
    suspend fun notifyDataChanged(context: Context) {
        NextExpiryWidget().updateAll(context)
    }
}

/** Redraws the widget whenever items change. */
@Singleton
class WidgetItemChangeNotifier @Inject constructor(
    @ApplicationContext private val context: Context,
) : ItemChangeNotifier {
    override suspend fun onItemsChanged() {
        WidgetRefreshScheduler.notifyDataChanged(context)
    }
}

@HiltWorker
class DailyWidgetRefreshWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        NextExpiryWidget().updateAll(applicationContext)
        return Result.success()
    }
}
