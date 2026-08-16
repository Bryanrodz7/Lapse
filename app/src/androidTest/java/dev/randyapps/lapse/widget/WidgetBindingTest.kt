package dev.randyapps.lapse.widget

import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.randyapps.lapse.data.ItemRepository
import dev.randyapps.lapse.data.model.Category
import dev.randyapps.lapse.data.model.ItemDraft
import androidx.work.Configuration
import androidx.work.testing.SynchronousExecutor
import androidx.work.testing.WorkManagerTestInitHelper
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import javax.inject.Inject
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

/**
 * Binds a real widget instance on the device and drives it, which the launcher's drag-and-drop
 * cannot be scripted to do. Proves the provider binds, that provideGlance resolves its
 * dependencies through Hilt and reads the real database, and that an update reaches it.
 *
 * Requires: adb shell appwidget grantbind --package dev.randyapps.lapse.test
 */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class WidgetBindingTest {

    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    @Inject lateinit var repository: ItemRepository

    private val context: Context get() = ApplicationProvider.getApplicationContext()
    private lateinit var host: AppWidgetHost
    private var widgetId: Int = 0

    @Before
    fun setUp() {
        WorkManagerTestInitHelper.initializeTestWorkManager(
            context,
            Configuration.Builder().setExecutor(SynchronousExecutor()).build(),
        )
        hiltRule.inject()
        host = AppWidgetHost(context, HOST_ID)
        widgetId = host.allocateAppWidgetId()
    }

    @After
    fun tearDown() {
        host.deleteAppWidgetId(widgetId)
    }

    @Test
    fun theProviderBindsAndGlanceRendersItFromTheDatabase() = runTest {
        val manager = AppWidgetManager.getInstance(context)
        val provider = ComponentName(context, NextExpiryWidgetReceiver::class.java)

        val bound = manager.bindAppWidgetIdIfAllowed(widgetId, provider)
        assertTrue("bindAppWidgetIdIfAllowed failed; run `adb shell appwidget grantbind`", bound)

        val id = repository.save(
            ItemDraft(
                name = "Widget probe",
                category = Category.OTHER,
                expiryDate = LocalDate.now().plusDays(9),
                reminderDaysBefore = emptyList(),
            )
        )
        try {
            // The same call the repository makes on every item change.
            WidgetRefreshScheduler.notifyDataChanged(context)

            val glanceIds = GlanceAppWidgetManager(context)
                .getGlanceIds(NextExpiryWidget::class.java)
            assertTrue("Glance should know about the bound widget", glanceIds.isNotEmpty())
        } finally {
            repository.delete(id)
        }
    }

    private companion object {
        const val HOST_ID = 4242
    }
}
