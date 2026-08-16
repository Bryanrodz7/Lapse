package dev.randyapps.lapse.data.model

/**
 * The quiet headers Home groups rows under, in display order.
 *
 * [EXPIRED] is last by declaration, not by date: expired items sink to the bottom so they're
 * present without nagging.
 */
enum class ExpirySection {
    THIS_MONTH,
    NEXT_3_MONTHS,
    LATER,
    EXPIRED,
}

/** Upper bound, in days, of [ExpirySection.NEXT_3_MONTHS]. */
const val NEXT_3_MONTHS_DAYS = 90

/**
 * Buckets by rolling day count rather than calendar month. "This month" meaning "within 30
 * days" keeps the header consistent with the SOON status; a calendar month would make an item
 * jump sections overnight on the 1st without anything actually changing.
 */
fun sectionFor(daysRemaining: Int): ExpirySection = when {
    daysRemaining < 0 -> ExpirySection.EXPIRED
    daysRemaining <= SOON_THRESHOLD_DAYS -> ExpirySection.THIS_MONTH
    daysRemaining <= NEXT_3_MONTHS_DAYS -> ExpirySection.NEXT_3_MONTHS
    else -> ExpirySection.LATER
}
