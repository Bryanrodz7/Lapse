package dev.randyapps.lapse.data.model

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

/**
 * Boundary coverage for the derived status. Every threshold is checked on both sides, since
 * an off-by-one here is invisible in the UI until the wrong day.
 */
class ItemStatusTest {

    private val today = LocalDate.of(2026, 8, 15)

    private fun statusIn(days: Long) = statusFor(today.plusDays(days), today)

    // --- the boundary the UI cares about most: yesterday / today / tomorrow ---

    @Test
    fun `one day past expiry is EXPIRED`() {
        assertEquals(ItemStatus.EXPIRED, statusIn(-1))
    }

    @Test
    fun `expiring today is EXPIRES_TODAY, not EXPIRED`() {
        // Still legally valid for the rest of the day, so it must not read as dead.
        assertEquals(ItemStatus.EXPIRES_TODAY, statusIn(0))
    }

    @Test
    fun `expiring tomorrow is URGENT, not EXPIRES_TODAY`() {
        assertEquals(ItemStatus.URGENT, statusIn(1))
    }

    // --- URGENT / SOON / ACTIVE thresholds, inclusive ---

    @Test
    fun `seven days out is still URGENT`() {
        assertEquals(ItemStatus.URGENT, statusIn(7))
    }

    @Test
    fun `eight days out is SOON`() {
        assertEquals(ItemStatus.SOON, statusIn(8))
    }

    @Test
    fun `thirty days out is still SOON`() {
        assertEquals(ItemStatus.SOON, statusIn(30))
    }

    @Test
    fun `thirty one days out is ACTIVE`() {
        assertEquals(ItemStatus.ACTIVE, statusIn(31))
    }

    @Test
    fun `far future is ACTIVE`() {
        assertEquals(ItemStatus.ACTIVE, statusIn(3650))
    }

    @Test
    fun `long past is EXPIRED`() {
        assertEquals(ItemStatus.EXPIRED, statusIn(-3650))
    }

    // --- the day count itself ---

    @Test
    fun `daysRemaining is zero on the expiry date`() {
        assertEquals(0, daysRemaining(today, today))
    }

    @Test
    fun `daysRemaining is negative once the date has passed`() {
        assertEquals(-1, daysRemaining(today.minusDays(1), today))
    }

    @Test
    fun `daysRemaining counts whole days forward`() {
        assertEquals(45, daysRemaining(today.plusDays(45), today))
    }

    @Test
    fun `daysRemaining spans a leap day correctly`() {
        // 2028 is a leap year; Feb 28 -> Mar 1 is two days, not one.
        val feb28 = LocalDate.of(2028, 2, 28)
        assertEquals(2, daysRemaining(LocalDate.of(2028, 3, 1), feb28))
    }

    @Test
    fun `status ordering runs least to most urgent`() {
        // Downstream code sorts and compares by this ordinal; lock the order in.
        assertEquals(
            listOf(
                ItemStatus.ACTIVE,
                ItemStatus.SOON,
                ItemStatus.URGENT,
                ItemStatus.EXPIRES_TODAY,
                ItemStatus.EXPIRED,
            ),
            ItemStatus.entries.toList(),
        )
    }
}
