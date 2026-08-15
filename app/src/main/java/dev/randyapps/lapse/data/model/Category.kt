package dev.randyapps.lapse.data.model

/**
 * What kind of thing is expiring.
 *
 * Deliberately free of icons and colors: those are presentation concerns and live in the
 * theme layer, so the data layer stays independent of Compose. The mapping from category
 * to icon and accent color arrives with the theme stage.
 */
enum class Category {
    ID_AND_LICENSE,
    VEHICLE,
    INSURANCE,
    HEALTH,
    HOME,
    WORK_AND_CERTS,
    SUBSCRIPTION,
    OTHER,
    ;

    companion object {
        /**
         * Reads a category back from its stored name, falling back to [OTHER] rather than
         * throwing. A row written by a newer build with a category this build doesn't know
         * about should still be readable — losing the icon beats losing the item.
         */
        fun fromName(name: String?): Category =
            entries.firstOrNull { it.name == name } ?: OTHER
    }
}
