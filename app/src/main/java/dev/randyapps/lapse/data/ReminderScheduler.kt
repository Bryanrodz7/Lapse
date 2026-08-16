package dev.randyapps.lapse.data

import dev.randyapps.lapse.data.model.Item

/**
 * Keeps scheduled reminders in step with stored items.
 *
 * Declared in the data layer so [ItemRepository] can call it on every mutation — that is what
 * guarantees a save, edit or delete can never leave stale reminders behind. The Android/
 * WorkManager implementation lives in the notifications package, and tests substitute a fake.
 */
interface ReminderScheduler {

    /** Replaces every reminder for this item, cancelling any that no longer apply. */
    suspend fun schedule(item: Item)

    /** Removes all reminders for an item, used on delete. */
    fun cancel(itemId: Long)

    /** Rebuilds the whole schedule, used after a reboot clears WorkManager's alarms. */
    suspend fun rescheduleAll(items: List<Item>)
}

/** Used by tests and by any build that has no scheduling side effects. */
object NoOpReminderScheduler : ReminderScheduler {
    override suspend fun schedule(item: Item) = Unit
    override fun cancel(itemId: Long) = Unit
    override suspend fun rescheduleAll(items: List<Item>) = Unit
}

/**
 * Told whenever stored items change, so surfaces outside the app (the home-screen widget) can
 * redraw. An interface for the same reason as the scheduler: the implementation needs a Context.
 */
interface ItemChangeNotifier {
    suspend fun onItemsChanged()
}

object NoOpItemChangeNotifier : ItemChangeNotifier {
    override suspend fun onItemsChanged() = Unit
}
