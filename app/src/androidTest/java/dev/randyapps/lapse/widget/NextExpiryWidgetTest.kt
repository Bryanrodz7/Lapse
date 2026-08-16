package dev.randyapps.lapse.widget

import android.content.Context
import android.content.Intent
import androidx.glance.appwidget.testing.unit.runGlanceAppWidgetUnitTest
import androidx.glance.testing.unit.hasText
import androidx.test.core.app.ApplicationProvider
import dev.randyapps.lapse.data.model.Category
import dev.randyapps.lapse.data.model.Item
import dev.randyapps.lapse.data.model.statusFor
import org.junit.Test
import java.time.Instant
import java.time.LocalDate

/**
 * Asserts what the widget actually renders, per scenario. Launcher drag-and-drop cannot be driven
 * reliably from adb, so widget content is verified here rather than by eye.
 */
class NextExpiryWidgetTest {

    private val context: Context get() = ApplicationProvider.getApplicationContext()
    private val today: LocalDate = LocalDate.now()

    private fun item(name: String, days: Int) = Item(
        id = 1,
        name = name,
        category = Category.ID_AND_LICENSE,
        expiryDate = today.plusDays(days.toLong()),
        reminderDaysBefore = listOf(30),
        note = null,
        photoPath = null,
        createdAt = Instant.EPOCH,
        daysRemaining = days,
        status = statusFor(days),
    )

    @Test
    fun showsTheNameAndDayCount() = runGlanceAppWidgetUnitTest {
        provideComposable { WidgetBody(contentFor(context, item("Passport", 23)), Intent()) }

        onNode(hasText("Passport")).assertExists()
        onNode(hasText("23")).assertExists()
        onNode(hasText("DAYS LEFT")).assertExists()
    }

    @Test
    fun showsTodayRatherThanZero() = runGlanceAppWidgetUnitTest {
        provideComposable {
            WidgetBody(contentFor(context, item("Driver's License", 0)), Intent())
        }

        onNode(hasText("Today")).assertExists()
        onNode(hasText("LAST DAY")).assertExists()
    }

    @Test
    fun anExpiredItemIsShownAsExpiredNotSkipped() = runGlanceAppWidgetUnitTest {
        provideComposable { WidgetBody(contentFor(context, item("Boiler Service", -12)), Intent()) }

        onNode(hasText("Boiler Service")).assertExists()
        onNode(hasText("12")).assertExists()
        onNode(hasText("DAYS AGO")).assertExists()
    }

    @Test
    fun aSingleDayIsSingular() = runGlanceAppWidgetUnitTest {
        provideComposable { WidgetBody(contentFor(context, item("Passport", 1)), Intent()) }
        onNode(hasText("DAY LEFT")).assertExists()
    }

    @Test
    fun withNothingTrackedItInvitesYouToAdd() = runGlanceAppWidgetUnitTest {
        provideComposable { WidgetBody(emptyContent(context), Intent()) }

        onNode(hasText("Nothing is about to lapse.")).assertExists()
        onNode(hasText("Tap to add something")).assertExists()
    }
}
