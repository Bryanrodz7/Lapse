package dev.randyapps.lapse.ui

import androidx.annotation.StringRes
import dev.randyapps.lapse.R
import dev.randyapps.lapse.data.model.Category
import dev.randyapps.lapse.data.model.ExpirySection
import java.time.format.DateTimeFormatter

/**
 * Presentation labels for data-layer enums. Kept here so the models stay free of Android
 * resource references and remain testable on the JVM.
 */
@get:StringRes
val Category.labelRes: Int
    get() = when (this) {
        Category.ID_AND_LICENSE -> R.string.category_id_and_license
        Category.VEHICLE -> R.string.category_vehicle
        Category.INSURANCE -> R.string.category_insurance
        Category.HEALTH -> R.string.category_health
        Category.HOME -> R.string.category_home
        Category.WORK_AND_CERTS -> R.string.category_work_and_certs
        Category.SUBSCRIPTION -> R.string.category_subscription
        Category.OTHER -> R.string.category_other
    }

@get:StringRes
val ExpirySection.labelRes: Int
    get() = when (this) {
        ExpirySection.THIS_MONTH -> R.string.section_this_month
        ExpirySection.NEXT_3_MONTHS -> R.string.section_next_3_months
        ExpirySection.LATER -> R.string.section_later
        ExpirySection.EXPIRED -> R.string.section_expired
    }

/** "14 Sep 2026" — short enough to sit under the number without competing with it. */
val ExpiryDateFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMM yyyy")

/** Spoken form, since "14 Sep 2026" reads badly aloud. */
val SpokenDateFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMMM yyyy")
