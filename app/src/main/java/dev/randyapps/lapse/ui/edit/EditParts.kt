package dev.randyapps.lapse.ui.edit

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.randyapps.lapse.R
import dev.randyapps.lapse.data.model.Category
import dev.randyapps.lapse.data.model.REMINDER_CHOICES
import dev.randyapps.lapse.ui.labelRes

/** Small, uppercase, wide-tracked — the same quiet label treatment the list uses. */
@Composable
fun FieldLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier.padding(bottom = 10.dp),
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CategoryChips(
    selected: Category,
    onSelect: (Category) -> Unit,
    modifier: Modifier = Modifier,
) {
    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Category.entries.forEach { category ->
            val isSelected = category == selected
            FilterChip(
                selected = isSelected,
                onClick = { onSelect(category) },
                label = {
                    Text(
                        stringResource(category.labelRes),
                        style = MaterialTheme.typography.labelMedium,
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                    labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
            )
        }
    }
}

/** Reminder offsets as toggles: one tap on, one tap off, no dialog. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ReminderChips(
    selected: List<Int>,
    onToggle: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier) {
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            REMINDER_CHOICES.forEach { days ->
                val isSelected = days in selected
                FilterChip(
                    selected = isSelected,
                    onClick = { onToggle(days) },
                    label = {
                        Text(
                            pluralStringResource(R.plurals.reminder_days_before, days, days),
                            style = MaterialTheme.typography.labelMedium,
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                        labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    ),
                )
            }
        }
        if (selected.isEmpty()) {
            Text(
                text = stringResource(R.string.reminders_none_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    }
}

/**
 * Relative date shortcuts. Most renewals are exactly one, two or five years, so these remove
 * the date picker from the common path entirely.
 */
@Composable
fun DateShortcutRow(
    onYears: (Long) -> Unit,
    onPickDate: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        listOf(1L, 2L, 5L).forEach { years ->
            SuggestionChip(
                onClick = { onYears(years) },
                label = {
                    Text(
                        pluralStringResource(R.plurals.years_from_today, years.toInt(), years),
                        style = MaterialTheme.typography.labelMedium,
                    )
                },
                colors = SuggestionChipDefaults.suggestionChipColors(
                    labelColor = MaterialTheme.colorScheme.onBackground,
                ),
            )
        }
        SuggestionChip(
            onClick = onPickDate,
            label = {
                Text(
                    stringResource(R.string.pick_a_date),
                    style = MaterialTheme.typography.labelMedium,
                )
            },
            colors = SuggestionChipDefaults.suggestionChipColors(
                labelColor = MaterialTheme.colorScheme.onBackground,
            ),
        )
    }
}
