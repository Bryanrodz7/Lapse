package dev.randyapps.lapse.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.randyapps.lapse.R
import dev.randyapps.lapse.data.model.Item
import dev.randyapps.lapse.data.model.ItemStatus
import dev.randyapps.lapse.ui.ExpiryDateFormat
import dev.randyapps.lapse.ui.SpokenDateFormat
import dev.randyapps.lapse.ui.labelRes
import dev.randyapps.lapse.ui.theme.DaysNumberStyle
import dev.randyapps.lapse.ui.theme.ExpiredDaysNumberStyle
import dev.randyapps.lapse.ui.theme.LocalStatusPalette
import kotlin.math.absoluteValue

/**
 * One row: quiet category overline, item name in serif, and the day count as the hero on the
 * right with its date small underneath. No card, no border, no elevation — the status bar at
 * the leading edge is the only ornament.
 */
@Composable
fun ItemRow(
    item: Item,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val statusColor = LocalStatusPalette.current.colorFor(item.status)
    val expired = item.status == ItemStatus.EXPIRED
    // Expired items are present, not nagging: the whole row steps back rather than shouting.
    val nameColor = if (expired) {
        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
    } else {
        MaterialTheme.colorScheme.onBackground
    }
    val metaColor = if (expired) {
        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    // One description for the whole row, so TalkBack reads a sentence instead of five fragments.
    val description = item.spokenDescription()

    Row(
        modifier = modifier
            .fillMaxWidth()
            // Opaque: the row sits on top of the swipe-to-delete background, which would
            // otherwise show through permanently instead of only during a swipe.
            .background(MaterialTheme.colorScheme.background)
            .clickable(onClick = onClick)
            .clearAndSetSemantics { contentDescription = description }
            // Vertical padding carries the whole separation job now that the hairlines are gone.
            .padding(horizontal = 20.dp, vertical = 22.dp)
            .heightIn(min = 56.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        StatusBar(color = statusColor, dimmed = expired)

        Spacer(Modifier.width(16.dp))

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            Text(
                text = stringResource(item.category.labelRes).uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = metaColor,
            )
            Text(
                text = item.name,
                style = MaterialTheme.typography.headlineMedium,
                color = nameColor,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }

        Spacer(Modifier.width(16.dp))

        DaysRemaining(
            item = item,
            color = if (expired) statusColor.copy(alpha = 0.7f) else statusColor,
            metaColor = metaColor,
            expired = expired,
        )
    }
}

/** A thin vertical rule rather than a dot: it gives the row a left edge to hang from. */
@Composable
private fun StatusBar(color: Color, dimmed: Boolean) {
    Box(
        modifier = Modifier
            .size(width = 3.dp, height = 36.dp)
            .clip(RoundedCornerShape(2.dp))
            .background(if (dimmed) color.copy(alpha = 0.4f) else color)
    )
}

@Composable
private fun DaysRemaining(
    item: Item,
    color: Color,
    metaColor: Color,
    expired: Boolean,
) {
    Column(horizontalAlignment = Alignment.End) {
        Text(
            // "Today" as a word, not "0" — a zero reads as nothing left rather than one day left.
            text = if (item.status == ItemStatus.EXPIRES_TODAY) {
                stringResource(R.string.expires_today)
            } else {
                item.daysRemaining.absoluteValue.toString()
            },
            style = if (expired) ExpiredDaysNumberStyle else DaysNumberStyle,
            color = color,
            textAlign = TextAlign.End,
        )
        Text(
            text = daysLabel(item),
            style = MaterialTheme.typography.labelSmall,
            color = metaColor,
            textAlign = TextAlign.End,
        )
        Text(
            // The date is secondary: people think in "how long have I got", not calendar dates.
            text = item.expiryDate.format(ExpiryDateFormat),
            style = MaterialTheme.typography.bodyMedium,
            color = metaColor,
            textAlign = TextAlign.End,
        )
    }
}

@Composable
private fun daysLabel(item: Item): String {
    val days = item.daysRemaining
    val label = when {
        item.status == ItemStatus.EXPIRES_TODAY -> stringResource(R.string.expires_today_label)
        days < 0 -> pluralStringResource(R.plurals.days_ago_label, days.absoluteValue)
        else -> pluralStringResource(R.plurals.days_left_label, days)
    }
    return label.uppercase()
}

@Composable
private fun Item.spokenDescription(): String {
    val category = stringResource(category.labelRes)
    val spokenDate = expiryDate.format(SpokenDateFormat)
    return when {
        status == ItemStatus.EXPIRES_TODAY ->
            stringResource(R.string.cd_item_expires_today, name, category, spokenDate)

        daysRemaining < 0 -> pluralStringResource(
            R.plurals.cd_item_expired_ago,
            daysRemaining.absoluteValue,
            name,
            category,
            daysRemaining.absoluteValue,
            spokenDate,
        )

        else -> pluralStringResource(
            R.plurals.cd_item_expires_in,
            daysRemaining,
            name,
            category,
            daysRemaining,
            spokenDate,
        )
    }
}
