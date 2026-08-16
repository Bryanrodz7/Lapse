package dev.randyapps.lapse.ui.edit

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import dev.randyapps.lapse.data.ItemRepository
import dev.randyapps.lapse.data.db.ItemDao
import dev.randyapps.lapse.data.db.ItemEntity
import dev.randyapps.lapse.data.db.LapseDatabase
import dev.randyapps.lapse.data.model.Category
import dev.randyapps.lapse.data.model.ItemDraft
import dev.randyapps.lapse.data.photo.PhotoStore
import dev.randyapps.lapse.data.settings.LapseSettings
import dev.randyapps.lapse.data.settings.SettingsStore
import dev.randyapps.lapse.data.settings.ThemeMode
import dev.randyapps.lapse.notifications.NotificationPermissionStore
import dev.randyapps.lapse.ui.nav.EditDestination
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

/**
 * The photo-picking cases, which need a real android.net.Uri and so cannot run on the JVM.
 * Everything else about EditViewModel is covered by the unit tests.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class EditViewModelPhotoTest {

    private val today = LocalDate.of(2026, 8, 15)
    private val clock = Clock.fixed(today.atStartOfDay(ZoneOffset.UTC).toInstant(), ZoneOffset.UTC)
    private val dispatcher = StandardTestDispatcher()

    private class FakePhotoStore(private val stored: String?) : PhotoStore {
        override suspend fun save(source: Uri): String? = stored
        override suspend fun delete(path: String?) = Unit
    }

    private class FakeSettingsStore : SettingsStore {
        private val state = MutableStateFlow(LapseSettings())
        override val settings = state
        override suspend fun setDefaultReminderDays(days: List<Int>) = Unit
        override suspend fun setThemeMode(mode: ThemeMode) = Unit
    }

    private class FakePermissionStore : NotificationPermissionStore {
        override var hasAsked = true
    }

    private lateinit var db: LapseDatabase
    private lateinit var dao: ItemDao
    private lateinit var repository: ItemRepository

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        // Direct executors so Room's suspend calls resume on the calling thread. With Room's
        // default background executor, advanceUntilIdle() can return before a query finishes and
        // the ViewModel looks as if it never loaded the item.
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            LapseDatabase::class.java,
        )
            .setQueryExecutor { it.run() }
            .setTransactionExecutor { it.run() }
            .build()
        dao = db.itemDao()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        db.close()
    }

    private fun viewModel(itemId: Long = ItemDraft.NEW_ITEM_ID, stored: String? = "/photos/new.jpg"):
        EditViewModel {
        repository = ItemRepository(dao, clock)
        return EditViewModel(
            repository = repository,
            clock = clock,
            permissionStore = FakePermissionStore(),
            settingsStore = FakeSettingsStore(),
            photoStore = FakePhotoStore(stored),
            savedStateHandle = SavedStateHandle(
                mapOf(
                    EditDestination.ARG_ITEM_ID to itemId,
                    EditDestination.ARG_QUICK_PICK to EditDestination.NO_QUICK_PICK,
                )
            ),
        )
    }

    private suspend fun seedWithPhoto(path: String): Long = dao.upsert(
        ItemEntity(
            name = "Passport",
            category = Category.ID_AND_LICENSE,
            expiryDate = today.plusDays(30),
            reminderDaysBefore = listOf(30),
            note = null,
            photoPath = path,
            createdAt = Instant.EPOCH,
        )
    )

    @Test
    fun pickingAPhotoStoresItAndPutsThePathOnTheForm() = runTest {
        val vm = viewModel()
        advanceUntilIdle()

        vm.onPhotoPicked(Uri.parse("content://media/picked"))
        advanceUntilIdle()

        assertEquals("/photos/new.jpg", vm.state.value.photoPath)
        assertFalse(vm.state.value.savingPhoto)
    }

    @Test
    fun aPhotoThatCannotBeReadLeavesTheExistingOneAlone() = runTest {
        val id = seedWithPhoto("/photos/old.jpg")
        val vm = viewModel(itemId = id, stored = null)
        advanceUntilIdle()

        vm.onPhotoPicked(Uri.parse("content://media/broken"))
        advanceUntilIdle()

        assertEquals("/photos/old.jpg", vm.state.value.photoPath)
        assertFalse(vm.state.value.savingPhoto)
    }

    @Test
    fun thePhotoPathIsCarriedThroughToTheSavedItem() = runTest {
        val vm = viewModel()
        advanceUntilIdle()
        vm.onNameChange("Passport")
        vm.onPhotoPicked(Uri.parse("content://media/picked"))
        advanceUntilIdle()

        vm.onSave()
        advanceUntilIdle()

        assertEquals("/photos/new.jpg", repository.getAllItems().single().photoPath)
    }
}
