package dev.randyapps.lapse.ads

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The single flag that decides whether the banner exists.
 *
 * Billing is not implemented. This is the seam a future one-time "remove ads" purchase plugs
 * into: swap the implementation to read the purchase state and every ad in the app disappears,
 * including the space it reserved, with no other change.
 */
interface AdsState {
    val adsEnabled: Flow<Boolean>
}

@Singleton
class DefaultAdsState @Inject constructor() : AdsState {
    // TODO: read the "remove ads" purchase here once billing exists.
    override val adsEnabled: Flow<Boolean> = flowOf(true)
}
