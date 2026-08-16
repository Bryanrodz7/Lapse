package dev.randyapps.lapse.data.db

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.randyapps.lapse.data.model.Category
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant
import java.time.LocalDate

/**
 * Exercises the DAO against real SQLite, which is the only way to prove the converters
 * survive a genuine write/read cycle rather than just a function call.
 */
@RunWith(AndroidJUnit4::class)
class ItemDaoTest {

    private lateinit var db: LapseDatabase
    private lateinit var dao: ItemDao

    private fun item(
        name: String,
        expiry: LocalDate,
        category: Category = Category.ID_AND_LICENSE,
        reminders: List<Int> = listOf(30, 7, 1),
        note: String? = null,
        photoPath: String? = null,
    ) = ItemEntity(
        name = name,
        category = category,
        expiryDate = expiry,
        reminderDaysBefore = reminders,
        note = note,
        photoPath = photoPath,
        createdAt = Instant.ofEpochMilli(1_700_000_000_000L),
    )

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            LapseDatabase::class.java,
        ).build()
        dao = db.itemDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun insertedItemComesBackIntact() = runTest {
        val id = dao.upsert(
            item(
                name = "Passport",
                expiry = LocalDate.of(2030, 4, 9),
                category = Category.ID_AND_LICENSE,
                reminders = listOf(90, 30, 7),
                note = "Renew at the county office",
                photoPath = "/data/user/0/dev.randyapps.lapse/files/photos/1.jpg",
            )
        )

        val stored = dao.getById(id)!!
        assertEquals("Passport", stored.name)
        assertEquals(Category.ID_AND_LICENSE, stored.category)
        assertEquals(LocalDate.of(2030, 4, 9), stored.expiryDate)
        assertEquals(listOf(90, 30, 7), stored.reminderDaysBefore)
        assertEquals("Renew at the county office", stored.note)
        assertEquals("/data/user/0/dev.randyapps.lapse/files/photos/1.jpg", stored.photoPath)
        assertEquals(Instant.ofEpochMilli(1_700_000_000_000L), stored.createdAt)
    }

    @Test
    fun optionalFieldsSurviveAsNull() = runTest {
        val id = dao.upsert(item("Gym membership", LocalDate.of(2027, 1, 1)))
        val stored = dao.getById(id)!!
        assertNull(stored.note)
        assertNull(stored.photoPath)
    }

    @Test
    fun emptyReminderListSurvivesTheRoundTrip() = runTest {
        val id = dao.upsert(item("Warranty", LocalDate.of(2029, 5, 5), reminders = emptyList()))
        assertEquals(emptyList<Int>(), dao.getById(id)!!.reminderDaysBefore)
    }

    @Test
    fun observeAllSortsBySoonestExpiry() = runTest {
        dao.upsert(item("Later", LocalDate.of(2030, 1, 1)))
        dao.upsert(item("Soonest", LocalDate.of(2026, 9, 1)))
        dao.upsert(item("Middle", LocalDate.of(2028, 3, 15)))

        val names = dao.observeAll().first().map { it.name }
        assertEquals(listOf("Soonest", "Middle", "Later"), names)
    }

    @Test
    fun observeAllEmitsAfterAWrite() = runTest {
        assertEquals(emptyList<ItemEntity>(), dao.observeAll().first())
        dao.upsert(item("Vehicle registration", LocalDate.of(2027, 6, 30)))
        assertEquals(1, dao.observeAll().first().size)
    }

    @Test
    fun upsertWithAnExistingIdUpdatesRatherThanDuplicating() = runTest {
        val id = dao.upsert(item("Car insurance", LocalDate.of(2027, 2, 1)))
        val original = dao.getById(id)!!

        dao.upsert(original.copy(expiryDate = LocalDate.of(2028, 2, 1)))

        val all = dao.getAll()
        assertEquals(1, all.size)
        assertEquals(LocalDate.of(2028, 2, 1), all.single().expiryDate)
    }

    @Test
    fun upsertReturnValueForAnUpdateIsNotTheRowId() = runTest {
        // Documents real Room behaviour: @Upsert returns the inserted rowId, and for an UPDATE
        // there is no insert, so the value is not usable as an id. Anything that needs the id
        // after saving an existing item must use the id it already had.
        val id = dao.upsert(item("Car insurance", LocalDate.of(2027, 2, 1)))
        val stored = dao.getById(id)!!

        val returnedOnUpdate = dao.upsert(stored.copy(name = "Renamed"))
        assertEquals("update should not report a fresh rowId", -1L, returnedOnUpdate)
    }

    @Test
    fun observeByIdEmitsNullOnceDeleted() = runTest {
        val id = dao.upsert(item("Inspection", LocalDate.of(2027, 8, 8)))
        assertEquals("Inspection", dao.observeById(id).first()!!.name)

        dao.deleteById(id)
        assertNull(dao.observeById(id).first())
    }

    @Test
    fun deleteRemovesOnlyTheTargetedItem() = runTest {
        val keepId = dao.upsert(item("Keep", LocalDate.of(2027, 1, 1)))
        val dropId = dao.upsert(item("Drop", LocalDate.of(2027, 2, 1)))

        dao.delete(dao.getById(dropId)!!)

        assertEquals(listOf(keepId), dao.getAll().map { it.id })
    }
}
