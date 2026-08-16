package dev.randyapps.lapse.data.model

import java.time.Instant
import java.time.LocalDate

/**
 * A tracked item as the UI consumes it: the stored fields plus the two values derived from
 * today's date. Built by the repository, never by Room.
 */
data class Item(
    val id: Long,
    val name: String,
    val category: Category,
    val expiryDate: LocalDate,
    val reminderDaysBefore: List<Int>,
    val note: String?,
    val photoPath: String?,
    val createdAt: Instant,
    val daysRemaining: Int,
    val status: ItemStatus,
) {
    val section: ExpirySection get() = sectionFor(daysRemaining)
}

/**
 * The editable shape of an item — stored fields only, no derived state.
 *
 * One type covers both create and edit: [id] of 0 means "new", and the repository assigns
 * `createdAt` on first save and preserves it on later ones.
 */
data class ItemDraft(
    val id: Long = NEW_ITEM_ID,
    val name: String,
    val category: Category,
    val expiryDate: LocalDate,
    val reminderDaysBefore: List<Int>,
    val note: String? = null,
    val photoPath: String? = null,
) {
    val isNew: Boolean get() = id == NEW_ITEM_ID

    companion object {
        const val NEW_ITEM_ID = 0L
    }
}

fun Item.toDraft(): ItemDraft = ItemDraft(
    id = id,
    name = name,
    category = category,
    expiryDate = expiryDate,
    reminderDaysBefore = reminderDaysBefore,
    note = note,
    photoPath = photoPath,
)
