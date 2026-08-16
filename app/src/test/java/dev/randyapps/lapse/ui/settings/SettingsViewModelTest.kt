package dev.randyapps.lapse.ui.settings

import dev.randyapps.lapse.ads.ConsentOptions
import dev.randyapps.lapse.data.settings.LapseSettings
import dev.randyapps.lapse.data.settings.SettingsStore
import dev.randyapps.lapse.data.settings.ThemeMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
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
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    private val dispatcher = StandardTestDispatcher()

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

    private class FakeConsentOptions(required: Boolean = false) : ConsentOptions {
        override val privacyOptionsRequired = MutableStateFlow(required)
        var formShown = false
        override fun showPrivacyOptionsForm(activity: android.app.Activity) { formShown = true }
    }

    private var consentOptions = FakeConsentOptions()

    private lateinit var store: FakeSettingsStore

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun viewModel(initial: LapseSettings = LapseSettings()): SettingsViewModel {
        store = FakeSettingsStore(initial)
        return SettingsViewModel(store, consentOptions)
    }

    /** Keeps a live collector, since settings is shared with WhileSubscribed. */
    private fun TestScope.latest(vm: SettingsViewModel): () -> LapseSettings {
        var value = vm.settings.value
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            vm.settings.collect { value = it }
        }
        return { value }
    }

    @Test
    fun `starts from the stored settings`() = runTest {
        val vm = viewModel(LapseSettings(defaultReminderDays = listOf(90, 14), themeMode = ThemeMode.DARK))
        val state = latest(vm)
        advanceUntilIdle()

        assertEquals(listOf(90, 14), state().defaultReminderDays)
        assertEquals(ThemeMode.DARK, state().themeMode)
    }

    @Test
    fun `toggling a reminder day off removes it`() = runTest {
        val vm = viewModel(LapseSettings(defaultReminderDays = listOf(30, 7, 1)))
        val state = latest(vm)
        advanceUntilIdle()

        vm.onToggleReminderDay(7)
        advanceUntilIdle()

        assertEquals(listOf(30, 1), state().defaultReminderDays)
    }

    @Test
    fun `toggling a reminder day on adds it, sorted`() = runTest {
        val vm = viewModel(LapseSettings(defaultReminderDays = listOf(30, 7)))
        val state = latest(vm)
        advanceUntilIdle()

        vm.onToggleReminderDay(90)
        advanceUntilIdle()

        assertEquals(listOf(90, 30, 7), state().defaultReminderDays)
    }

    @Test
    fun `every reminder day can be switched off`() = runTest {
        // A valid choice: track the item, stay silent about it.
        val vm = viewModel(LapseSettings(defaultReminderDays = listOf(7)))
        val state = latest(vm)
        advanceUntilIdle()

        vm.onToggleReminderDay(7)
        advanceUntilIdle()

        assertEquals(emptyList<Int>(), state().defaultReminderDays)
    }

    @Test
    fun `the privacy options entry is hidden unless the consent framework requires it`() = runTest {
        consentOptions = FakeConsentOptions(required = false)
        val vm = viewModel()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            vm.privacyOptionsRequired.collect {}
        }
        advanceUntilIdle()

        assertEquals(false, vm.privacyOptionsRequired.value)
    }

    @Test
    fun `the privacy options entry appears when consent was collected`() = runTest {
        // The consent form tells users to look for this in the app, so it must be there.
        consentOptions = FakeConsentOptions(required = true)
        val vm = viewModel()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            vm.privacyOptionsRequired.collect {}
        }
        advanceUntilIdle()

        assertEquals(true, vm.privacyOptionsRequired.value)
    }

    @Test
    fun `theme mode is persisted`() = runTest {
        val vm = viewModel()
        val state = latest(vm)
        advanceUntilIdle()

        vm.onThemeModeChange(ThemeMode.LIGHT)
        advanceUntilIdle()

        assertEquals(ThemeMode.LIGHT, state().themeMode)
    }
}
