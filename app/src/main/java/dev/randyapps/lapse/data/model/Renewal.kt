package dev.randyapps.lapse.data.model

import java.time.LocalDate
import java.time.Period
import java.time.temporal.ChronoUnit
import kotlin.math.roundToLong

/**
 * Works out the date a "Renewed" tap should move an item to.
 *
 * There is no stored renewal term, so the term is inferred from how long the item ran when it
 * was first added — the span from its creation date to its expiry. That is exact for the common
 * case (you add a passport when you get it) and approximate for an item added mid-term, which is
 * why the resulting date is always shown on the button rather than applied silently.
 *
 * The inferred term is then applied repeatedly until the date lands in the future, so renewing
 * something that lapsed three cycles ago still produces a sensible date rather than another
 * past one.
 */
fun renewalPeriod(createdOn: LocalDate, expiryDate: LocalDate): Period {
    val termDays = ChronoUnit.DAYS.between(createdOn, expiryDate)
    return when {
        // Snap near-year terms to whole years so a 4-year licence renews to the same calendar
        // day rather than drifting by the leap-day remainder.
        termDays >= 330 -> Period.ofYears((termDays / 365.25).roundToLong().coerceAtLeast(1).toInt())
        termDays >= 25 -> Period.ofMonths((termDays / 30.44).roundToLong().coerceAtLeast(1).toInt())
        termDays > 0 -> Period.ofDays(termDays.toInt())
        // Added on or after its expiry date: no term to infer, so assume annual.
        else -> Period.ofYears(1)
    }
}

/**
 * The date a renewal moves to: always at least one full term past the current expiry, then
 * further terms until it lands in the future.
 *
 * The first step is unconditional. Renewing something that expires next week means you have
 * renewed it for another term — returning the unchanged date because it happens to still be in
 * the future would make the button do nothing.
 */
fun renewedDate(createdOn: LocalDate, expiryDate: LocalDate, today: LocalDate): LocalDate {
    val period = renewalPeriod(createdOn, expiryDate)
    var next = expiryDate.plus(period)
    // Bounded so a pathological zero-length period can't spin forever.
    var guard = 0
    while (!next.isAfter(today) && guard < MAX_RENEWAL_STEPS) {
        next = next.plus(period)
        guard++
    }
    return if (next.isAfter(today)) next else today.plus(period)
}

private const val MAX_RENEWAL_STEPS = 200
