package dev.randyapps.lapse.notifications

import android.app.NotificationManager
import android.service.notification.StatusBarNotification
import android.content.Context
import androidx.hilt.work.HiltWorkerFactory
import androidx.test.core.app.ApplicationProvider
import androidx.test.rule.GrantPermissionRule
import androidx.work.Configuration
import androidx.work.ListenableWorker
import androidx.work.testing.SynchronousExecutor
import androidx.work.testing.TestListenableWorkerBuilder
import androidx.work.testing.WorkManagerTestInitHelper
import androidx.work.workDataOf
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dev.randyapps.lapse.data.ItemRepository
import dev.randyapps.lapse.data.model.Category
import dev.randyapps.lapse.data.model.Item
import dev.randyapps.lapse.data.model.ItemDraft
import dev.randyapps.lapse.data.model.statusFor
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import javax.inject.Inject

/**
 * End-to-end for a single reminder: the worker resolves the item through Hilt, and the copy it
 * posts names the item and the real time left.
 */
@HiltAndroidTest
class ReminderWorkerTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val permissionRule: GrantPermissionRule =
        GrantPermissionRule.grant("android.permission.POST_NOTIFICATIONS")

    @Inject lateinit var workerFactory: HiltWorkerFactory
    @Inject lateinit var repository: ItemRepository

    private val context: Context get() = ApplicationProvider.getApplicationContext()

    private val notificationManager: NotificationManager
        get() = context.getSystemService(NotificationManager::class.java)

    @Before
    fun setUp() {
        WorkManagerTestInitHelper.initializeTestWorkManager(
            context,
            Configuration.Builder().setExecutor(SynchronousExecutor()).build(),
        )
        hiltRule.inject()
        notificationManager.cancelAll()
    }

    @After
    fun tearDown() {
        notificationManager.cancelAll()
        // Cancellation is async too; let it drain so the next test starts from zero.
        val deadline = System.currentTimeMillis() + 3_000
        while (notificationManager.activeNotifications.isNotEmpty() &&
            System.currentTimeMillis() < deadline
        ) {
            Thread.sleep(50)
        }
    }

    /**
     * NotificationManager.notify() is asynchronous, so reading activeNotifications straight
     * after doWork() is a race — it passes most runs and fails some. Wait for the expected
     * count instead of sampling once.
     */
    private fun awaitNotifications(expected: Int, timeoutMs: Long = 5_000): Array<StatusBarNotification> {
        val deadline = System.currentTimeMillis() + timeoutMs
        var posted = notificationManager.activeNotifications
        while (posted.size != expected && System.currentTimeMillis() < deadline) {
            Thread.sleep(50)
            posted = notificationManager.activeNotifications
        }
        assertEquals("expected $expected notification(s)", expected, posted.size)
        return posted
    }

    private fun item(name: String, days: Int) = Item(
        id = 1,
        name = name,
        category = Category.ID_AND_LICENSE,
        expiryDate = LocalDate.now().plusDays(days.toLong()),
        reminderDaysBefore = listOf(30),
        note = null,
        photoPath = null,
        createdAt = Instant.EPOCH,
        daysRemaining = days,
        status = statusFor(days),
    )

    // --- copy ---

    @Test
    fun copyNamesTheItemAndTheTimeLeft() {
        assertEquals(
            "Your Passport expires in 30 days",
            Notifications.titleFor(context, item("Passport", 30)),
        )
    }

    @Test
    fun copyIsSingularForOneDay() {
        assertEquals(
            "Your Passport expires in 1 day",
            Notifications.titleFor(context, item("Passport", 1)),
        )
    }

    @Test
    fun copySaysTodayRatherThanZeroDays() {
        assertEquals(
            "Your Driver's License expires today",
            Notifications.titleFor(context, item("Driver's License", 0)),
        )
    }

    @Test
    fun copyForAnExpiredItemReadsInThePast() {
        assertEquals(
            "Your Passport expired 5 days ago",
            Notifications.titleFor(context, item("Passport", -5)),
        )
    }

    // --- worker ---

    @Test
    fun theWorkerPostsANotificationForARealItem() = runTest {
        val id = repository.save(
            ItemDraft(
                name = "Vehicle Inspection",
                category = Category.VEHICLE,
                expiryDate = LocalDate.now().plusDays(30),
                reminderDaysBefore = listOf(30),
            )
        )
        try {
            val worker = TestListenableWorkerBuilder<ReminderWorker>(context)
                .setWorkerFactory(workerFactory)
                .setInputData(workDataOf(ReminderWorker.KEY_ITEM_ID to id))
                .build()

            val result = worker.doWork()
            assertTrue(result is ListenableWorker.Result.Success)

            val posted = awaitNotifications(1)
            assertTrue(
                "notification should name the item",
                posted.single().notification.extras
                    .getString("android.title")!!
                    .contains("Vehicle Inspection"),
            )
        } finally {
            repository.delete(id)
        }
    }

    @Test
    fun theNotificationBodyShowsAFormattedDateNotAnIsoString() = runTest {
        val id = repository.save(
            ItemDraft(
                name = "Car Insurance",
                category = Category.INSURANCE,
                expiryDate = LocalDate.of(2026, 9, 8),
                reminderDaysBefore = listOf(30),
            )
        )
        try {
            TestListenableWorkerBuilder<ReminderWorker>(context)
                .setWorkerFactory(workerFactory)
                .setInputData(workDataOf(ReminderWorker.KEY_ITEM_ID to id))
                .build()
                .doWork()

            val body = awaitNotifications(1).single()
                .notification.extras.getString("android.text")!!
            assertEquals("Expires 8 Sep 2026", body)
        } finally {
            repository.delete(id)
        }
    }

    @Test
    fun aWorkerForADeletedItemSucceedsQuietlyWithoutNotifying() = runTest {
        // Reminders outlive their items; firing a notification for something already deleted
        // would be worse than doing nothing.
        val worker = TestListenableWorkerBuilder<ReminderWorker>(context)
            .setWorkerFactory(workerFactory)
            .setInputData(workDataOf(ReminderWorker.KEY_ITEM_ID to 999_999L))
            .build()

        val result = worker.doWork()
        assertTrue(result is ListenableWorker.Result.Success)
        // Give it the same window a real post would have had, then confirm nothing arrived.
        Thread.sleep(500)
        assertEquals(0, notificationManager.activeNotifications.size)
    }

    @Test
    fun aWorkerWithNoItemIdFails() = runTest {
        val worker = TestListenableWorkerBuilder<ReminderWorker>(context)
            .setWorkerFactory(workerFactory)
            .build()

        assertTrue(worker.doWork() is ListenableWorker.Result.Failure)
    }
}
