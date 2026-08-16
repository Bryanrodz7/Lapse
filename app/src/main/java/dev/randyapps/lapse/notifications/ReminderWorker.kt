package dev.randyapps.lapse.notifications

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import dev.randyapps.lapse.data.ItemRepository

/**
 * Fires one reminder. One worker per offset per item, so "30 days" and "7 days" are independent
 * and cancelling one never disturbs the other.
 */
@HiltWorker
class ReminderWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val repository: ItemRepository,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val itemId = inputData.getLong(KEY_ITEM_ID, -1L)
        if (itemId <= 0L) return Result.failure()

        // Read the item now rather than trusting anything captured at schedule time: the date
        // may have moved, or the item may be gone entirely.
        val item = repository.getItem(itemId) ?: return Result.success()

        Notifications.post(applicationContext, item)
        return Result.success()
    }

    companion object {
        const val KEY_ITEM_ID = "itemId"
        const val KEY_DAYS_BEFORE = "daysBefore"
    }
}
