package dev.randyapps.lapse.ui.edit

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.randyapps.lapse.R

/** Binds [EditScreen] to its ViewModel and closes the screen once a save completes. */
@Composable
fun EditRoute(
    onClose: () -> Unit,
    viewModel: EditViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    // The system Photo Picker: no media permission required, and the user grants access to
    // exactly the one image they pick.
    val photoLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri -> uri?.let(viewModel::onPhotoPicked) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {
        // Either answer is fine; the item is already saved and reminders simply stay silent
        // if declined.
        viewModel.onNotificationPermissionSettled()
    }

    // Below Android 13 there is no runtime permission, so there is nothing to ask.
    LaunchedEffect(state.askNotificationPermission) {
        if (state.askNotificationPermission && Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            viewModel.onNotificationPermissionSettled()
        }
    }

    LaunchedEffect(state.finished) {
        if (state.finished) onClose()
    }

    EditScreen(
        state = state,
        onNameChange = viewModel::onNameChange,
        onCategoryChange = viewModel::onCategoryChange,
        onExpiryDateChange = viewModel::onExpiryDateChange,
        onToggleReminder = viewModel::onToggleReminder,
        onNoteChange = viewModel::onNoteChange,
        onPickPhoto = {
            photoLauncher.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
            )
        },
        onRemovePhoto = viewModel::onPhotoRemoved,
        onQuickPick = viewModel::onQuickPick,
        onRenew = viewModel::onRenew,
        onSave = viewModel::onSave,
        onClose = onClose,
    )

    if (state.askNotificationPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        // One sentence explaining why, before the system dialog — so the system prompt arrives
        // with context instead of out of nowhere.
        AlertDialog(
            onDismissRequest = { viewModel.onNotificationPermissionSettled() },
            title = {
                Text(
                    text = stringResource(R.string.permission_title),
                    style = MaterialTheme.typography.titleLarge,
                )
            },
            text = {
                Text(
                    text = stringResource(R.string.permission_rationale),
                    style = MaterialTheme.typography.bodyLarge,
                )
            },
            confirmButton = {
                TextButton(
                    onClick = { permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) }
                ) {
                    Text(stringResource(R.string.permission_allow))
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.onNotificationPermissionSettled() }) {
                    Text(stringResource(R.string.permission_not_now))
                }
            },
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        )
    }
}
