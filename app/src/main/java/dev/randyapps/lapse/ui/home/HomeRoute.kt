package dev.randyapps.lapse.ui.home

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.randyapps.lapse.data.model.Item
import dev.randyapps.lapse.data.model.QuickPick

/**
 * Binds [HomeScreen] to its ViewModel. Kept separate so the screen itself stays stateless and
 * previewable.
 */
@Composable
fun HomeRoute(
    onAddClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onItemClick: (Item) -> Unit,
    onQuickPick: (QuickPick) -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val recentlyDeleted by viewModel.recentlyDeleted.collectAsStateWithLifecycle()
    val adsEnabled by viewModel.adsEnabled.collectAsStateWithLifecycle()
    val adsReady by viewModel.adsReady.collectAsStateWithLifecycle()

    HomeScreen(
        state = state,
        recentlyDeleted = recentlyDeleted,
        onAddClick = onAddClick,
        onSettingsClick = onSettingsClick,
        onItemClick = onItemClick,
        onQuickPick = onQuickPick,
        onDelete = viewModel::delete,
        onUndoDelete = viewModel::undoDelete,
        onUndoWindowClosed = viewModel::clearRecentlyDeleted,
        adsEnabled = adsEnabled,
        adsReady = adsReady,
    )
}
