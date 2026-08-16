package dev.randyapps.lapse.debug

import android.content.Context
import dev.randyapps.lapse.data.ItemRepository

/**
 * Release-build twin of the debug seeder. Same signature, does nothing — this is what keeps
 * the demo rows out of a shipped build entirely.
 */
object DemoSeed {
    @Suppress("UNUSED_PARAMETER")
    suspend fun seedIfNeeded(context: Context, repository: ItemRepository) = Unit
}
