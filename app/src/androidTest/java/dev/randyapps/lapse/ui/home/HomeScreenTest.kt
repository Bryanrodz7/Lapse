package dev.randyapps.lapse.ui.home

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import dev.randyapps.lapse.data.model.Category
import dev.randyapps.lapse.data.model.Item
import dev.randyapps.lapse.data.model.statusFor
import dev.randyapps.lapse.ui.theme.LapseTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import java.time.Instant
import java.time.LocalDate

/**
 * Guards the swipe/undo round trip at the UI level.
 *
 * The ViewModel tests pass even when this is broken: SwipeToDismissBoxState is saved against
 * the LazyColumn item key, so an item removed by a swipe and then put back under that same key
 * returned still in its dismissed state — present in the list and invisible.
 *
 * The restore is driven directly rather than through the undo snackbar, because the snackbar's
 * timeout races Compose's auto-advancing test clock. What matters here is the list transition,
 * not the snackbar.
 */
class HomeScreenTest {

    @get:Rule
    val rule = createComposeRule()

    private val today = LocalDate.of(2026, 8, 15)

    private fun item(id: Long, name: String, days: Int) = Item(
        id = id,
        name = name,
        category = Category.VEHICLE,
        expiryDate = today.plusDays(days.toLong()),
        reminderDaysBefore = listOf(7),
        note = null,
        photoPath = null,
        createdAt = Instant.EPOCH,
        daysRemaining = days,
        status = statusFor(days),
    )

    private val target = item(1, "Vehicle inspection", 4)
    private val other = item(2, "Passport", 61)

    /** Held outside setContent so the test body can drive the list the way the ViewModel would. */
    private val items = mutableStateListOf<Item>()

    private fun rowsNamed(name: String) =
        rule.onAllNodesWithContentDescription(name, substring = true).fetchSemanticsNodes().size

    private fun setContent() {
        rule.setContent {
            LapseTheme(darkTheme = false) {
                HomeScreen(
                    state = HomeUiState(isLoading = false, groups = groupBySection(items.toList())),
                    recentlyDeleted = null,
                    onAddClick = {},
                    onSettingsClick = {},
                    onItemClick = {},
                    onQuickPick = {},
                    onDelete = { items.remove(it) },
                    onUndoDelete = {},
                    onUndoWindowClosed = {},
                )
            }
        }
    }

    @Test
    fun swipingARowRemovesIt() {
        items.clear()
        items.addAll(listOf(target, other))
        setContent()

        rule.onNodeWithContentDescription("Vehicle inspection", substring = true).assertIsDisplayed()

        rule.onNodeWithContentDescription("Vehicle inspection", substring = true)
            // Inset from the node edges: a swipe ending at x=0 is rejected as out of bounds.
            .performTouchInput {
                swipeLeft(startX = width * 0.9f, endX = width * 0.1f, durationMillis = 200)
            }
        rule.waitForIdle()

        assertEquals(0, rowsNamed("Vehicle inspection"))
        rule.onNodeWithContentDescription("Passport", substring = true).assertIsDisplayed()
    }

    @Test
    fun anItemRestoredUnderTheSameKeyIsVisibleAgain() {
        items.clear()
        items.addAll(listOf(target, other))
        setContent()

        rule.onNodeWithContentDescription("Vehicle inspection", substring = true)
            .performTouchInput {
                swipeLeft(startX = width * 0.9f, endX = width * 0.1f, durationMillis = 200)
            }
        rule.waitForIdle()
        assertEquals("row should be gone after the swipe", 0, rowsNamed("Vehicle inspection"))

        // What undo does: the same item, same id, back into the list.
        rule.runOnUiThread { items.add(0, target) }
        rule.waitForIdle()

        // Without resetting the saved dismiss state this row is in the tree but rendered
        // swiped off-screen, so assertIsDisplayed fails while a mere existence check passes.
        rule.onNodeWithContentDescription("Vehicle inspection", substring = true).assertIsDisplayed()
    }

    @Test
    fun expiredItemsStayCollapsedUntilTheHeaderIsTapped() {
        items.clear()
        items.addAll(listOf(other, item(3, "Boiler service", -12)))
        setContent()

        assertEquals("expired rows must start collapsed", 0, rowsNamed("Boiler service"))

        rule.onNodeWithContentDescription("Expired", substring = true).performClick()
        rule.waitForIdle()

        rule.onNodeWithContentDescription("Boiler service", substring = true).assertIsDisplayed()
    }
}
