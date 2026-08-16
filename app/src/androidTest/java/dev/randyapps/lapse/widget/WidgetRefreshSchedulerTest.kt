package dev.randyapps.lapse.widget

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.work.Configuration
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.testing.SynchronousExecutor
import androidx.work.testing.WorkManagerTestInitHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Clock
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset

/**
 * The widget refreshes once a day, not on a poll. These assert the schedule rather than the
 * rendering, which is what keeps "do not poll frequently" honest.
 */
@RunWith(AndroidJUnit4::class)
class WidgetRefreshSchedulerTest {

    private val zone: ZoneId = ZoneOffset.UTC
    private lateinit var workManager: WorkManager

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        WorkManagerTestInitHelper.initializeTestWorkManager(
            context,
            Configuration.Builder().setExecutor(SynchronousExecutor()).build(),
        )
        workManager = WorkManager.getInstance(context)
        WidgetRefreshScheduler.cancelDailyRefresh(context)
    }

    private fun clockAt(hour: Int, minute: Int): Clock = Clock.fixed(
        LocalDateTime.of(2026, 8, 16, hour, minute).toInstant(ZoneOffset.UTC),
        zone,
    )

    private fun scheduledWork() =
        workManager.getWorkInfosForUniqueWork("widget-daily-refresh").get()

    @Test
    fun schedulesExactlyOneDailyJob() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        WidgetRefreshScheduler.scheduleDailyRefresh(context, clockAt(9, 0))

        val work = scheduledWork()
        assertEquals(1, work.size)
        assertEquals(WorkInfo.State.ENQUEUED, work.single().state)
    }

    @Test
    fun theFirstRunIsJustAfterTheNextMidnight() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        // 09:00 -> the next 00:05 is 15h05m away, not 30 minutes.
        WidgetRefreshScheduler.scheduleDailyRefresh(context, clockAt(9, 0))

        val delayMs = scheduledWork().single().initialDelayMillis
        val hours = delayMs / 3_600_000.0
        assertTrue("expected ~15.08h, was $hours", hours > 15.0 && hours < 15.2)
    }

    @Test
    fun rightAfterMidnightItWaitsUntilTomorrow() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        // 00:10 is past 00:05, so the next run is nearly a full day away.
        WidgetRefreshScheduler.scheduleDailyRefresh(context, clockAt(0, 10))

        val hours = scheduledWork().single().initialDelayMillis / 3_600_000.0
        assertTrue("expected ~23.9h, was $hours", hours > 23.8 && hours < 24.0)
    }

    @Test
    fun reschedulingDoesNotStackDuplicateJobs() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        repeat(3) { WidgetRefreshScheduler.scheduleDailyRefresh(context, clockAt(9, 0)) }
        assertEquals("KEEP policy should leave one job", 1, scheduledWork().size)
    }

    @Test
    fun removingTheLastWidgetCancelsTheJob() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        WidgetRefreshScheduler.scheduleDailyRefresh(context, clockAt(9, 0))
        WidgetRefreshScheduler.cancelDailyRefresh(context)

        assertTrue(scheduledWork().all { it.state == WorkInfo.State.CANCELLED })
    }
}
