package dev.randyapps.lapse.ui.edit

import dev.randyapps.lapse.data.model.Category
import dev.randyapps.lapse.data.model.DEFAULT_REMINDER_DAYS
import dev.randyapps.lapse.data.model.ItemDraft
import dev.randyapps.lapse.data.model.ItemStatus
import dev.randyapps.lapse.data.model.SOON_THRESHOLD_DAYS
import dev.randyapps.lapse.data.model.daysRemaining
import dev.randyapps.lapse.data.model.renewedDate
import dev.randyapps.lapse.data.model.statusFor
import java.time.LocalDate

data class EditUiState(
    val itemId: Long = ItemDraft.NEW_ITEM_ID,
    val today: LocalDate = LocalDate.EPOCH,
    val name: String = "",
    val category: Category = Category.OTHER,
    val expiryDate: LocalDate = LocalDate.EPOCH,
    val reminderDaysBefore: List<Int> = DEFAULT_REMINDER_DAYS,
    val note: String = "",
    val photoPath: String? = null,
    /** True while a picked image is being copied and downscaled. */
    val savingPhoto: Boolean = false,
    /** Set once the item is loaded (edit) or immediately (new), so the form never flashes blanks. */
    val ready: Boolean = false,
    /** The creation date of an existing item, needed to infer its renewal term. */
    val createdOn: LocalDate? = null,
    val finished: Boolean = false,
    /** True once a save has completed and the notification prompt is still owed. */
    val askNotificationPermission: Boolean = false,
) {
    val isNew: Boolean get() = itemId == ItemDraft.NEW_ITEM_ID

    /** A name is the only thing genuinely required; everything else has a usable default. */
    val canSave: Boolean get() = name.isNotBlank()

    val daysRemaining: Int get() = daysRemaining(expiryDate, today)

    val status: ItemStatus get() = statusFor(daysRemaining)

    /**
     * Renewing is offered only once it's actually relevant — an item with a year left doesn't
     * need the button, and showing it everywhere would dilute it.
     */
    val canRenew: Boolean
        get() = !isNew && daysRemaining <= SOON_THRESHOLD_DAYS

    /** The date a Renewed tap would move to; shown on the control so it is never a surprise. */
    val renewalTarget: LocalDate?
        get() = if (canRenew) renewedDate(createdOn ?: expiryDate, expiryDate, today) else null

    fun toDraft(): ItemDraft = ItemDraft(
        id = itemId,
        name = name.trim(),
        category = category,
        expiryDate = expiryDate,
        reminderDaysBefore = reminderDaysBefore.sortedDescending(),
        note = note.trim().takeIf { it.isNotBlank() },
        photoPath = photoPath,
    )
}
