package dev.randyapps.lapse.widget

import dev.randyapps.lapse.data.model.Category
import dev.randyapps.lapse.data.model.Item
import dev.randyapps.lapse.data.model.statusFor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Instant
import java.time.LocalDate

/** Which single item the widget shows. Pure logic, so it lives on the JVM. */
class WidgetItemSelectionTest {

    private val today = LocalDate.of(2026, 8, 16)

    private fun item(name: String, days: Int) = Item(
        id = name.hashCode().toLong(),
        name = name,
        category = Category.OTHER,
        expiryDate = today.plusDays(days.toLong()),
        reminderDaysBefore = emptyList(),
        note = null,
        photoPath = null,
        createdAt = Instant.EPOCH,
        daysRemaining = days,
        status = statusFor(days),
    )

    @Test
    fun `with nothing tracked there is nothing to show`() {
        assertNull(selectWidgetItem(emptyList()))
    }

    @Test
    fun `with nothing expired it is the soonest upcoming`() {
        val chosen = selectWidgetItem(
            listOf(item("Later", 200), item("Soonest", 4), item("Middle", 60))
        )
        assertEquals("Soonest", chosen?.name)
    }

    @Test
    fun `an expired item beats an upcoming one`() {
        // Expired is shown as expired, not skipped for the next active item.
        val chosen = selectWidgetItem(listOf(item("Upcoming", 3), item("Expired", -2)))
        assertEquals("Expired", chosen?.name)
    }

    @Test
    fun `among expired items the most recent one wins`() {
        // The gym membership that lapsed three months ago is not the useful thing to show.
        val chosen = selectWidgetItem(
            listOf(item("Gym", -95), item("Boiler", -12), item("Inspection", -40))
        )
        assertEquals("Boiler", chosen?.name)
    }

    @Test
    fun `an item expiring today is upcoming, not expired`() {
        // Zero days remaining is still valid today, so it must not lose to a past item.
        val chosen = selectWidgetItem(listOf(item("Today", 0), item("Tomorrow", 1)))
        assertEquals("Today", chosen?.name)
    }

    @Test
    fun `expiring today still loses to something already expired`() {
        val chosen = selectWidgetItem(listOf(item("Today", 0), item("Expired", -1)))
        assertEquals("Expired", chosen?.name)
    }

    @Test
    fun `a single expired item is shown`() {
        assertEquals("Only", selectWidgetItem(listOf(item("Only", -30)))?.name)
    }
}
