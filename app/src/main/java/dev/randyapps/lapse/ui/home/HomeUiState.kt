package dev.randyapps.lapse.ui.home

import dev.randyapps.lapse.data.model.ExpirySection
import dev.randyapps.lapse.data.model.Item

data class ItemGroup(
    val section: ExpirySection,
    val items: List<Item>,
)

data class HomeUiState(
    val isLoading: Boolean = true,
    val groups: List<ItemGroup> = emptyList(),
) {
    /** Distinct from "loading": drives the empty state, which must not flash before the first read. */
    val isEmpty: Boolean get() = !isLoading && groups.isEmpty()
}

/**
 * Buckets items into display order, dropping sections that have nothing in them so Home never
 * renders a header with no rows under it.
 *
 * Input order is preserved, and the DAO already sorts by soonest expiry, so rows stay sorted
 * within each group without sorting again here.
 */
fun groupBySection(items: List<Item>): List<ItemGroup> {
    val bySection = items.groupBy { it.section }
    return ExpirySection.entries.mapNotNull { section ->
        bySection[section]?.takeIf { it.isNotEmpty() }?.let { ItemGroup(section, it) }
    }
}
