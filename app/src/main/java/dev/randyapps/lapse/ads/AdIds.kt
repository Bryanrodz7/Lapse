package dev.randyapps.lapse.ads

/**
 * Ad unit identifiers.
 *
 * One banner unit, and deliberately nothing else. This app has no interstitial, rewarded, or
 * app-open units and must not gain any: a reminder app that interrupts you is worse than no
 * reminder app.
 */
object AdIds {

    /**
     * Google's public test banner unit. Safe to ship in debug; serving real ads against a real
     * unit from a development build risks the AdMob account being flagged for invalid traffic.
     */
    const val TEST_BANNER_UNIT_ID = "ca-app-pub-3940256099942544/9214589741"

    /**
     * TODO: replace with the real AdMob banner unit id before the first Play release.
     * Until then this intentionally holds the test id, so a forgotten swap serves test ads
     * rather than silently failing or violating AdMob policy.
     */
    const val PRODUCTION_BANNER_UNIT_ID = TEST_BANNER_UNIT_ID

    /** Google's public test application id, mirrored in the manifest meta-data. */
    const val TEST_APPLICATION_ID = "ca-app-pub-3940256099942544~3347511713"

    /**
     * TODO: replace with the real AdMob application id, and update the
     * com.google.android.gms.ads.APPLICATION_ID meta-data in AndroidManifest.xml to match.
     */
    const val PRODUCTION_APPLICATION_ID = TEST_APPLICATION_ID

    /**
     * The unit the banner actually requests.
     *
     * Debug builds always use the test unit. Release builds will use the production constant
     * once it is filled in above.
     */
    val bannerUnitId: String
        get() = if (dev.randyapps.lapse.BuildConfig.DEBUG) {
            TEST_BANNER_UNIT_ID
        } else {
            PRODUCTION_BANNER_UNIT_ID
        }
}
