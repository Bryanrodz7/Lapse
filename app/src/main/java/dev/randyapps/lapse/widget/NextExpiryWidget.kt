package dev.randyapps.lapse.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.text.FontFamily
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import dev.randyapps.lapse.MainActivity
import dev.randyapps.lapse.R
import dev.randyapps.lapse.data.ItemRepository
import dev.randyapps.lapse.data.model.Item
import dev.randyapps.lapse.data.model.ItemStatus
import dev.randyapps.lapse.notifications.Notifications
import kotlin.math.absoluteValue

/**
 * Everything the widget draws, resolved before composition.
 *
 * Strings and the tap intent are prepared by [NextExpiryWidget] rather than read from a
 * composition local, which keeps the composable pure — and testable, since Glance's test harness
 * provides no Android context.
 */
internal data class WidgetContent(
    val headline: String,
    val bigText: String,
    val label: String,
    /** Null when nothing is tracked, which is also what selects the empty layout. */
    val status: ItemStatus?,
)

/**
 * A single glanceable line: the next thing to expire.
 *
 * Reads the database on every update rather than holding anything in memory, so it survives
 * process death and reboot with no cached state to lose.
 *
 * One deliberate deviation from the app: the type is the *system* serif, not Instrument Serif.
 * Widgets render through RemoteViews, which cannot use a bundled font. Rendering the number to a
 * bitmap would allow it, at the cost of re-rendering on every theme and size change; a system
 * serif keeps the family right for a fraction of the complexity.
 */
class NextExpiryWidget : GlanceAppWidget() {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface WidgetEntryPoint {
        fun itemRepository(): ItemRepository
    }

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val repository = EntryPointAccessors
            .fromApplication(context, WidgetEntryPoint::class.java)
            .itemRepository()

        val next = selectWidgetItem(repository.getAllItems())

        val content = next?.let { contentFor(context, it) } ?: emptyContent(context)
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            // Tapping an item opens straight to its edit screen, exactly like a notification.
            if (next != null) putExtra(Notifications.EXTRA_ITEM_ID, next.id)
        }

        provideContent { WidgetBody(content, intent) }
    }
}

/**
 * Picks the one item the widget shows.
 *
 * Anything already expired wins, because a missed renewal is more urgent than an upcoming one —
 * an expired item is shown as expired rather than skipped in favour of the next active one. Among
 * several expired items it is the *most recently* expired that is shown: the gym membership that
 * lapsed three months ago is not what you need reminding about, and leaving it pinned there
 * forever would make the widget useless. With nothing expired, it is simply the soonest upcoming.
 */
internal fun selectWidgetItem(items: List<Item>): Item? {
    val expired = items.filter { it.daysRemaining < 0 }
    return if (expired.isNotEmpty()) {
        expired.maxByOrNull { it.expiryDate }
    } else {
        items.minByOrNull { it.expiryDate }
    }
}

internal fun contentFor(context: Context, item: Item): WidgetContent = WidgetContent(
    headline = item.name,
    // "Today" as a word rather than a bare 0, matching the list and the notifications.
    bigText = if (item.status == ItemStatus.EXPIRES_TODAY) {
        context.getString(R.string.expires_today)
    } else {
        item.daysRemaining.absoluteValue.toString()
    },
    label = daysLabel(context, item),
    status = item.status,
)

internal fun emptyContent(context: Context): WidgetContent = WidgetContent(
    headline = context.getString(R.string.widget_empty_headline),
    bigText = "",
    label = context.getString(R.string.widget_empty_action),
    status = null,
)

/** Same wording and pluralisation as the list, so the two never disagree. */
private fun daysLabel(context: Context, item: Item): String {
    val days = item.daysRemaining
    val label = when {
        item.status == ItemStatus.EXPIRES_TODAY -> context.getString(R.string.expires_today_label)
        days < 0 -> context.resources.getQuantityString(
            R.plurals.days_ago_label, days.absoluteValue, days.absoluteValue,
        )
        else -> context.resources.getQuantityString(R.plurals.days_left_label, days, days)
    }
    return label.uppercase()
}

@Composable
internal fun WidgetBody(content: WidgetContent, intent: Intent) = LapseWidgetTheme {
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(GlanceTheme.colors.background)
            .padding(16.dp)
            .clickable(actionStartActivity(intent)),
        verticalAlignment = Alignment.Vertical.CenterVertically,
    ) {
        Text(
            text = content.headline,
            maxLines = 2,
            style = TextStyle(
                fontFamily = FontFamily.Serif,
                fontSize = if (content.status == null) 16.sp else 15.sp,
                color = GlanceTheme.colors.onBackground,
            ),
        )
        if (content.status != null) {
            Text(
                text = content.bigText,
                style = TextStyle(
                    fontFamily = FontFamily.Serif,
                    fontSize = 34.sp,
                    color = statusColor(content.status),
                ),
            )
        }
        Text(
            text = content.label,
            style = TextStyle(
                fontSize = 12.sp,
                color = GlanceTheme.colors.onSurfaceVariant,
            ),
            modifier = if (content.status == null) {
                GlanceModifier.padding(top = 4.dp)
            } else {
                GlanceModifier
            },
        )
    }
}
