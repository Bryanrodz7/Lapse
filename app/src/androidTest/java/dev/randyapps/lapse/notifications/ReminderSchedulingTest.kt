package dev.randyapps.lapse.notifications

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.work.Configuration
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.testing.SynchronousExecutor
import androidx.work.testing.WorkManagerTestInitHelper
import dev.randyapps.lapse.data.model.Category
import dev.randyapps.lapse.data.model.Item
import dev.randyapps.lapse.data.model.statusFor
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * Covers the scheduling contract: one job per offset, orphans cancelled on reschedule, and past
 * offsets skipped. These are the failures nobody notices until a reminder doesn't arrive.
 */
@RunWith(AndroidJUnit4::class)
class ReminderSchedulingTest {

    private val zone: ZoneId = ZoneId.of("UTC")
    private val today = LocalDate.of(2026, 8, 15)
    private val clock: Clock = Clock.fixed(
        today.atStartOfDay(zone).toInstant().plusSeconds(6 * 3600),
        zone,
    )

    private lateinit var workManager: WorkManager
    private lateinit var scheduler: WorkManagerReminderScheduler

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        WorkManagerTestInitHelper.initializeTestWorkManager(
            context,
            Configuration.Builder().setExecutor(SynchronousExecutor()).build(),
        )
        workManager = WorkManager.getInstance(context)
        scheduler = WorkManagerReminderScheduler({ workManager }, clock)
    }

    private fun item(
        id: Long,
        daysUntilExpiry: Long,
        reminders: List<Int>,
    ) = Item(
        id = id,
        name = "Passport",
        category = Category.ID_AND_LICENSE,
        expiryDate = today.plusDays(daysUntilExpiry),
        reminderDaysBefore = reminders,
        note = null,
        photoPath = null,
        createdAt = Instant.EPOCH,
        daysRemaining = daysUntilExpiry.toInt(),
        status = statusFor(daysUntilExpiry.toInt()),
    )

    private fun scheduledCountFor(itemId: Long): Int =
        workManager.getWorkInfosByTag(WorkManagerReminderScheduler.tagFor(itemId)).get()
            .count { it.state == WorkInfo.State.ENQUEUED }

    @Test
    fun oneJobIsScheduledPerReminderOffset() = runTest {
        scheduler.schedule(item(1, daysUntilExpiry = 400, reminders = listOf(90, 30, 7)))
        assertEquals(3, scheduledCountFor(1))
    }

    @Test
    fun offsetsAlreadyInThePastAreSkipped() = runTest {
        // Expiry in 10 days: the 90- and 30-day reminders are long gone, only 7 and 1 remain.
        scheduler.schedule(item(1, daysUntilExpiry = 10, reminders = listOf(90, 30, 7, 1)))
        assertEquals(2, scheduledCountFor(1))
    }

    @Test
    fun anItemWithNoRemindersSchedulesNothing() = runTest {
        scheduler.schedule(item(1, daysUntilExpiry = 400, reminders = emptyList()))
        assertEquals(0, scheduledCountFor(1))
    }

    @Test
    fun anAlreadyExpiredItemSchedulesNothing() = runTest {
        scheduler.schedule(item(1, daysUntilExpiry = -5, reminders = listOf(30, 7, 1)))
        assertEquals(0, scheduledCountFor(1))
    }

    @Test
    fun reschedulingWithFewerOffsetsCancelsTheOrphans() = runTest {
        scheduler.schedule(item(1, daysUntilExpiry = 400, reminders = listOf(90, 30, 7)))
        assertEquals(3, scheduledCountFor(1))

        // Switching an offset off must remove its job, not leave it firing.
        scheduler.schedule(item(1, daysUntilExpiry = 400, reminders = listOf(30)))
        assertEquals(1, scheduledCountFor(1))
    }

    @Test
    fun movingTheDateCloserDropsOffsetsThatNoLongerApply() = runTest {
        scheduler.schedule(item(1, daysUntilExpiry = 400, reminders = listOf(90, 30, 7)))
        assertEquals(3, scheduledCountFor(1))

        scheduler.schedule(item(1, daysUntilExpiry = 5, reminders = listOf(90, 30, 7)))
        assertEquals(0, scheduledCountFor(1))
    }

    @Test
    fun cancelRemovesEveryJobForThatItem() = runTest {
        scheduler.schedule(item(1, daysUntilExpiry = 400, reminders = listOf(90, 30, 7)))
        scheduler.cancel(1)
        assertEquals(0, scheduledCountFor(1))
    }

    @Test
    fun cancellingOneItemLeavesAnotherAlone() = runTest {
        scheduler.schedule(item(1, daysUntilExpiry = 400, reminders = listOf(90, 30)))
        scheduler.schedule(item(2, daysUntilExpiry = 400, reminders = listOf(90, 30)))

        scheduler.cancel(1)

        assertEquals(0, scheduledCountFor(1))
        assertEquals(2, scheduledCountFor(2))
    }

    @Test
    fun rescheduleAllRebuildsEveryItem() = runTest {
        scheduler.rescheduleAll(
            listOf(
                item(1, daysUntilExpiry = 400, reminders = listOf(90, 30)),
                item(2, daysUntilExpiry = 200, reminders = listOf(7)),
            )
        )
        assertEquals(2, scheduledCountFor(1))
        assertEquals(1, scheduledCountFor(2))
    }

    @Test
    fun theJobCarriesTheItemIdSoTheWorkerCanReadItBack() = runTest {
        scheduler.schedule(item(42, daysUntilExpiry = 400, reminders = listOf(30)))
        val info = workManager
            .getWorkInfosByTag(WorkManagerReminderScheduler.tagFor(42)).get()
            .single()
        assertTrue(info.state == WorkInfo.State.ENQUEUED)
    }
}
