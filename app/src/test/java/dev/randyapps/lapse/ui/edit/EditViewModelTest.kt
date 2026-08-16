package dev.randyapps.lapse.ui.edit

import androidx.lifecycle.SavedStateHandle
import dev.randyapps.lapse.data.FakeItemDao
import dev.randyapps.lapse.data.ItemRepository
import dev.randyapps.lapse.data.db.ItemEntity
import dev.randyapps.lapse.data.model.Category
import dev.randyapps.lapse.data.model.ItemDraft
import dev.randyapps.lapse.data.model.QuickPick
import android.net.Uri
import dev.randyapps.lapse.data.photo.PhotoStore
import dev.randyapps.lapse.data.settings.LapseSettings
import dev.randyapps.lapse.data.settings.SettingsStore
import dev.randyapps.lapse.data.settings.ThemeMode
import dev.randyapps.lapse.notifications.NotificationPermissionStore
import dev.randyapps.lapse.ui.nav.EditDestination
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

@OptIn(ExperimentalCoroutinesApi::class)
class EditViewModelTest {

    private val today = LocalDate.of(2026, 8, 15)
    private val clock = Clock.fixed(today.atStartOfDay(ZoneOffset.UTC).toInstant(), ZoneOffset.UTC)
    private val dispatcher = StandardTestDispatcher()

    private lateinit var repository: ItemRepository

    /** Starts "already asked" so most tests exercise the plain save path. */
    private class FakePermissionStore(override var hasAsked: Boolean = true) :
        NotificationPermissionStore

    private var permissionStore = FakePermissionStore()

    private class FakeSettingsStore(initial: LapseSettings = LapseSettings()) : SettingsStore {
        private val state = MutableStateFlow(initial)
        override val settings = state
        override suspend fun setDefaultReminderDays(days: List<Int>) {
            state.value = state.value.copy(defaultReminderDays = days)
        }
        override suspend fun setThemeMode(mode: ThemeMode) {
            state.value = state.value.copy(themeMode = mode)
        }
    }

    private var settingsStore = FakeSettingsStore()

    /** Pretends a pick always yields this stored path. */
    private class FakePhotoStore(private val stored: String? = "/photos/new.jpg") : PhotoStore {
        val deleted = mutableListOf<String?>()
        override suspend fun save(source: Uri): String? = stored
        override suspend fun delete(path: String?) { deleted += path }
    }

    private var photoStore = FakePhotoStore()

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun viewModel(
        itemId: Long = ItemDraft.NEW_ITEM_ID,
        seed: List<ItemEntity> = emptyList(),
        quickPick: String = EditDestination.NO_QUICK_PICK,
    ): EditViewModel {
        repository = ItemRepository(FakeItemDao(seed), clock)
        return EditViewModel(
            repository = repository,
            clock = clock,
            permissionStore = permissionStore,
            settingsStore = settingsStore,
            photoStore = photoStore,
            savedStateHandle = SavedStateHandle(
                mapOf(
                    EditDestination.ARG_ITEM_ID to itemId,
                    EditDestination.ARG_QUICK_PICK to quickPick,
                )
            ),
        )
    }

    private fun entity(
        id: Long,
        name: String,
        expiry: LocalDate,
        createdAt: Instant = today.minusYears(1).atStartOfDay(ZoneOffset.UTC).toInstant(),
    ) = ItemEntity(
        id = id,
        name = name,
        category = Category.VEHICLE,
        expiryDate = expiry,
        reminderDaysBefore = listOf(30, 7),
        note = "note here",
        photoPath = null,
        createdAt = createdAt,
    )

    // --- new item ---

    @Test
    fun `a new item opens ready, dated a year out, and cannot be saved blank`() = runTest {
        val vm = viewModel()
        advanceUntilIdle()
        val state = vm.state.value

        assertTrue(state.isNew)
        assertTrue(state.ready)
        assertEquals(today.plusYears(1), state.expiryDate)
        assertFalse("a blank name must block Save", state.canSave)
    }

    @Test
    fun `a new item uses the reminder defaults from settings`() = runTest {
        settingsStore = FakeSettingsStore(LapseSettings(defaultReminderDays = listOf(90, 14)))
        val vm = viewModel()
        advanceUntilIdle()

        assertEquals(listOf(90, 14), vm.state.value.reminderDaysBefore)
    }

    @Test
    fun `a quick pick's own reminders beat the settings defaults`() = runTest {
        // The pick is the more specific choice, so it must win.
        settingsStore = FakeSettingsStore(LapseSettings(defaultReminderDays = listOf(90, 14)))
        val vm = viewModel(quickPick = QuickPick.PASSPORT.name)
        advanceUntilIdle()

        assertEquals(listOf(180, 90, 30), vm.state.value.reminderDaysBefore)
    }

    @Test
    fun `editing an existing item ignores the settings defaults`() = runTest {
        // Changing a default must not silently rewrite reminders on items already saved.
        settingsStore = FakeSettingsStore(LapseSettings(defaultReminderDays = listOf(90, 14)))
        val vm = viewModel(4, listOf(entity(4, "Passport", today.plusDays(200))))
        advanceUntilIdle()

        assertEquals(listOf(30, 7), vm.state.value.reminderDaysBefore)
    }

    @Test
    fun `a whitespace-only name does not count as a name`() = runTest {
        val vm = viewModel()
        advanceUntilIdle()
        vm.onNameChange("   ")
        assertFalse(vm.state.value.canSave)
    }

    @Test
    fun `a quick pick fills name, category, reminders and date in one tap`() = runTest {
        val vm = viewModel()
        advanceUntilIdle()
        vm.onQuickPick(QuickPick.PASSPORT)

        val state = vm.state.value
        assertEquals("Passport", state.name)
        assertEquals(Category.ID_AND_LICENSE, state.category)
        assertEquals(listOf(180, 90, 30), state.reminderDaysBefore)
        assertEquals(today.plusYears(10), state.expiryDate)
        assertTrue("a quick pick alone should be enough to save", state.canSave)
    }

    @Test
    fun `saving a new item persists it and closes the screen`() = runTest {
        val vm = viewModel()
        advanceUntilIdle()
        vm.onQuickPick(QuickPick.CAR_INSURANCE)
        vm.onSave()
        advanceUntilIdle()

        val saved = repository.getAllItems().single()
        assertEquals("Car Insurance", saved.name)
        assertEquals(Category.INSURANCE, saved.category)
        assertTrue(vm.state.value.finished)
    }

    @Test
    fun `saving trims the name and drops a blank note`() = runTest {
        val vm = viewModel()
        advanceUntilIdle()
        vm.onNameChange("  Passport  ")
        vm.onNoteChange("   ")
        vm.onSave()
        advanceUntilIdle()

        val saved = repository.getAllItems().single()
        assertEquals("Passport", saved.name)
        assertNull(saved.note)
    }

    @Test
    fun `save is a no-op while the name is blank`() = runTest {
        val vm = viewModel()
        advanceUntilIdle()
        vm.onSave()
        advanceUntilIdle()

        assertEquals(emptyList<String>(), repository.getAllItems().map { it.name })
        assertFalse(vm.state.value.finished)
    }

    @Test
    fun `reminders toggle off and on, staying sorted`() = runTest {
        val vm = viewModel()
        advanceUntilIdle()
        vm.onToggleReminder(7)
        assertFalse(7 in vm.state.value.reminderDaysBefore)

        vm.onToggleReminder(90)
        assertEquals(listOf(90, 30, 1), vm.state.value.reminderDaysBefore)
    }

    @Test
    fun `date shortcuts set a date relative to today`() = runTest {
        val vm = viewModel()
        advanceUntilIdle()
        vm.onExpiryDateChange(today.plusYears(5))
        assertEquals(today.plusYears(5), vm.state.value.expiryDate)
    }

    @Test
    fun `opening from an empty-state quick pick arrives pre-filled and ready to save`() = runTest {
        val vm = viewModel(quickPick = QuickPick.VEHICLE_REGISTRATION.name)
        advanceUntilIdle()
        val state = vm.state.value

        assertEquals("Vehicle Registration", state.name)
        assertEquals(Category.VEHICLE, state.category)
        assertEquals(listOf(30, 7, 1), state.reminderDaysBefore)
        assertEquals(today.plusYears(1), state.expiryDate)
        assertTrue("one tap from the empty state should be enough to save", state.canSave)
    }

    @Test
    fun `an unknown quick pick name is ignored rather than crashing`() = runTest {
        // Guards against a stale deep link or a renamed enum constant.
        val vm = viewModel(quickPick = "NOT_A_REAL_PICK")
        advanceUntilIdle()
        assertEquals("", vm.state.value.name)
        assertTrue(vm.state.value.ready)
    }

    // --- existing item ---

    @Test
    fun `an existing item loads its stored values`() = runTest {
        val vm = viewModel(4, listOf(entity(4, "Vehicle Registration", today.plusDays(200))))
        advanceUntilIdle()

        val state = vm.state.value
        assertFalse(state.isNew)
        assertTrue(state.ready)
        assertEquals("Vehicle Registration", state.name)
        assertEquals(today.plusDays(200), state.expiryDate)
        assertEquals("note here", state.note)
    }

    @Test
    fun `editing updates in place rather than creating a second item`() = runTest {
        val vm = viewModel(4, listOf(entity(4, "Old name", today.plusDays(200))))
        advanceUntilIdle()

        vm.onNameChange("New name")
        vm.onSave()
        advanceUntilIdle()

        val all = repository.getAllItems()
        assertEquals(1, all.size)
        assertEquals("New name", all.single().name)
        assertEquals(4L, all.single().id)
    }

    @Test
    fun `a missing item closes the screen instead of showing a blank form`() = runTest {
        val vm = viewModel(99)
        advanceUntilIdle()
        assertTrue(vm.state.value.finished)
    }

    // --- notification permission ---

    @Test
    fun `saving the first item asks for notification permission before closing`() = runTest {
        permissionStore = FakePermissionStore(hasAsked = false)
        val vm = viewModel()
        advanceUntilIdle()
        vm.onNameChange("Passport")
        vm.onSave()
        advanceUntilIdle()

        // The item is saved, but the screen stays put until the prompt resolves.
        assertEquals("Passport", repository.getAllItems().single().name)
        assertTrue(vm.state.value.askNotificationPermission)
        assertFalse(vm.state.value.finished)
    }

    @Test
    fun `the permission prompt is never shown twice`() = runTest {
        permissionStore = FakePermissionStore(hasAsked = true)
        val vm = viewModel()
        advanceUntilIdle()
        vm.onNameChange("Passport")
        vm.onSave()
        advanceUntilIdle()

        assertFalse(vm.state.value.askNotificationPermission)
        assertTrue(vm.state.value.finished)
    }

    @Test
    fun `declining the permission still saves and closes`() = runTest {
        permissionStore = FakePermissionStore(hasAsked = false)
        val vm = viewModel()
        advanceUntilIdle()
        vm.onNameChange("Passport")
        vm.onSave()
        advanceUntilIdle()

        vm.onNotificationPermissionSettled()

        assertTrue("asked once, never again", permissionStore.hasAsked)
        assertTrue(vm.state.value.finished)
        assertEquals(1, repository.getAllItems().size)
    }

    // --- photo ---
    // Picking needs a real android.net.Uri, which JVM tests cannot construct, so those cases
    // live in EditViewModelPhotoTest under androidTest.

    @Test
    fun `removing a photo clears it from the form only`() = runTest {
        val vm = viewModel(4, listOf(entity(4, "Passport", today.plusDays(30)).copy(photoPath = "/photos/old.jpg")))
        advanceUntilIdle()

        vm.onPhotoRemoved()

        assertNull(vm.state.value.photoPath)
        // Backing out without saving must not have destroyed the file.
        assertEquals(emptyList<String?>(), photoStore.deleted)
    }

    // --- renew ---

    @Test
    fun `renew is not offered on a new item`() = runTest {
        val vm = viewModel()
        advanceUntilIdle()
        assertFalse(vm.state.value.canRenew)
    }

    @Test
    fun `renew is not offered while the expiry is far away`() = runTest {
        val vm = viewModel(4, listOf(entity(4, "Passport", today.plusDays(200))))
        advanceUntilIdle()
        assertFalse(vm.state.value.canRenew)
        assertNull(vm.state.value.renewalTarget)
    }

    @Test
    fun `renew is offered once an item is close to expiry`() = runTest {
        val vm = viewModel(4, listOf(entity(4, "Registration", today.plusDays(5))))
        advanceUntilIdle()
        assertTrue(vm.state.value.canRenew)
    }

    @Test
    fun `renew is offered on an already expired item`() = runTest {
        val vm = viewModel(4, listOf(entity(4, "Boiler service", today.minusDays(30))))
        advanceUntilIdle()
        assertTrue(vm.state.value.canRenew)
    }

    @Test
    fun `renewing moves the date a full term forward`() = runTest {
        // Created a year before expiry, so the inferred term is annual.
        val expiry = today.plusDays(5)
        val vm = viewModel(
            4,
            listOf(
                entity(
                    id = 4,
                    name = "Registration",
                    expiry = expiry,
                    createdAt = expiry.minusYears(1).atStartOfDay(ZoneOffset.UTC).toInstant(),
                )
            ),
        )
        advanceUntilIdle()

        vm.onRenew()
        assertEquals(expiry.plusYears(1), vm.state.value.expiryDate)
    }

    @Test
    fun `renewing only changes the form until it is saved`() = runTest {
        val expiry = today.plusDays(5)
        val vm = viewModel(4, listOf(entity(4, "Registration", expiry)))
        advanceUntilIdle()

        vm.onRenew()
        // Still the original date on disk: renew is reviewable before Save.
        assertEquals(expiry, repository.getItem(4)!!.expiryDate)

        vm.onSave()
        advanceUntilIdle()
        assertTrue(repository.getItem(4)!!.expiryDate.isAfter(today))
    }
}
