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

/**
 * Whether the Ads SDK has been initialised, which only happens once UMP consent is resolved.
 * An interface so ViewModels stay JVM-testable; ConsentManager needs a Context.
 */
interface AdsReadiness {
    val adsReady: Flow<Boolean>
}

/**
 * The consent surface Settings needs. An interface for the same reason as the others: the real
 * implementation needs a Context and an Activity.
 */
interface ConsentOptions {
    val privacyOptionsRequired: Flow<Boolean>
    fun showPrivacyOptionsForm(activity: android.app.Activity)
}

@Singleton
class DefaultAdsState @Inject constructor() : AdsState {
    // TODO: read the "remove ads" purchase here once billing exists.
    override val adsEnabled: Flow<Boolean> = flowOf(true)
}
