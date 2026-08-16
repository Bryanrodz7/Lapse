package dev.randyapps.lapse.data

import dev.randyapps.lapse.data.db.ItemEntity
import dev.randyapps.lapse.data.model.Category
import dev.randyapps.lapse.data.model.ExpirySection
import dev.randyapps.lapse.data.model.ItemDraft
import dev.randyapps.lapse.data.model.ItemStatus
import dev.randyapps.lapse.data.model.Item
import dev.randyapps.lapse.data.model.toDraft
import dev.randyapps.lapse.data.photo.PhotoStore
import org.junit.Assert.assertTrue
import android.net.Uri
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

class ItemRepositoryTest {

    // Pinned so "today" is 2026-08-15 in every test below.
    private val today = LocalDate.of(2026, 8, 15)
    private val clock = Clock.fixed(
        today.atStartOfDay(ZoneOffset.UTC).toInstant().plusSeconds(12 * 3600),
        ZoneOffset.UTC,
    )

    private fun entity(
        id: Long,
        name: String,
        expiry: LocalDate,
        createdAt: Instant = Instant.ofEpochMilli(1_000),
    ) = ItemEntity(
        id = id,
        name = name,
        category = Category.VEHICLE,
        expiryDate = expiry,
        reminderDaysBefore = listOf(30, 7),
        note = null,
        photoPath = null,
        createdAt = createdAt,
    )

    /** Records what the repository asked the scheduler to do. */
    private class RecordingScheduler : ReminderScheduler {
        val scheduled = mutableListOf<Item>()
        val cancelled = mutableListOf<Long>()
        var rescheduledAll = 0

        override suspend fun schedule(item: Item) { scheduled += item }
        override fun cancel(itemId: Long) { cancelled += itemId }
        override suspend fun rescheduleAll(items: List<Item>) { rescheduledAll++ }
    }

    /** Records photo deletions so the lifecycle can be asserted without touching a filesystem. */
    private class RecordingPhotoStore : PhotoStore {
        val deleted = mutableListOf<String?>()
        override suspend fun save(source: Uri): String? = null
        override suspend fun delete(path: String?) { deleted += path }
    }

    private class RecordingChangeNotifier : ItemChangeNotifier {
        var changes = 0
        override suspend fun onItemsChanged() { changes++ }
    }

    private lateinit var scheduler: RecordingScheduler
    private lateinit var photos: RecordingPhotoStore
    private lateinit var changes: RecordingChangeNotifier

    private fun repository(vararg entities: ItemEntity): ItemRepository {
        scheduler = RecordingScheduler()
        photos = RecordingPhotoStore()
        changes = RecordingChangeNotifier()
        return ItemRepository(FakeItemDao(entities.toList()), clock, scheduler, photos, changes)
    }

    @Test
    fun `derived status is attached on read`() = runTest {
        val repo = repository(entity(1, "Passport", today.plusDays(3)))
        val item = repo.observeItems().first().single()

        assertEquals(3, item.daysRemaining)
        assertEquals(ItemStatus.URGENT, item.status)
    }

    @Test
    fun `an item expiring today reads as EXPIRES_TODAY, not EXPIRED`() = runTest {
        val repo = repository(entity(1, "License", today))
        val item = repo.observeItems().first().single()

        assertEquals(0, item.daysRemaining)
        assertEquals(ItemStatus.EXPIRES_TODAY, item.status)
        assertEquals(ExpirySection.THIS_MONTH, item.section)
    }

    @Test
    fun `a past date reads as EXPIRED with a negative day count`() = runTest {
        val repo = repository(entity(1, "Inspection", today.minusDays(5)))
        val item = repo.observeItems().first().single()

        assertEquals(-5, item.daysRemaining)
        assertEquals(ItemStatus.EXPIRED, item.status)
        assertEquals(ExpirySection.EXPIRED, item.section)
    }

    @Test
    fun `stored fields survive the mapping unchanged`() = runTest {
        val repo = repository(
            entity(7, "Passport", today.plusDays(400)).copy(
                category = Category.ID_AND_LICENSE,
                reminderDaysBefore = listOf(90, 30, 7),
                note = "County office",
                photoPath = "/data/photos/7.jpg",
            )
        )
        val item = repo.observeItems().first().single()

        assertEquals(7L, item.id)
        assertEquals("Passport", item.name)
        assertEquals(Category.ID_AND_LICENSE, item.category)
        assertEquals(listOf(90, 30, 7), item.reminderDaysBefore)
        assertEquals("County office", item.note)
        assertEquals("/data/photos/7.jpg", item.photoPath)
    }

    @Test
    fun `items come back sorted soonest first`() = runTest {
        val repo = repository(
            entity(1, "Later", today.plusDays(200)),
            entity(2, "Soonest", today.plusDays(2)),
            entity(3, "Middle", today.plusDays(60)),
        )
        assertEquals(
            listOf("Soonest", "Middle", "Later"),
            repo.observeItems().first().map { it.name },
        )
    }

    @Test
    fun `saving a new draft stamps createdAt from the clock`() = runTest {
        val repo = repository()
        val id = repo.save(
            ItemDraft(
                name = "Car insurance",
                category = Category.INSURANCE,
                expiryDate = today.plusDays(120),
                reminderDaysBefore = listOf(30),
            )
        )

        assertEquals(clock.instant(), repo.getItem(id)!!.createdAt)
    }

    @Test
    fun `editing an existing item preserves its original createdAt`() = runTest {
        val original = Instant.ofEpochMilli(1_600_000_000_000)
        val repo = repository(entity(4, "Registration", today.plusDays(10), createdAt = original))

        val edited = repo.getItem(4)!!.toDraft().copy(name = "Vehicle registration")
        repo.save(edited)

        val stored = repo.getItem(4)!!
        assertEquals("Vehicle registration", stored.name)
        // The edit must not look like a brand-new item.
        assertEquals(original, stored.createdAt)
        assertNotEquals(clock.instant(), stored.createdAt)
    }

    @Test
    fun `saving an edit updates in place rather than adding a row`() = runTest {
        val repo = repository(entity(4, "Registration", today.plusDays(10)))
        repo.save(repo.getItem(4)!!.toDraft().copy(expiryDate = today.plusDays(400)))

        val all = repo.observeItems().first()
        assertEquals(1, all.size)
        assertEquals(today.plusDays(400), all.single().expiryDate)
    }

    @Test
    fun `delete removes the item`() = runTest {
        val repo = repository(entity(1, "Gym", today.plusDays(30)))
        repo.delete(1)
        assertEquals(emptyList<String>(), repo.observeItems().first().map { it.name })
        assertNull(repo.getItem(1))
    }

    @Test
    fun `restore puts the item back with its original id and createdAt`() = runTest {
        val original = Instant.ofEpochMilli(1_600_000_000_000)
        val repo = repository(entity(9, "Warranty", today.plusDays(45), createdAt = original))

        val deleted = repo.getItem(9)!!
        repo.delete(9)
        repo.restore(deleted)

        val restored = repo.getItem(9)!!
        // Undo must restore the item, not create a lookalike with a new id.
        assertEquals(9L, restored.id)
        assertEquals(original, restored.createdAt)
        assertEquals("Warranty", restored.name)
    }

    @Test
    fun `observeItem emits null once the item is gone`() = runTest {
        val repo = repository(entity(1, "Passport", today.plusDays(5)))
        assertEquals("Passport", repo.observeItem(1).first()!!.name)

        repo.delete(1)
        assertNull(repo.observeItem(1).first())
    }

    @Test
    fun `getAllItems carries derived status too`() = runTest {
        val repo = repository(
            entity(1, "Expired", today.minusDays(1)),
            entity(2, "Active", today.plusDays(365)),
        )
        assertEquals(
            listOf(ItemStatus.EXPIRED, ItemStatus.ACTIVE),
            repo.getAllItems().map { it.status },
        )
    }

    // --- reminder scheduling ---

    @Test
    fun `saving a new item schedules its reminders`() = runTest {
        val repo = repository()
        repo.save(
            ItemDraft(
                name = "Passport",
                category = Category.ID_AND_LICENSE,
                expiryDate = today.plusDays(400),
                reminderDaysBefore = listOf(90, 30),
            )
        )

        assertEquals(1, scheduler.scheduled.size)
        assertEquals("Passport", scheduler.scheduled.single().name)
    }

    @Test
    fun `editing an item reschedules it with the new date`() = runTest {
        // Regression: @Upsert returns -1 for an update, so trusting its return value meant the
        // repository looked up id -1, found nothing, and silently skipped rescheduling. The
        // item's reminders stayed on the old date.
        val repo = repository(entity(4, "Dentist", today.plusDays(88)))
        val moved = repo.getItem(4)!!.toDraft().copy(expiryDate = today.plusDays(400))

        repo.save(moved)

        assertEquals(1, scheduler.scheduled.size)
        assertEquals(4L, scheduler.scheduled.single().id)
        assertEquals(today.plusDays(400), scheduler.scheduled.single().expiryDate)
    }

    @Test
    fun `save returns the real id when editing, not the upsert return value`() = runTest {
        val repo = repository(entity(4, "Dentist", today.plusDays(88)))
        val id = repo.save(repo.getItem(4)!!.toDraft().copy(name = "Dentist check-up"))
        assertEquals(4L, id)
    }

    @Test
    fun `deleting an item cancels its reminders`() = runTest {
        val repo = repository(entity(7, "Gym", today.plusDays(30)))
        repo.delete(7)
        assertEquals(listOf(7L), scheduler.cancelled)
    }

    @Test
    fun `restoring an undone delete schedules its reminders again`() = runTest {
        val repo = repository(entity(9, "Warranty", today.plusDays(45)))
        val item = repo.getItem(9)!!
        repo.delete(9)
        repo.restore(item)

        assertEquals(listOf(9L), scheduler.cancelled)
        assertEquals(listOf(9L), scheduler.scheduled.map { it.id })
    }

    @Test
    fun `rescheduleAllReminders hands over every item`() = runTest {
        val repo = repository(
            entity(1, "A", today.plusDays(10)),
            entity(2, "B", today.plusDays(20)),
        )
        repo.rescheduleAllReminders()
        assertEquals(1, scheduler.rescheduledAll)
    }

    // --- photo lifecycle ---

    @Test
    fun `replacing a photo deletes the file it replaced`() = runTest {
        val repo = repository(entity(4, "Passport", today.plusDays(200)).copy(photoPath = "/p/old.jpg"))
        repo.save(repo.getItem(4)!!.toDraft().copy(photoPath = "/p/new.jpg"))

        assertEquals(listOf("/p/old.jpg"), photos.deleted)
    }

    @Test
    fun `removing a photo deletes the file`() = runTest {
        val repo = repository(entity(4, "Passport", today.plusDays(200)).copy(photoPath = "/p/old.jpg"))
        repo.save(repo.getItem(4)!!.toDraft().copy(photoPath = null))

        assertEquals(listOf("/p/old.jpg"), photos.deleted)
    }

    @Test
    fun `saving with the photo unchanged deletes nothing`() = runTest {
        val repo = repository(entity(4, "Passport", today.plusDays(200)).copy(photoPath = "/p/keep.jpg"))
        repo.save(repo.getItem(4)!!.toDraft().copy(name = "Renamed"))

        assertEquals(emptyList<String?>(), photos.deleted)
    }

    @Test
    fun `deleting an item keeps its photo so undo can restore it`() = runTest {
        val repo = repository(entity(4, "Passport", today.plusDays(200)).copy(photoPath = "/p/keep.jpg"))
        repo.delete(4)

        // Deleting the file at swipe time would leave undo restoring an item with no photo.
        assertEquals(emptyList<String?>(), photos.deleted)
    }

    @Test
    fun `the photo is discarded only once the undo window closes`() = runTest {
        val repo = repository(entity(4, "Passport", today.plusDays(200)).copy(photoPath = "/p/gone.jpg"))
        val item = repo.getItem(4)!!
        repo.delete(4)
        repo.purgePhotoFor(item)

        assertEquals(listOf("/p/gone.jpg"), photos.deleted)
    }

    // --- widget refresh ---

    @Test
    fun `saving notifies that items changed so the widget redraws`() = runTest {
        val repo = repository()
        repo.save(
            ItemDraft(
                name = "Passport",
                category = Category.ID_AND_LICENSE,
                expiryDate = today.plusDays(30),
                reminderDaysBefore = emptyList(),
            )
        )
        assertEquals(1, changes.changes)
    }

    @Test
    fun `deleting and restoring both notify`() = runTest {
        val repo = repository(entity(4, "Passport", today.plusDays(30)))
        val item = repo.getItem(4)!!
        repo.delete(4)
        repo.restore(item)

        // Delete then restore: the widget must not be left showing a deleted item.
        assertEquals(2, changes.changes)
    }

    @Test
    fun `the widget's next item is the soonest by date, expired included`() = runTest {
        // The widget shows minByOrNull { expiryDate }; an expired item is shown as expired
        // rather than skipped in favour of the next active one.
        val repo = repository(
            entity(1, "Active", today.plusDays(10)),
            entity(2, "Expired", today.minusDays(3)),
        )
        val next = repo.getAllItems().minByOrNull { it.expiryDate }!!
        assertEquals("Expired", next.name)
        assertTrue(next.daysRemaining < 0)
    }

    @Test
    fun `with nothing tracked the widget has no item to show`() = runTest {
        assertEquals(null, repository().getAllItems().minByOrNull { it.expiryDate })
    }
}
