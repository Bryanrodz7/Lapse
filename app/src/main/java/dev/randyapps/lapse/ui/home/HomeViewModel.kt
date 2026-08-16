package dev.randyapps.lapse.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.randyapps.lapse.data.ItemRepository
import dev.randyapps.lapse.data.model.Item
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: ItemRepository,
) : ViewModel() {

    val uiState: StateFlow<HomeUiState> = repository.observeItems()
        .map { items -> HomeUiState(isLoading = false, groups = groupBySection(items)) }
        .stateIn(
            scope = viewModelScope,
            // Survives a configuration change without re-reading, but releases the DB flow if
            // the user actually leaves.
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = HomeUiState(),
        )

    private val _recentlyDeleted = MutableStateFlow<Item?>(null)

    /** Non-null while an undo snackbar should be offered. */
    val recentlyDeleted: StateFlow<Item?> = _recentlyDeleted.asStateFlow()

    /**
     * Deletes immediately and holds the item in memory for undo. Deleting straight away is what
     * lets the row animate out without a confirmation dialog; [undoDelete] puts it back with its
     * original id.
     */
    fun delete(item: Item) {
        viewModelScope.launch {
            repository.delete(item.id)
            _recentlyDeleted.value = item
        }
    }

    fun undoDelete() {
        val item = _recentlyDeleted.value ?: return
        viewModelScope.launch {
            repository.restore(item)
            _recentlyDeleted.value = null
        }
    }

    /**
     * Called once the undo window closes, so a stale item can't be restored later. This is also
     * where a deleted item's photo is finally discarded — deleting it at swipe time would make
     * undo restore an item whose photo had already gone.
     */
    fun clearRecentlyDeleted() {
        val item = _recentlyDeleted.value ?: return
        _recentlyDeleted.value = null
        viewModelScope.launch { repository.purgePhotoFor(item) }
    }
}
