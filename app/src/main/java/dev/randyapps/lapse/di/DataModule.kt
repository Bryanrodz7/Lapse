package dev.randyapps.lapse.di

import android.content.Context
import androidx.room.Room
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import androidx.work.WorkManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dev.randyapps.lapse.data.ReminderScheduler
import dev.randyapps.lapse.data.db.ItemDao
import dev.randyapps.lapse.data.db.LapseDatabase
import dev.randyapps.lapse.data.settings.DataStoreSettingsStore
import dev.randyapps.lapse.data.settings.SettingsStore
import dev.randyapps.lapse.notifications.NotificationPermissionStore
import dev.randyapps.lapse.notifications.SharedPrefsNotificationPermissionStore
import dev.randyapps.lapse.notifications.WorkManagerReminderScheduler
import java.time.Clock
import javax.inject.Singleton

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "lapse_settings",
)

@Module
@InstallIn(SingletonComponent::class)
object DataModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): LapseDatabase =
        Room.databaseBuilder(context, LapseDatabase::class.java, LapseDatabase.NAME).build()

    @Provides
    fun provideItemDao(database: LapseDatabase): ItemDao = database.itemDao()

    /**
     * Injected rather than called statically so "today" can be pinned in tests. Every derived
     * status in the app ultimately comes from this clock.
     */
    @Provides
    @Singleton
    fun provideClock(): Clock = Clock.systemDefaultZone()

    @Provides
    @Singleton
    fun provideWorkManager(@ApplicationContext context: Context): WorkManager =
        WorkManager.getInstance(context)

    @Provides
    @Singleton
    fun provideReminderScheduler(
        scheduler: WorkManagerReminderScheduler,
    ): ReminderScheduler = scheduler

    @Provides
    @Singleton
    fun provideSettingsDataStore(@ApplicationContext context: Context): DataStore<Preferences> =
        context.settingsDataStore

    @Provides
    @Singleton
    fun provideSettingsStore(store: DataStoreSettingsStore): SettingsStore = store

    @Provides
    @Singleton
    fun provideNotificationPermissionStore(
        store: SharedPrefsNotificationPermissionStore,
    ): NotificationPermissionStore = store
}
