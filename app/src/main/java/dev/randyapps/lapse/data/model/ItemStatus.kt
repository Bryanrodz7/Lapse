package dev.randyapps.lapse.data.model

import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * How close an item is to expiring, ordered least to most urgent.
 *
 * Never persisted. Deriving this at read time is what keeps it honest: an app left closed
 * for a month would otherwise reopen showing a stale ACTIVE badge on something long expired.
 */
enum class ItemStatus {
    ACTIVE,

    /** Inside [SOON_THRESHOLD_DAYS]. Worth knowing about, not worth worrying about. */
    SOON,

    /** Inside [URGENT_THRESHOLD_DAYS]. Act this week. */
    URGENT,

    /**
     * Expires at the end of today. Still valid right now, which is why it is not EXPIRED —
     * a license you can legally use today should not be shown as dead.
     */
    EXPIRES_TODAY,

    /** The date has passed. */
    EXPIRED,
}

const val SOON_THRESHOLD_DAYS = 30
const val URGENT_THRESHOLD_DAYS = 7

/**
 * Whole days from [today] until [expiryDate]. Negative once the date has passed, zero on the
 * expiry date itself.
 *
 * [today] is a parameter rather than a call to `LocalDate.now()` so that every caller shares
 * one notion of "today" within a frame, and so tests don't depend on the system clock.
 */
fun daysRemaining(expiryDate: LocalDate, today: LocalDate): Int =
    ChronoUnit.DAYS.between(today, expiryDate).toInt()

/** Maps [daysRemaining] onto a status. Thresholds are inclusive. */
fun statusFor(daysRemaining: Int): ItemStatus = when {
    daysRemaining < 0 -> ItemStatus.EXPIRED
    daysRemaining == 0 -> ItemStatus.EXPIRES_TODAY
    daysRemaining <= URGENT_THRESHOLD_DAYS -> ItemStatus.URGENT
    daysRemaining <= SOON_THRESHOLD_DAYS -> ItemStatus.SOON
    else -> ItemStatus.ACTIVE
}

/** Convenience for callers holding a date rather than a day count. */
fun statusFor(expiryDate: LocalDate, today: LocalDate): ItemStatus =
    statusFor(daysRemaining(expiryDate, today))
