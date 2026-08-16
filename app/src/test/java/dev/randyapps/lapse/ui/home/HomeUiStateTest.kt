package dev.randyapps.lapse.ui.home

import dev.randyapps.lapse.data.model.Category
import dev.randyapps.lapse.data.model.ExpirySection
import dev.randyapps.lapse.data.model.Item
import dev.randyapps.lapse.data.model.statusFor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDate

class HomeUiStateTest {

    private val today = LocalDate.of(2026, 8, 15)

    private fun item(name: String, days: Int) = Item(
        id = name.hashCode().toLong(),
        name = name,
        category = Category.OTHER,
        expiryDate = today.plusDays(days.toLong()),
        reminderDaysBefore = listOf(7),
        note = null,
        photoPath = null,
        createdAt = Instant.EPOCH,
        daysRemaining = days,
        status = statusFor(days),
    )

    @Test
    fun `sections appear in display order with expired last`() {
        val groups = groupBySection(
            listOf(
                item("Expired", -10),
                item("This month", 5),
                item("Next 3 months", 60),
                item("Later", 400),
            )
        )
        assertEquals(
            listOf(
                ExpirySection.THIS_MONTH,
                ExpirySection.NEXT_3_MONTHS,
                ExpirySection.LATER,
                ExpirySection.EXPIRED,
            ),
            groups.map { it.section },
        )
    }

    @Test
    fun `empty sections are dropped rather than rendered as bare headers`() {
        val groups = groupBySection(listOf(item("Only one", 3)))
        assertEquals(1, groups.size)
        assertEquals(ExpirySection.THIS_MONTH, groups.single().section)
    }

    @Test
    fun `section boundaries land on the right side`() {
        val groups = groupBySection(
            listOf(
                item("Day 30", 30),
                item("Day 31", 31),
                item("Day 90", 90),
                item("Day 91", 91),
            )
        ).associate { it.section to it.items.map { i -> i.name } }

        assertEquals(listOf("Day 30"), groups[ExpirySection.THIS_MONTH])
        assertEquals(listOf("Day 31", "Day 90"), groups[ExpirySection.NEXT_3_MONTHS])
        assertEquals(listOf("Day 91"), groups[ExpirySection.LATER])
    }

    @Test
    fun `expiring today groups under this month, not expired`() {
        val groups = groupBySection(listOf(item("Today", 0)))
        assertEquals(ExpirySection.THIS_MONTH, groups.single().section)
    }

    @Test
    fun `input order is preserved within a group`() {
        val groups = groupBySection(listOf(item("A", 1), item("B", 2), item("C", 3)))
        assertEquals(listOf("A", "B", "C"), groups.single().items.map { it.name })
    }

    @Test
    fun `empty state only applies after loading finishes`() {
        assertFalse("must not flash the empty state before the first read", HomeUiState().isEmpty)
        assertTrue(HomeUiState(isLoading = false).isEmpty)
        assertFalse(
            HomeUiState(isLoading = false, groups = groupBySection(listOf(item("X", 1)))).isEmpty
        )
    }
}
