package dev.randyapps.lapse.ui.home

import dev.randyapps.lapse.ads.AdsState
import dev.randyapps.lapse.data.FakeItemDao
import dev.randyapps.lapse.data.ItemRepository
import dev.randyapps.lapse.data.db.ItemEntity
import dev.randyapps.lapse.data.model.Category
import dev.randyapps.lapse.data.model.ExpirySection
import dev.randyapps.lapse.data.model.ItemDraft
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

    private val today = LocalDate.of(2026, 8, 15)
    private val clock = Clock.fixed(today.atStartOfDay(ZoneOffset.UTC).toInstant(), ZoneOffset.UTC)
    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun entity(id: Long, name: String, days: Long) = ItemEntity(
        id = id,
        name = name,
        category = Category.OTHER,
        expiryDate = today.plusDays(days),
        reminderDaysBefore = listOf(7),
        note = null,
        photoPath = null,
        createdAt = Instant.ofEpochMilli(1_000),
    )

    private lateinit var repository: ItemRepository

    private class FakeAdsState(enabled: Boolean) : AdsState {
        override val adsEnabled = flowOf(enabled)
    }

    private var adsState: AdsState = FakeAdsState(false)

    private fun viewModel(vararg entities: ItemEntity): HomeViewModel {
        repository = ItemRepository(FakeItemDao(entities.toList()), clock)
        return HomeViewModel(repository, adsState)
    }

    /**
     * Keeps a live collector on uiState for the whole test.
     *
     * uiState is shared with WhileSubscribed, so a one-shot `first {}` unsubscribes as soon as
     * it matches and the flow stops updating — later assertions would then read a stale cached
     * value. A real Compose screen holds the subscription while it's on screen, so this mirrors
     * production rather than working around it.
     */
    private fun TestScope.latestStateOf(vm: HomeViewModel): () -> HomeUiState {
        var latest = vm.uiState.value
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            vm.uiState.collect { latest = it }
        }
        return { latest }
    }

    @Test
    fun `starts loading and settles into grouped items`() = runTest {
        val vm = viewModel(entity(1, "Passport", 5), entity(2, "Warranty", 200))
        assertTrue(vm.uiState.value.isLoading)

        val state = latestStateOf(vm)
        advanceUntilIdle()

        assertFalse(state().isLoading)
        assertEquals(
            listOf(ExpirySection.THIS_MONTH, ExpirySection.LATER),
            state().groups.map { it.section },
        )
    }

    @Test
    fun `no items settles into the empty state, not permanent loading`() = runTest {
        val vm = viewModel()
        val state = latestStateOf(vm)
        advanceUntilIdle()

        assertFalse(state().isLoading)
        assertTrue(state().isEmpty)
    }

    @Test
    fun `deleting removes the row and offers an undo`() = runTest {
        val vm = viewModel(entity(1, "Gym", 20))
        val state = latestStateOf(vm)
        advanceUntilIdle()

        vm.delete(state().groups.single().items.single())
        advanceUntilIdle()

        assertTrue(state().isEmpty)
        assertEquals("Gym", vm.recentlyDeleted.value?.name)
    }

    @Test
    fun `undo restores the item with its original id`() = runTest {
        val vm = viewModel(entity(42, "Passport", 5))
        val state = latestStateOf(vm)
        advanceUntilIdle()

        vm.delete(state().groups.single().items.single())
        advanceUntilIdle()
        assertTrue(state().isEmpty)

        vm.undoDelete()
        advanceUntilIdle()

        val restored = state().groups.single().items.single()
        assertEquals(42L, restored.id)
        assertEquals("Passport", restored.name)
        assertNull("undo should not stay armed after use", vm.recentlyDeleted.value)
    }

    @Test
    fun `undo with nothing pending is a no-op`() = runTest {
        val vm = viewModel(entity(1, "Passport", 5))
        val state = latestStateOf(vm)
        advanceUntilIdle()

        vm.undoDelete()
        advanceUntilIdle()

        assertEquals(1, state().groups.single().items.size)
    }

    @Test
    fun `clearing the undo window prevents a later restore`() = runTest {
        val vm = viewModel(entity(1, "Gym", 20))
        val state = latestStateOf(vm)
        advanceUntilIdle()

        vm.delete(state().groups.single().items.single())
        advanceUntilIdle()
        assertNotNull(vm.recentlyDeleted.value)

        vm.clearRecentlyDeleted()
        vm.undoDelete()
        advanceUntilIdle()

        // Once the snackbar is gone the item must stay deleted.
        assertTrue(state().isEmpty)
    }

    @Test
    fun `expired items are grouped last`() = runTest {
        val vm = viewModel(entity(1, "Old inspection", -30), entity(2, "Passport", 10))
        val state = latestStateOf(vm)
        advanceUntilIdle()

        assertEquals(ExpirySection.EXPIRED, state().groups.last().section)
        assertEquals("Old inspection", state().groups.last().items.single().name)
        assertFalse(state().isEmpty)
    }

    @Test
    fun `the banner slot is reserved on the very first frame`() = runTest {
        // Starting false would appear a frame later and shove the list up on every cold start.
        adsState = FakeAdsState(true)
        val vm = viewModel(entity(1, "Passport", 5))
        assertTrue("no advanceUntilIdle: this is the first frame", vm.adsEnabled.value)
    }

    @Test
    fun `the banner follows the ads flag`() = runTest {
        adsState = FakeAdsState(true)
        val vm = viewModel(entity(1, "Passport", 5))
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.adsEnabled.collect {} }
        advanceUntilIdle()

        assertTrue(vm.adsEnabled.value)
    }

    @Test
    fun `a remove-ads purchase hides the banner via the single flag`() = runTest {
        // The seam billing will plug into: flip the flag, the banner and its reserved space go.
        adsState = FakeAdsState(false)
        val vm = viewModel(entity(1, "Passport", 5))
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.adsEnabled.collect {} }
        advanceUntilIdle()

        assertFalse(vm.adsEnabled.value)
    }

    @Test
    fun `an item saved elsewhere appears without a manual refresh`() = runTest {
        val vm = viewModel()
        val state = latestStateOf(vm)
        advanceUntilIdle()
        assertTrue(state().isEmpty)

        // Saved through the repository, not the ViewModel: proves the DB Flow drives the UI,
        // which is what makes returning from the Add screen just work.
        repository.save(
            ItemDraft(
                name = "Car insurance",
                category = Category.INSURANCE,
                expiryDate = today.plusDays(45),
                reminderDaysBefore = listOf(30, 7),
            )
        )
        advanceUntilIdle()

        val added = state().groups.single().items.single()
        assertEquals("Car insurance", added.name)
        assertEquals(ExpirySection.NEXT_3_MONTHS, added.section)
        assertEquals(45, added.daysRemaining)
    }
}
