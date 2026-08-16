package dev.randyapps.lapse.notifications

import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import dev.randyapps.lapse.data.ReminderScheduler
import dev.randyapps.lapse.data.model.Item
import java.time.Clock
import java.time.Duration
import java.time.LocalTime
import java.time.ZonedDateTime
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton

/**
 * Schedules one WorkManager job per reminder offset per item.
 *
 * Every item's jobs share a tag, so replacing an item's schedule is a cancel-by-tag followed by
 * fresh enqueues. That is what removes orphans when an offset is switched off or the date moves
 * closer, which a per-offset REPLACE alone would miss.
 */
@Singleton
class WorkManagerReminderScheduler @Inject constructor(
    /**
     * A Provider, not a WorkManager, and that matters.
     *
     * LapseApp field-injects the repository, which depends on this scheduler. Resolving a
     * WorkManager during that injection calls back into LapseApp.workManagerConfiguration,
     * which reads the not-yet-injected workerFactory and crashes the app on launch. Deferring
     * the lookup to first use breaks the cycle.
     */
    private val workManagerProvider: Provider<WorkManager>,
    private val clock: Clock,
) : ReminderScheduler {

    private val workManager: WorkManager get() = workManagerProvider.get()

    override suspend fun schedule(item: Item) {
        cancel(item.id)

        val now = ZonedDateTime.now(clock)
        item.reminderDaysBefore.forEach { daysBefore ->
            val fireAt = item.expiryDate
                .minusDays(daysBefore.toLong())
                .atTime(NOTIFY_AT)
                .atZone(clock.zone)

            // A reminder whose moment has already passed is simply not scheduled; firing it
            // late would tell the user about a deadline they have already missed.
            if (!fireAt.isAfter(now)) return@forEach

            val delay = Duration.between(now, fireAt)
            val request = OneTimeWorkRequestBuilder<ReminderWorker>()
                // The Duration overload is @RequiresApi(26) and minSdk is 24. Desugaring covers
                // java.time itself, but not WorkManager's API gate, so use the millis overload.
                .setInitialDelay(delay.toMillis(), TimeUnit.MILLISECONDS)
                .addTag(tagFor(item.id))
                .setInputData(
                    Data.Builder()
                        .putLong(ReminderWorker.KEY_ITEM_ID, item.id)
                        .putInt(ReminderWorker.KEY_DAYS_BEFORE, daysBefore)
                        .build()
                )
                .build()

            workManager.enqueueUniqueWork(
                uniqueNameFor(item.id, daysBefore),
                ExistingWorkPolicy.REPLACE,
                request,
            )
        }
    }

    override fun cancel(itemId: Long) {
        workManager.cancelAllWorkByTag(tagFor(itemId))
    }

    override suspend fun rescheduleAll(items: List<Item>) {
        items.forEach { schedule(it) }
    }

    companion object {
        /** Late enough to be awake, early enough to act on it the same day. */
        private val NOTIFY_AT: LocalTime = LocalTime.of(9, 0)

        fun tagFor(itemId: Long): String = "reminder-item-$itemId"

        fun uniqueNameFor(itemId: Long, daysBefore: Int): String = "reminder-$itemId-$daysBefore"
    }
}
