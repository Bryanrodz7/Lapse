package dev.randyapps.lapse.ui.edit

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import dev.randyapps.lapse.data.model.Category
import dev.randyapps.lapse.ui.theme.LapseTheme
import java.time.LocalDate

private val PreviewToday: LocalDate = LocalDate.of(2026, 8, 15)

private val NewItemState = EditUiState(
    today = PreviewToday,
    expiryDate = PreviewToday.plusYears(1),
    ready = true,
)

private val EditingState = EditUiState(
    itemId = 4,
    today = PreviewToday,
    name = "Passport",
    category = Category.ID_AND_LICENSE,
    expiryDate = PreviewToday.plusDays(61),
    reminderDaysBefore = listOf(180, 90, 30),
    note = "Renew at the county office",
    createdOn = PreviewToday.minusYears(10).plusDays(61),
    ready = true,
)

/** An item close enough to expiry that the one-tap renew is offered. */
private val RenewableState = EditingState.copy(
    name = "Vehicle Registration",
    category = Category.VEHICLE,
    expiryDate = PreviewToday.plusDays(3),
    createdOn = PreviewToday.minusYears(1).plusDays(3),
    reminderDaysBefore = listOf(30, 7, 1),
    note = "",
)

@Composable
private fun PreviewEdit(state: EditUiState, dark: Boolean) {
    LapseTheme(darkTheme = dark) {
        EditScreen(
            state = state,
            onNameChange = {},
            onCategoryChange = {},
            onExpiryDateChange = {},
            onToggleReminder = {},
            onNoteChange = {},
            onQuickPick = {},
            onRenew = {},
            onSave = {},
            onClose = {},
        )
    }
}

@Preview(name = "New item - light", showBackground = true, heightDp = 900)
@Composable
private fun NewItemLightPreview() = PreviewEdit(NewItemState, dark = false)

@Preview(
    name = "New item - dark",
    showBackground = true,
    heightDp = 900,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun NewItemDarkPreview() = PreviewEdit(NewItemState, dark = true)

@Preview(name = "Editing - light", showBackground = true, heightDp = 900)
@Composable
private fun EditingLightPreview() = PreviewEdit(EditingState, dark = false)

@Preview(
    name = "Editing - dark",
    showBackground = true,
    heightDp = 900,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun EditingDarkPreview() = PreviewEdit(EditingState, dark = true)

@Preview(name = "Renewable - light", showBackground = true, heightDp = 900)
@Composable
private fun RenewableLightPreview() = PreviewEdit(RenewableState, dark = false)

@Preview(
    name = "Renewable - dark",
    showBackground = true,
    heightDp = 900,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun RenewableDarkPreview() = PreviewEdit(RenewableState, dark = true)

/** Guards the 200% font-scaling requirement on the form. */
@Preview(name = "New item - 200% font", showBackground = true, heightDp = 1400, fontScale = 2.0f)
@Composable
private fun NewItemLargeFontPreview() = PreviewEdit(NewItemState, dark = false)
