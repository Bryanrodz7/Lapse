package dev.randyapps.lapse.ads

/**
 * Debug build's ad units — Google's public test units, and a release twin holding the real ones.
 *
 * This is a source-set split rather than a `BuildConfig.DEBUG` branch for the same reason
 * [dev.randyapps.lapse.debug.DemoSeed] is: a runtime check compiles *both* ids into *both*
 * builds. Here that difference matters more than it does for demo rows. Serving a real ad
 * against a development build — and clicking it — is invalid traffic, and the usual outcome is
 * the AdMob account being suspended. With the split, the production unit id does not exist in
 * the debug APK at all, so no code path, flag, or future refactor can reach it.
 */
internal object AdUnits {

    /** Google's public test banner unit. */
    const val BANNER = "ca-app-pub-3940256099942544/9214589741"
}
