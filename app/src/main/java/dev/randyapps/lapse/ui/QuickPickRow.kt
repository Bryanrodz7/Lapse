package dev.randyapps.lapse.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.randyapps.lapse.data.model.QuickPick

/**
 * The quick picks, shared by the empty state and the add form.
 *
 * Horizontally scrolling rather than wrapped so they stay on one line and don't push the rest
 * of the form below the fold on a small screen.
 */
@Composable
fun QuickPickRow(
    onPick: (QuickPick) -> Unit,
    modifier: Modifier = Modifier,
    edgePadding: Dp = 0.dp,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            // Applied inside the scroll so the first and last chips clear the screen edge
            // without the row itself being inset — chips can still scroll edge to edge.
            .padding(horizontal = edgePadding),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        QuickPick.entries.forEach { pick ->
            SuggestionChip(
                onClick = { onPick(pick) },
                label = { Text(pick.itemName, style = MaterialTheme.typography.labelMedium) },
                colors = SuggestionChipDefaults.suggestionChipColors(
                    labelColor = MaterialTheme.colorScheme.onBackground,
                ),
            )
        }
    }
}
