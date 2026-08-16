package dev.randyapps.lapse.data

import dev.randyapps.lapse.data.db.ItemEntity
import dev.randyapps.lapse.data.model.Category
import dev.randyapps.lapse.data.model.ExpirySection
import dev.randyapps.lapse.data.model.ItemDraft
import dev.randyapps.lapse.data.model.ItemStatus
import dev.randyapps.lapse.data.model.toDraft
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

    private fun repository(vararg entities: ItemEntity) =
        ItemRepository(FakeItemDao(entities.toList()), clock)

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
}
