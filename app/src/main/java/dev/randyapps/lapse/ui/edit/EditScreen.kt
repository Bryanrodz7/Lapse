package dev.randyapps.lapse.ui.edit

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import dev.randyapps.lapse.R
import dev.randyapps.lapse.data.model.Category
import dev.randyapps.lapse.data.model.ItemStatus
import dev.randyapps.lapse.data.model.QuickPick
import dev.randyapps.lapse.ui.ExpiryDateFormat
import dev.randyapps.lapse.ui.QuickPickRow
import dev.randyapps.lapse.ui.theme.DaysNumberStyle
import dev.randyapps.lapse.ui.theme.LocalStatusPalette
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import kotlin.math.absoluteValue

/**
 * One form for both create and edit. Stateless, so it previews in both themes without a
 * ViewModel.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditScreen(
    state: EditUiState,
    onNameChange: (String) -> Unit,
    onCategoryChange: (Category) -> Unit,
    onExpiryDateChange: (LocalDate) -> Unit,
    onToggleReminder: (Int) -> Unit,
    onNoteChange: (String) -> Unit,
    onQuickPick: (QuickPick) -> Unit,
    onRenew: () -> Unit,
    onSave: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showDatePicker by remember { mutableStateOf(false) }
    val nameFocus = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current

    // Under-15-seconds goal: a new item opens with the name field focused and the keyboard up,
    // so the first keystroke needs no taps at all. Editing an existing item does not steal
    // focus, because the user came to change something specific.
    LaunchedEffect(state.ready, state.isNew) {
        if (state.ready && state.isNew) {
            nameFocus.requestFocus()
            keyboard?.show()
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(
                            if (state.isNew) R.string.edit_title_new else R.string.edit_title_edit
                        ),
                        style = MaterialTheme.typography.titleLarge,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.cd_back),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                actions = {
                    // Save lives in the bar so it stays reachable above the keyboard, rather
                    // than at the bottom of a scrolling form.
                    TextButton(onClick = onSave, enabled = state.canSave) {
                        Text(
                            text = stringResource(R.string.action_save),
                            style = MaterialTheme.typography.labelMedium,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                ),
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(horizontal = 20.dp),
        ) {
            if (state.isNew) {
                Spacer(Modifier.height(4.dp))
                FieldLabel(stringResource(R.string.label_quick_add))
                QuickPickRow(onPick = onQuickPick)
                Spacer(Modifier.height(24.dp))
            }

            OutlinedTextField(
                value = state.name,
                onValueChange = onNameChange,
                label = { Text(stringResource(R.string.label_name)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Words,
                    imeAction = ImeAction.Done,
                ),
                textStyle = MaterialTheme.typography.bodyLarge,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(nameFocus),
            )

            Spacer(Modifier.height(28.dp))
            ExpirySummary(state = state, onRenew = onRenew)

            Spacer(Modifier.height(16.dp))
            DateShortcutRow(
                onYears = { years -> onExpiryDateChange(state.today.plusYears(years)) },
                onPickDate = { showDatePicker = true },
            )

            Spacer(Modifier.height(28.dp))
            FieldLabel(stringResource(R.string.label_category))
            CategoryChips(selected = state.category, onSelect = onCategoryChange)

            Spacer(Modifier.height(28.dp))
            FieldLabel(stringResource(R.string.label_remind_me))
            ReminderChips(selected = state.reminderDaysBefore, onToggle = onToggleReminder)

            Spacer(Modifier.height(28.dp))
            OutlinedTextField(
                value = state.note,
                onValueChange = onNoteChange,
                label = { Text(stringResource(R.string.label_note)) },
                minLines = 2,
                textStyle = MaterialTheme.typography.bodyLarge,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                ),
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(48.dp))
        }
    }

    if (showDatePicker) {
        ExpiryDatePickerDialog(
            initial = state.expiryDate,
            onDismiss = { showDatePicker = false },
            onConfirm = { picked ->
                onExpiryDateChange(picked)
                showDatePicker = false
            },
        )
    }
}

/**
 * Mirrors the list's hierarchy: the day count is the hero, the date sits small underneath.
 * Seeing the same treatment while editing makes the consequence of a date change obvious.
 */
@Composable
private fun ExpirySummary(state: EditUiState, onRenew: () -> Unit) {
    val statusColor = LocalStatusPalette.current.colorFor(state.status)
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            FieldLabel(stringResource(R.string.label_expires))
            Text(
                text = state.expiryDate.format(ExpiryDateFormat),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground,
            )
            if (state.canRenew && state.renewalTarget != null) {
                Spacer(Modifier.height(12.dp))
                OutlinedButton(onClick = onRenew) {
                    Text(
                        // Naming the resulting date keeps the one-tap renew from being magic.
                        text = stringResource(
                            R.string.action_renewed_to,
                            state.renewalTarget!!.format(ExpiryDateFormat),
                        ),
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
            }
        }
        Text(
            // Matches the list exactly, including "Today" rather than a bare 0.
            text = if (state.status == ItemStatus.EXPIRES_TODAY) {
                stringResource(R.string.expires_today)
            } else {
                state.daysRemaining.absoluteValue.toString()
            },
            style = DaysNumberStyle,
            color = statusColor,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExpiryDatePickerDialog(
    initial: LocalDate,
    onDismiss: () -> Unit,
    onConfirm: (LocalDate) -> Unit,
) {
    val pickerState = rememberDatePickerState(
        initialSelectedDateMillis = initial.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    pickerState.selectedDateMillis?.let { millis ->
                        // The picker works in UTC millis; convert back on the same zone so the
                        // chosen calendar day is never off by one.
                        onConfirm(
                            Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()
                        )
                    }
                },
            ) { Text(stringResource(R.string.action_ok)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        },
        colors = DatePickerDefaults.colors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        DatePicker(
            state = pickerState,
            colors = DatePickerDefaults.colors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
            ),
        )
    }
}
