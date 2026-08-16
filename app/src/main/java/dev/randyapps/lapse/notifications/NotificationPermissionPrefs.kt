package dev.randyapps.lapse.notifications

import android.content.Context
import androidx.core.content.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Remembers whether the notification permission has already been asked for.
 *
 * The prompt appears at the moment the first item is saved — when the value of a reminder is
 * obvious — and never again, whatever the answer. Asking twice is how apps train people to
 * decline reflexively.
 *
 * An interface so the ViewModel stays a plain JVM test subject; the SharedPreferences
 * implementation needs a Context.
 */
interface NotificationPermissionStore {
    var hasAsked: Boolean
}

@Singleton
class SharedPrefsNotificationPermissionStore @Inject constructor(
    @ApplicationContext private val context: Context,
) : NotificationPermissionStore {

    private val prefs by lazy { context.getSharedPreferences(PREFS, Context.MODE_PRIVATE) }

    override var hasAsked: Boolean
        get() = prefs.getBoolean(KEY_ASKED, false)
        set(value) {
            prefs.edit { putBoolean(KEY_ASKED, value) }
        }

    private companion object {
        const val PREFS = "lapse_notifications"
        const val KEY_ASKED = "has_asked_post_notifications"
    }
}
