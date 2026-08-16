package dev.randyapps.lapse.debug

import android.content.Context
import androidx.core.content.edit
import dev.randyapps.lapse.data.ItemRepository
import dev.randyapps.lapse.data.model.Category
import dev.randyapps.lapse.data.model.ItemDraft
import java.time.LocalDate

/**
 * Debug-build demo data, so the populated list can be looked at before the Add screen exists.
 *
 * This lives in src/debug and has a no-op twin in src/release, so the sample rows are not
 * compiled into a release build at all — a BuildConfig.DEBUG check would still ship the strings.
 */
object DemoSeed {

    private const val PREFS = "lapse_debug"
    private const val KEY_SEEDED = "demo_seeded"

    /**
     * Seeds once, ever. Guarded by a flag rather than only by "is the table empty", so that
     * deleting every item by hand doesn't resurrect the demo rows on next launch.
     */
    suspend fun seedIfNeeded(context: Context, repository: ItemRepository) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (prefs.getBoolean(KEY_SEEDED, false)) return

        if (repository.getAllItems().isEmpty()) {
            drafts(LocalDate.now()).forEach { repository.save(it) }
        }
        prefs.edit { putBoolean(KEY_SEEDED, true) }
    }

    /** Spans every status and all four sections, including the 0-day and expired edges. */
    private fun drafts(today: LocalDate): List<ItemDraft> = listOf(
        draft("Driver's License", Category.ID_AND_LICENSE, today, 0, listOf(30, 7, 1)),
        draft("Vehicle Inspection", Category.VEHICLE, today, 4, listOf(30, 7)),
        draft("Car Insurance", Category.INSURANCE, today, 23, listOf(30, 7)),
        draft("Passport", Category.ID_AND_LICENSE, today, 61, listOf(90, 30)),
        draft("Dentist Check-up", Category.HEALTH, today, 88, listOf(14)),
        draft("First Aid Certificate", Category.WORK_AND_CERTS, today, 240, listOf(60, 14)),
        draft("Boiler Service", Category.HOME, today, -12, listOf(30, 7)),
        draft("Gym Membership", Category.SUBSCRIPTION, today, -95, listOf(7)),
    )

    private fun draft(
        name: String,
        category: Category,
        today: LocalDate,
        days: Long,
        reminders: List<Int>,
    ) = ItemDraft(
        name = name,
        category = category,
        expiryDate = today.plusDays(days),
        reminderDaysBefore = reminders,
    )
}
