package dev.randyapps.lapse.data.db

import dev.randyapps.lapse.data.model.Category
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Instant
import java.time.LocalDate

class ConvertersTest {

    private val converters = Converters()

    @Test
    fun `LocalDate round trips`() {
        val date = LocalDate.of(2031, 2, 28)
        val stored = converters.localDateToEpochDay(date)
        assertEquals(date, converters.epochDayToLocalDate(stored))
    }

    @Test
    fun `LocalDate round trips before the epoch`() {
        val date = LocalDate.of(1957, 6, 3)
        assertEquals(date, converters.epochDayToLocalDate(converters.localDateToEpochDay(date)))
    }

    @Test
    fun `Instant round trips at millisecond precision`() {
        val instant = Instant.ofEpochMilli(1_776_123_456_789L)
        assertEquals(instant, converters.epochMilliToInstant(converters.instantToEpochMilli(instant)))
    }

    @Test
    fun `every Category round trips`() {
        Category.entries.forEach { category ->
            assertEquals(category, converters.nameToCategory(converters.categoryToName(category)))
        }
    }

    @Test
    fun `unknown category name falls back to OTHER rather than throwing`() {
        // Guards against a row written by a future build with a category this one lacks.
        assertEquals(Category.OTHER, converters.nameToCategory("PET_LICENSE"))
    }

    @Test
    fun `reminder days round trip`() {
        val reminders = listOf(30, 7, 1)
        assertEquals(reminders, converters.stringToReminderDays(converters.reminderDaysToString(reminders)))
    }

    @Test
    fun `reminder days preserve order`() {
        val reminders = listOf(1, 90, 7)
        assertEquals(reminders, converters.stringToReminderDays(converters.reminderDaysToString(reminders)))
    }

    @Test
    fun `empty reminder list round trips as empty, not as a bogus entry`() {
        val stored = converters.reminderDaysToString(emptyList())
        assertEquals(emptyList<Int>(), converters.stringToReminderDays(stored))
    }

    @Test
    fun `single reminder round trips`() {
        assertEquals(listOf(14), converters.stringToReminderDays(converters.reminderDaysToString(listOf(14))))
    }

    @Test
    fun `nulls stay null in both directions`() {
        assertNull(converters.localDateToEpochDay(null))
        assertNull(converters.epochDayToLocalDate(null))
        assertNull(converters.instantToEpochMilli(null))
        assertNull(converters.epochMilliToInstant(null))
        assertNull(converters.categoryToName(null))
        assertNull(converters.nameToCategory(null))
        assertNull(converters.reminderDaysToString(null))
        assertNull(converters.stringToReminderDays(null))
    }

    @Test
    fun `malformed reminder entries are skipped rather than crashing the read`() {
        assertEquals(listOf(30, 7), converters.stringToReminderDays("30,oops,7"))
    }
}
