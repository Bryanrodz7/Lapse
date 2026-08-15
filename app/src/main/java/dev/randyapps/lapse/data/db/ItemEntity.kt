package dev.randyapps.lapse.data.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import dev.randyapps.lapse.data.model.Category
import java.time.Instant
import java.time.LocalDate

/**
 * A tracked thing with an expiry date. The app's only entity.
 *
 * Note what is absent: no `daysRemaining`, no `status`. Both are computed on read from
 * [expiryDate], so they cannot drift out of date while the app is closed.
 */
// Indexed on expiryDate because every read is sorted by it.
@Entity(tableName = "items", indices = [Index("expiryDate")])
data class ItemEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,

    val name: String,

    val category: Category,

    val expiryDate: LocalDate,

    /** Days before expiry to fire a reminder, e.g. `[30, 7, 1]`. One worker per entry. */
    val reminderDaysBefore: List<Int>,

    val note: String? = null,

    /** Absolute path inside app-internal storage. Never external, never uploaded. */
    val photoPath: String? = null,

    val createdAt: Instant,
)
