package dev.randyapps.lapse.ads

/**
 * Release build's ad units — the real AdMob units. See the debug twin for why this is a
 * source-set split and not a `BuildConfig.DEBUG` branch.
 *
 * The matching AdMob application id lives in the manifest meta-data, which is shared by both
 * builds; that is Google's documented setup, and it is the *unit* id that decides whether a
 * real ad is served.
 */
internal object AdUnits {

    /** Real anchored banner unit, Home screen. The only ad unit this app has or should have. */
    const val BANNER = "ca-app-pub-5316920474106293/5919529714"
}
