package dev.randyapps.lapse.ui.edit

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.randyapps.lapse.data.ItemRepository
import dev.randyapps.lapse.data.model.Category
import dev.randyapps.lapse.data.model.ItemDraft
import dev.randyapps.lapse.data.model.QuickPick
import dev.randyapps.lapse.notifications.NotificationPermissionStore
import dev.randyapps.lapse.ui.nav.EditDestination
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

@HiltViewModel
class EditViewModel @Inject constructor(
    private val repository: ItemRepository,
    private val clock: Clock,
    private val permissionStore: NotificationPermissionStore,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val itemId: Long = savedStateHandle[EditDestination.ARG_ITEM_ID] ?: ItemDraft.NEW_ITEM_ID

    /** Set when the form was opened from a quick pick on the empty state. */
    private val initialQuickPick: QuickPick? =
        savedStateHandle.get<String>(EditDestination.ARG_QUICK_PICK)
            ?.takeIf { it.isNotBlank() }
            ?.let { name -> QuickPick.entries.firstOrNull { it.name == name } }

    private val _state = MutableStateFlow(EditUiState())
    val state: StateFlow<EditUiState> = _state.asStateFlow()

    init {
        val today = LocalDate.now(clock)
        if (itemId == ItemDraft.NEW_ITEM_ID) {
            val base = EditUiState(
                today = today,
                // A year out is the single most common renewal term, so the form opens with a
                // usable date already in place rather than demanding one.
                expiryDate = today.plusYears(1),
                ready = true,
            )
            _state.value = initialQuickPick?.let { pick ->
                val draft = pick.toDraft(today)
                base.copy(
                    name = draft.name,
                    category = draft.category,
                    expiryDate = draft.expiryDate,
                    reminderDaysBefore = draft.reminderDaysBefore,
                )
            } ?: base
        } else {
            loadExisting(today)
        }
    }

    private fun loadExisting(today: LocalDate) {
        viewModelScope.launch {
            val item = repository.getItem(itemId)
            if (item == null) {
                // Deleted from under us — close rather than showing an empty form.
                _state.update { it.copy(today = today, finished = true, ready = true) }
                return@launch
            }
            _state.value = EditUiState(
                itemId = item.id,
                today = today,
                name = item.name,
                category = item.category,
                expiryDate = item.expiryDate,
                reminderDaysBefore = item.reminderDaysBefore,
                note = item.note.orEmpty(),
                createdOn = item.createdAt.atZone(clock.zone ?: ZoneId.systemDefault()).toLocalDate(),
                ready = true,
            )
        }
    }

    fun onNameChange(value: String) = _state.update { it.copy(name = value) }

    fun onCategoryChange(value: Category) = _state.update { it.copy(category = value) }

    fun onExpiryDateChange(value: LocalDate) = _state.update { it.copy(expiryDate = value) }

    fun onNoteChange(value: String) = _state.update { it.copy(note = value) }

    /** Reminder offsets are toggles rather than a picker: one tap on, one tap off. */
    fun onToggleReminder(days: Int) = _state.update { current ->
        val next = if (days in current.reminderDaysBefore) {
            current.reminderDaysBefore - days
        } else {
            current.reminderDaysBefore + days
        }
        current.copy(reminderDaysBefore = next.sortedDescending())
    }

    /** Fills name, category, reminders and a typical date in one tap. */
    fun onQuickPick(pick: QuickPick) = _state.update { current ->
        val draft = pick.toDraft(current.today)
        current.copy(
            name = draft.name,
            category = draft.category,
            expiryDate = draft.expiryDate,
            reminderDaysBefore = draft.reminderDaysBefore,
        )
    }

    /**
     * Moves the expiry forward by the item's inferred term. Applied to the form rather than
     * saved outright, so it stays reviewable and undoable before Save.
     */
    fun onRenew() = _state.update { current ->
        val target = current.renewalTarget ?: return@update current
        current.copy(expiryDate = target)
    }

    fun onSave() {
        val current = _state.value
        if (!current.canSave) return
        viewModelScope.launch {
            repository.save(current.toDraft())
            // Ask for notifications here rather than at launch: the request only makes sense
            // once there is something to be reminded about. Asked once, ever.
            if (!permissionStore.hasAsked) {
                _state.update { it.copy(askNotificationPermission = true) }
            } else {
                _state.update { it.copy(finished = true) }
            }
        }
    }

    /** Called once the permission flow resolves, whatever the answer. */
    fun onNotificationPermissionSettled() {
        permissionStore.hasAsked = true
        _state.update { it.copy(askNotificationPermission = false, finished = true) }
    }
}
