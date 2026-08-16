package dev.randyapps.lapse.data.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.Period

class RenewalTest {

    private val today = LocalDate.of(2026, 8, 15)

    @Test
    fun `a one-year term renews by a year`() {
        val created = LocalDate.of(2025, 8, 15)
        val expiry = LocalDate.of(2026, 8, 15)
        assertEquals(Period.ofYears(1), renewalPeriod(created, expiry))
        assertEquals(LocalDate.of(2027, 8, 15), renewedDate(created, expiry, today))
    }

    @Test
    fun `a ten-year passport renews by ten years, not one`() {
        // The whole point of inferring the term: a passport must not renew annually.
        val created = LocalDate.of(2020, 4, 9)
        val expiry = LocalDate.of(2030, 4, 9)
        assertEquals(Period.ofYears(10), renewalPeriod(created, expiry))
        assertEquals(LocalDate.of(2040, 4, 9), renewedDate(created, expiry, today))
    }

    @Test
    fun `a four-year licence renews on the same calendar day`() {
        // Leap days inside the term must not drift the renewal date.
        val created = LocalDate.of(2022, 3, 1)
        val expiry = LocalDate.of(2026, 3, 1)
        assertEquals(Period.ofYears(4), renewalPeriod(created, expiry))
        assertEquals(LocalDate.of(2030, 3, 1), renewedDate(created, expiry, today))
    }

    @Test
    fun `a six-month term renews in months`() {
        val created = LocalDate.of(2026, 3, 1)
        val expiry = LocalDate.of(2026, 9, 1)
        assertEquals(Period.ofMonths(6), renewalPeriod(created, expiry))
        assertEquals(LocalDate.of(2027, 3, 1), renewedDate(created, expiry, today))
    }

    @Test
    fun `a short term renews in days`() {
        val created = LocalDate.of(2026, 8, 1)
        val expiry = LocalDate.of(2026, 8, 15)
        assertEquals(Period.ofDays(14), renewalPeriod(created, expiry))
        assertEquals(LocalDate.of(2026, 8, 29), renewedDate(created, expiry, today))
    }

    @Test
    fun `an item long expired lands in the future, not on another past date`() {
        // Three annual cycles missed: renewing should skip forward past all of them.
        val created = LocalDate.of(2022, 1, 10)
        val expiry = LocalDate.of(2023, 1, 10)
        val renewed = renewedDate(created, expiry, today)

        assertTrue("renewed date must be in the future", renewed.isAfter(today))
        assertEquals(LocalDate.of(2027, 1, 10), renewed)
    }

    @Test
    fun `an item added on its expiry date falls back to an annual renewal`() {
        // No term to infer, so guessing annually beats guessing zero.
        val sameDay = LocalDate.of(2026, 8, 15)
        assertEquals(Period.ofYears(1), renewalPeriod(sameDay, sameDay))
        assertEquals(LocalDate.of(2027, 8, 15), renewedDate(sameDay, sameDay, today))
    }

    @Test
    fun `an item added after it already expired still renews forward`() {
        val created = LocalDate.of(2026, 8, 10)
        val expiry = LocalDate.of(2026, 8, 1)
        val renewed = renewedDate(created, expiry, today)
        assertTrue(renewed.isAfter(today))
    }

    @Test
    fun `renewing an item expiring today moves it a full term ahead`() {
        val created = LocalDate.of(2025, 8, 15)
        val expiry = today
        assertEquals(LocalDate.of(2027, 8, 15), renewedDate(created, expiry, today))
    }
}
