package dev.randyapps.lapse.ads

/**
 * Ad unit identifiers.
 *
 * One banner unit, and deliberately nothing else. This app has no interstitial, rewarded, or
 * app-open units and must not gain any: a reminder app that interrupts you is worse than no
 * reminder app.
 *
 * The unit id itself is not declared here. It comes from [AdUnits], which exists twice — once in
 * `src/debug` holding Google's test unit, once in `src/release` holding the real one — so the
 * production id is never compiled into a debug build. See either file for why.
 */
object AdIds {

    /**
     * Hashed device ids that UMP treats as test devices, so the debug EEA geography applies.
     * The value is printed by the Ads SDK on first run: look for "Use ... setTestDeviceIds".
     * Debug builds only — it has no effect in release.
     */
    val TEST_DEVICE_HASHED_IDS: List<String> = listOf(
        // Populated at runtime during development; see CLAUDE.md for how to read it.
    )

    /**
     * The unit the banner requests: the test unit in debug, the real one in release, decided at
     * compile time by which source set is in the build.
     */
    val bannerUnitId: String get() = AdUnits.BANNER
}
