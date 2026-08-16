package dev.randyapps.lapse.di

import androidx.test.core.app.ApplicationProvider
import androidx.work.Configuration
import androidx.work.testing.SynchronousExecutor
import androidx.work.testing.WorkManagerTestInitHelper
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dev.randyapps.lapse.data.ItemRepository
import dev.randyapps.lapse.data.db.LapseDatabase
import dev.randyapps.lapse.data.model.Category
import dev.randyapps.lapse.data.model.ItemDraft
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertSame
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject

/**
 * Proves the Hilt graph actually builds on device: a DI mistake is invisible at compile time
 * and surfaces as a crash on launch, so it's worth catching before any UI depends on it.
 *
 * Runs against HiltTestApplication rather than LapseApp, so it validates the modules and their
 * wiring. That @HiltAndroidApp is present on LapseApp itself is proven when the app launches.
 */
@HiltAndroidTest
class DependencyGraphTest {

    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    @Inject lateinit var repository: ItemRepository
    @Inject lateinit var database: LapseDatabase
    @Inject lateinit var clock: Clock
    @Inject lateinit var reminderScheduler: dev.randyapps.lapse.data.ReminderScheduler

    // A second injection point for the same bindings, to verify @Singleton actually holds.
    @Inject lateinit var repositoryAgain: ItemRepository
    @Inject lateinit var databaseAgain: LapseDatabase

    @Before
    fun inject() {
        // The manifest removes WorkManager's on-startup initialiser so LapseApp can supply the
        // Hilt worker factory. Instrumented tests run against HiltTestApplication, which is not
        // a Configuration.Provider, so WorkManager has to be initialised here or resolving the
        // graph throws "WorkManager is not initialized properly".
        WorkManagerTestInitHelper.initializeTestWorkManager(
            ApplicationProvider.getApplicationContext(),
            Configuration.Builder().setExecutor(SynchronousExecutor()).build(),
        )
        hiltRule.inject()
    }

    @Test
    fun everyDependencyResolves() {
        assertNotNull(repository)
        assertNotNull(database)
        assertNotNull(clock)
        assertNotNull(reminderScheduler)
    }

    @Test
    fun databaseIsASingleton() {
        // Two Room instances over one file would mean two write paths and stale Flows.
        assertSame(database, databaseAgain)
    }

    @Test
    fun repositoryIsASingleton() {
        assertSame(repository, repositoryAgain)
    }

    @Test
    fun theInjectedRepositoryTalksToTheRealDatabase() = runTest {
        // Exercises the full chain: Hilt -> Room -> converters -> derived status.
        val before = repository.getAllItems().size

        val id = repository.save(
            ItemDraft(
                name = "Graph probe",
                category = Category.OTHER,
                expiryDate = LocalDate.now(clock).plusDays(10),
                reminderDaysBefore = listOf(7),
            )
        )
        try {
            val saved = repository.getItem(id)!!
            assertEquals("Graph probe", saved.name)
            assertEquals(10, saved.daysRemaining)
            assertNotNull(saved.createdAt)
        } finally {
            repository.delete(id)
        }

        // The probe row must not linger in the real on-device database.
        assertEquals(before, repository.getAllItems().size)
    }
}
