package dev.randyapps.lapse.ui.home

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.randyapps.lapse.R
import dev.randyapps.lapse.data.model.ExpirySection
import dev.randyapps.lapse.data.model.QuickPick
import dev.randyapps.lapse.ui.QuickPickRow
import dev.randyapps.lapse.ui.labelRes

/** Quiet, wide-tracked, and small. The header should organise the list without competing. */
@Composable
fun SectionHeader(
    section: ExpirySection,
    modifier: Modifier = Modifier,
) {
    Text(
        text = stringResource(section.labelRes).uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 20.dp, top = 28.dp, bottom = 8.dp),
    )
}

/**
 * The expired header doubles as a disclosure control. Expired items collapse by default so
 * they stay present without nagging.
 */
@Composable
fun ExpiredSectionHeader(
    count: Int,
    expanded: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val description = stringResource(
        if (expanded) R.string.cd_expired_section_expanded else R.string.cd_expired_section_collapsed,
        count,
    )
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle)
            .clearAndSetSemantics { contentDescription = description }
            .padding(start = 20.dp, end = 20.dp, top = 28.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(ExpirySection.EXPIRED.labelRes).uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.width(10.dp))
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
        )
        Spacer(Modifier.weight(1f))
        // A caret would need an icon; a rotating chevron drawn from text keeps this dependency-free.
        Text(
            text = if (expanded) "–" else "+",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/**
 * Fades and lifts each row into place, staggered by position.
 *
 * The stagger is capped so a long list never takes longer than a short one to settle — the
 * whole sequence finishes inside the 300ms budget regardless of item count.
 */
@Composable
fun StaggeredEntry(
    index: Int,
    content: @Composable () -> Unit,
) {
    val delay = (index * 28).coerceAtMost(200)
    // animateFloatAsState starts *at* its target on first composition, so animating straight to
    // 1f would produce no animation at all. Flip a flag after the first frame to give it
    // somewhere to travel from.
    var appeared by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { appeared = true }
    val progress by animateFloatAsState(
        targetValue = if (appeared) 1f else 0f,
        animationSpec = tween(durationMillis = 220, delayMillis = delay),
        label = "row-entry",
    )
    Box(
        modifier = Modifier.graphicsLayer {
            alpha = progress
            translationY = (1f - progress) * 16.dp.toPx()
        }
    ) {
        content()
    }
}

/**
 * Warm and brief, with one obvious action. A serif line carries it; there is no illustration
 * and no grey box.
 */
@Composable
fun EmptyState(
    onAddClick: () -> Unit,
    onQuickPick: (QuickPick) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Puts the block's centre at ~40% of the screen rather than dead centre: true centring
        // reads as low, because the eye takes the optical centre to be above the geometric one.
        // The weights split the *free* space, and the block itself occupies roughly 17% of the
        // height, so a 0.4 top weight would still land near 47%.
        Spacer(Modifier.weight(0.3f))

        Text(
            text = stringResource(R.string.empty_headline),
            style = MaterialTheme.typography.displayMedium,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 36.dp),
        )
        Spacer(Modifier.height(14.dp))
        Text(
            text = stringResource(R.string.empty_body),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 36.dp),
        )
        Spacer(Modifier.height(32.dp))
        Button(
            onClick = onAddClick,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ),
        ) {
            Text(
                text = stringResource(R.string.empty_action),
                style = MaterialTheme.typography.labelMedium,
            )
        }

        Spacer(Modifier.height(36.dp))
        // The fastest possible path to a first item: one tap straight into a pre-filled form.
        // This is the moment the shortcut is worth most, so it sits directly under the primary
        // action rather than being reachable only from inside the form.
        Text(
            text = stringResource(R.string.empty_or_start_with),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 36.dp),
        )
        Spacer(Modifier.height(12.dp))
        // Full width, not inset, so the chips can scroll from edge to edge.
        QuickPickRow(onPick = onQuickPick, edgePadding = 20.dp)

        Spacer(Modifier.weight(0.7f))
    }
}

/**
 * Revealed behind a row being swiped away. Deliberately flat and wordy rather than a red panel
 * with a trash icon — deletion here is undoable, so it shouldn't look like a warning.
 */
@Composable
fun SwipeToDeleteBackground(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 24.dp),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.action_delete).uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
