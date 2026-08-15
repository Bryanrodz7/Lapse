package dev.randyapps.lapse.data.db

import androidx.room.TypeConverter
import dev.randyapps.lapse.data.model.Category
import java.time.Instant
import java.time.LocalDate

/**
 * Room type converters for the entity's non-primitive columns.
 *
 * Dates store as epoch day and instants as epoch millis — both integers, so SQLite can sort
 * and range-query them directly. Reminder offsets store as a comma-joined string rather than
 * JSON, which avoids adding a serialization dependency for a list of small ints.
 */
class Converters {

    @TypeConverter
    fun localDateToEpochDay(value: LocalDate?): Long? = value?.toEpochDay()

    @TypeConverter
    fun epochDayToLocalDate(value: Long?): LocalDate? = value?.let(LocalDate::ofEpochDay)

    @TypeConverter
    fun instantToEpochMilli(value: Instant?): Long? = value?.toEpochMilli()

    @TypeConverter
    fun epochMilliToInstant(value: Long?): Instant? = value?.let(Instant::ofEpochMilli)

    @TypeConverter
    fun categoryToName(value: Category?): String? = value?.name

    @TypeConverter
    fun nameToCategory(value: String?): Category? = value?.let(Category::fromName)

    @TypeConverter
    fun reminderDaysToString(value: List<Int>?): String? = value?.joinToString(SEPARATOR)

    @TypeConverter
    fun stringToReminderDays(value: String?): List<Int>? = when {
        value == null -> null
        // "".split(",") yields [""], which would blow up on toInt(). An item with reminders
        // switched off is a real state, so it has to round-trip as an empty list.
        value.isBlank() -> emptyList()
        else -> value.split(SEPARATOR).mapNotNull { it.trim().toIntOrNull() }
    }

    private companion object {
        const val SEPARATOR = ","
    }
}
