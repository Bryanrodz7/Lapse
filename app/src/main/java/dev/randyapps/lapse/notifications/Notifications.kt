package dev.randyapps.lapse.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import dev.randyapps.lapse.MainActivity
import dev.randyapps.lapse.R
import dev.randyapps.lapse.data.model.Item

object Notifications {

    const val CHANNEL_ID = "expiry_reminders"
    const val EXTRA_ITEM_ID = "dev.randyapps.lapse.extra.ITEM_ID"

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        if (manager.getNotificationChannel(CHANNEL_ID) != null) return

        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.channel_reminders_name),
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply {
                description = context.getString(R.string.channel_reminders_description)
            }
        )
    }

    fun canPost(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED

    /**
     * Copy names the item and the actual time left — "Your Passport expires in 30 days", never
     * "Reminder: item expiring". Days are recomputed at fire time rather than baked into the
     * schedule, so a notification delayed by Doze still says something true.
     */
    fun titleFor(context: Context, item: Item): String = when {
        item.daysRemaining < 0 -> context.resources.getQuantityString(
            R.plurals.notification_expired_ago,
            -item.daysRemaining,
            item.name,
            -item.daysRemaining,
        )

        item.daysRemaining == 0 -> context.getString(R.string.notification_expires_today, item.name)

        else -> context.resources.getQuantityString(
            R.plurals.notification_expires_in,
            item.daysRemaining,
            item.name,
            item.daysRemaining,
        )
    }

    fun post(context: Context, item: Item) {
        if (!canPost(context)) return
        ensureChannel(context)

        // Tapping opens straight to this item's edit screen, not a generic launch.
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(EXTRA_ITEM_ID, item.id)
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            item.id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(titleFor(context, item))
            .setContentText(context.getString(R.string.notification_body, item.expiryDate))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        // Distinct id per item so several reminders don't overwrite each other.
        NotificationManagerCompat.from(context).notify(item.id.toInt(), notification)
    }
}
