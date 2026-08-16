package dev.randyapps.lapse.ui.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.animation.core.tween
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.randyapps.lapse.R
import dev.randyapps.lapse.ads.AnchoredAdBanner
import dev.randyapps.lapse.data.model.ExpirySection
import dev.randyapps.lapse.data.model.Item
import dev.randyapps.lapse.data.model.QuickPick

/**
 * Stateless. Everything it needs arrives as parameters, which is what keeps it previewable in
 * both themes without a ViewModel or a database.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    state: HomeUiState,
    recentlyDeleted: Item?,
    onAddClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onItemClick: (Item) -> Unit,
    onQuickPick: (QuickPick) -> Unit,
    onDelete: (Item) -> Unit,
    onUndoDelete: () -> Unit,
    onUndoWindowClosed: () -> Unit,
    modifier: Modifier = Modifier,
    /**
     * Whether the banner is shown at all. False removes it and the space it reserved, which is
     * the single switch a future "remove ads" purchase flips.
     */
    adsEnabled: Boolean = false,
    adsReady: Boolean = false,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val message = recentlyDeleted?.let { stringResource(R.string.snackbar_deleted, it.name) }
    val undoLabel = stringResource(R.string.action_undo)

    // Swiping deletes immediately and offers undo, rather than asking first.
    LaunchedEffect(recentlyDeleted) {
        if (message == null) return@LaunchedEffect
        val result = snackbarHostState.showSnackbar(
            message = message,
            actionLabel = undoLabel,
            duration = SnackbarDuration.Short,
        )
        if (result == SnackbarResult.ActionPerformed) onUndoDelete() else onUndoWindowClosed()
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.home_title),
                        style = MaterialTheme.typography.titleLarge,
                    )
                },
                actions = {
                    IconButton(onClick = onSettingsClick) {
                        Icon(
                            // Outlined and dimmed: the filled gear out-weighted the serif
                            // wordmark and pulled the eye to the corner.
                            imageVector = Icons.Outlined.Settings,
                            contentDescription = stringResource(R.string.cd_settings),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                ),
            )
        },
        floatingActionButton = {
            // While the list is empty the "Add something" pill is the single obvious action;
            // a FAB beside it would be two calls to action for one job. It fades in with the
            // first item.
            AnimatedVisibility(
                visible = state.groups.isNotEmpty(),
                enter = fadeIn(tween(200)),
                exit = fadeOut(tween(150)),
            ) {
                FloatingActionButton(
                    onClick = onAddClick,
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ) {
                    Icon(
                        Icons.Filled.Add,
                        contentDescription = stringResource(R.string.cd_add_item),
                    )
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        // As the bottomBar, the banner's reserved height flows into the content padding below,
        // so the list scrolls clear of it rather than under it.
        bottomBar = { if (adsEnabled) AnchoredAdBanner(ready = adsReady) },
    ) { padding ->
        when {
            // Blank rather than a spinner: the first read is fast enough that a spinner would
            // only ever be seen as a flash.
            state.isLoading -> Box(Modifier.fillMaxSize())

            state.isEmpty -> EmptyState(
                onAddClick = onAddClick,
                onQuickPick = onQuickPick,
                modifier = Modifier.padding(padding),
            )

            else -> ItemList(
                groups = state.groups,
                contentPadding = padding,
                onItemClick = onItemClick,
                onDelete = onDelete,
            )
        }
    }
}

/** Kept inside the 300ms budget, like every other movement in the app. */
private const val ROW_MOVE_MS = 250

@Composable
private fun ItemList(
    groups: List<ItemGroup>,
    contentPadding: PaddingValues,
    onItemClick: (Item) -> Unit,
    onDelete: (Item) -> Unit,
) {
    val listState = rememberLazyListState()
    var expiredExpanded by remember { mutableStateOf(false) }

    // Position of each visible row across the whole list, so the stagger reads as one sweep
    // rather than restarting at every section. Computed up front because the LazyColumn builder
    // and the row composables run at different times.
    val entryIndex = remember(groups, expiredExpanded) {
        groups
            .filterNot { it.section == ExpirySection.EXPIRED && !expiredExpanded }
            .flatMap { it.items }
            .withIndex()
            .associate { (index, item) -> item.id to index }
    }

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            top = contentPadding.calculateTopPadding(),
            // Clears the FAB so the last row is never trapped underneath it.
            bottom = contentPadding.calculateBottomPadding() + 96.dp,
        ),
    ) {
        groups.forEach { group ->
            val isExpired = group.section == ExpirySection.EXPIRED

            item(key = "header-${group.section.name}") {
                if (isExpired) {
                    ExpiredSectionHeader(
                        count = group.items.size,
                        expanded = expiredExpanded,
                        onToggle = { expiredExpanded = !expiredExpanded },
                    )
                } else {
                    SectionHeader(group.section)
                }
            }

            if (isExpired && !expiredExpanded) return@forEach

            itemsIndexed(group.items) { _, item ->
                // No divider: spacing alone separates rows, consistent with the no-boxes
                // direction. Row padding was increased to carry that.
                //
                // animateItem slides a row to its new position when the sort order changes, so
                // saving an edit that moves an item earlier reads as movement rather than a jump.
                StaggeredEntry(
                    index = entryIndex[item.id] ?: 0,
                    modifier = Modifier.animateItem(
                        placementSpec = tween(ROW_MOVE_MS),
                        fadeInSpec = tween(ROW_MOVE_MS),
                        fadeOutSpec = tween(ROW_MOVE_MS),
                    ),
                ) {
                    SwipeableRow(
                        item = item,
                        onClick = { onItemClick(item) },
                        onDelete = { onDelete(item) },
                    )
                }
            }
        }
    }
}

/** Keyed by item id so a delete animates the right row out and undo returns it in place. */
private fun androidx.compose.foundation.lazy.LazyListScope.itemsIndexed(
    items: List<Item>,
    row: @Composable LazyItemScope.(Int, Item) -> Unit,
) {
    items.forEachIndexed { index, item ->
        item(key = "item-${item.id}") { row(index, item) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeableRow(
    item: Item,
    onClick: () -> Unit,
    onDelete: () -> Unit,
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) {
                onDelete()
                true
            } else {
                false
            }
        },
    )

    // The dismiss state is saved against the LazyColumn item key. Undo restores the item under
    // that same key, so without this the row comes back still swiped off-screen — present in
    // the list and invisible. Snap rather than animate: the row should just be there.
    LaunchedEffect(item.id) {
        if (dismissState.currentValue != SwipeToDismissBoxValue.Settled) {
            dismissState.snapTo(SwipeToDismissBoxValue.Settled)
        }
    }

    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = false,
        backgroundContent = { SwipeToDeleteBackground(Modifier.fillMaxSize()) },
    ) {
        ItemRow(item = item, onClick = onClick)
    }
}
