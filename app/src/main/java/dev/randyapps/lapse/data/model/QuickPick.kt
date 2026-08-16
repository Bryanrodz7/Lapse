package dev.randyapps.lapse.data.model

/**
 * The handful of things people actually track, pre-filled.
 *
 * This is the single biggest speed win over the competition: tapping one of these fills the
 * name, category and a sensible reminder set, leaving only the date to choose.
 */
enum class QuickPick(
    val itemName: String,
    val category: Category,
    val reminderDaysBefore: List<Int>,
    /** Typical renewal term, used to pre-fill the date so most picks need no date entry at all. */
    val typicalYears: Long,
) {
    DRIVERS_LICENSE("Driver's License", Category.ID_AND_LICENSE, listOf(30, 7, 1), 4),
    PASSPORT("Passport", Category.ID_AND_LICENSE, listOf(180, 90, 30), 10),
    VEHICLE_REGISTRATION("Vehicle Registration", Category.VEHICLE, listOf(30, 7, 1), 1),
    CAR_INSURANCE("Car Insurance", Category.INSURANCE, listOf(30, 7), 1),
    VEHICLE_INSPECTION("Vehicle Inspection", Category.VEHICLE, listOf(30, 7), 1),
    ;

    fun toDraft(today: java.time.LocalDate): ItemDraft = ItemDraft(
        name = itemName,
        category = category,
        expiryDate = today.plusYears(typicalYears),
        reminderDaysBefore = reminderDaysBefore,
    )
}

/** Used when nothing more specific is known. */
val DEFAULT_REMINDER_DAYS = listOf(30, 7, 1)

/** Offered as toggles on the edit form. */
val REMINDER_CHOICES = listOf(180, 90, 30, 14, 7, 1)
