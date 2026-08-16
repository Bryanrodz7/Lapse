package dev.randyapps.lapse.ui.home

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import dev.randyapps.lapse.data.model.Category
import dev.randyapps.lapse.data.model.Item
import dev.randyapps.lapse.data.model.statusFor
import dev.randyapps.lapse.ui.theme.LapseTheme
import java.time.Instant
import java.time.LocalDate

private val PreviewToday: LocalDate = LocalDate.of(2026, 8, 15)

private fun previewItem(
    id: Long,
    name: String,
    category: Category,
    days: Int,
): Item = Item(
    id = id,
    name = name,
    category = category,
    expiryDate = PreviewToday.plusDays(days.toLong()),
    reminderDaysBefore = listOf(30, 7),
    note = null,
    photoPath = null,
    createdAt = Instant.EPOCH,
    daysRemaining = days,
    status = statusFor(days),
)

/** Covers every status, including the two that are easy to get wrong: 0 days and expired. */
private val PreviewItems = listOf(
    previewItem(1, "Driver's License", Category.ID_AND_LICENSE, 0),
    previewItem(2, "Vehicle inspection", Category.VEHICLE, 4),
    previewItem(3, "Car insurance", Category.INSURANCE, 23),
    previewItem(4, "Passport", Category.ID_AND_LICENSE, 61),
    previewItem(5, "First aid certificate", Category.WORK_AND_CERTS, 240),
    previewItem(6, "Boiler service", Category.HOME, -12),
    previewItem(7, "Gym membership", Category.SUBSCRIPTION, -95),
)

private val PopulatedState = HomeUiState(
    isLoading = false,
    groups = groupBySection(PreviewItems),
)

@Composable
private fun PreviewHome(state: HomeUiState, dark: Boolean) {
    LapseTheme(darkTheme = dark) {
        HomeScreen(
            state = state,
            recentlyDeleted = null,
            onAddClick = {},
            onSettingsClick = {},
            onItemClick = {},
            onQuickPick = {},
            onDelete = {},
            onUndoDelete = {},
            onUndoWindowClosed = {},
        )
    }
}

@Preview(name = "Home - light", showBackground = true)
@Composable
private fun HomeLightPreview() = PreviewHome(PopulatedState, dark = false)

@Preview(
    name = "Home - dark",
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun HomeDarkPreview() = PreviewHome(PopulatedState, dark = true)

@Preview(name = "Empty - light", showBackground = true)
@Composable
private fun EmptyLightPreview() =
    PreviewHome(HomeUiState(isLoading = false), dark = false)

@Preview(
    name = "Empty - dark",
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun EmptyDarkPreview() =
    PreviewHome(HomeUiState(isLoading = false), dark = true)

/** Guards the 200% font-scaling requirement: this is where fixed row heights would break. */
@Preview(name = "Home - 200% font", showBackground = true, fontScale = 2.0f, heightDp = 900)
@Composable
private fun HomeLargeFontPreview() = PreviewHome(PopulatedState, dark = false)

@Preview(name = "Row - each status", showBackground = true, heightDp = 620)
@Composable
private fun RowStatusPreview() {
    LapseTheme(darkTheme = false) {
        androidx.compose.foundation.layout.Column {
            PreviewItems.forEach { item ->
                ItemRow(item = item, onClick = {})
            }
        }
    }
}

@Preview(
    name = "Row - each status, dark",
    showBackground = true,
    heightDp = 620,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun RowStatusDarkPreview() {
    LapseTheme(darkTheme = true) {
        androidx.compose.foundation.layout.Column {
            PreviewItems.forEach { item ->
                ItemRow(item = item, onClick = {})
            }
        }
    }
}
