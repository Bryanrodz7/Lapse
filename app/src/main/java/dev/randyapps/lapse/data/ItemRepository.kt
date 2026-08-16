package dev.randyapps.lapse.data

import dev.randyapps.lapse.data.db.ItemDao
import dev.randyapps.lapse.data.db.ItemEntity
import dev.randyapps.lapse.data.model.Item
import dev.randyapps.lapse.data.model.ItemDraft
import dev.randyapps.lapse.data.model.daysRemaining
import dev.randyapps.lapse.data.model.statusFor
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The only thing that turns stored rows into [Item]s, which is where days-remaining and status
 * get attached. Keeping that in one place is what guarantees no caller can accidentally read a
 * row without its derived state.
 */
@Singleton
class ItemRepository @Inject constructor(
    private val dao: ItemDao,
    private val clock: Clock,
) {

    fun observeItems(): Flow<List<Item>> =
        dao.observeAll().map { entities -> entities.map { it.toItem(today()) } }

    fun observeItem(id: Long): Flow<Item?> =
        dao.observeById(id).map { it?.toItem(today()) }

    suspend fun getItem(id: Long): Item? = dao.getById(id)?.toItem(today())

    /** One-shot read for rescheduling reminders, where there's no Flow to collect. */
    suspend fun getAllItems(): List<Item> = dao.getAll().map { it.toItem(today()) }

    /**
     * Creates or updates, returning the row id so a caller can schedule reminders for it
     * immediately. `createdAt` is stamped on first save and preserved on every later one.
     */
    suspend fun save(draft: ItemDraft): Long {
        val createdAt = if (draft.isNew) {
            clock.instant()
        } else {
            dao.getById(draft.id)?.createdAt ?: clock.instant()
        }
        return dao.upsert(draft.toEntity(createdAt))
    }

    suspend fun delete(id: Long) = dao.deleteById(id)

    /**
     * Puts a deleted item back with its original id and creation time, so undo restores the
     * item rather than making a lookalike copy.
     */
    suspend fun restore(item: Item): Long = dao.upsert(item.toEntity())

    private fun today(): LocalDate = LocalDate.now(clock)
}

internal fun ItemEntity.toItem(today: LocalDate): Item {
    val days = daysRemaining(expiryDate, today)
    return Item(
        id = id,
        name = name,
        category = category,
        expiryDate = expiryDate,
        reminderDaysBefore = reminderDaysBefore,
        note = note,
        photoPath = photoPath,
        createdAt = createdAt,
        daysRemaining = days,
        status = statusFor(days),
    )
}

internal fun ItemDraft.toEntity(createdAt: Instant): ItemEntity = ItemEntity(
    id = id,
    name = name,
    category = category,
    expiryDate = expiryDate,
    reminderDaysBefore = reminderDaysBefore,
    note = note,
    photoPath = photoPath,
    createdAt = createdAt,
)

internal fun Item.toEntity(): ItemEntity = ItemEntity(
    id = id,
    name = name,
    category = category,
    expiryDate = expiryDate,
    reminderDaysBefore = reminderDaysBefore,
    note = note,
    photoPath = photoPath,
    createdAt = createdAt,
)
