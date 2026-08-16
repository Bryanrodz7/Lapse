package dev.randyapps.lapse.ads

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.material3.MaterialTheme
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView

/**
 * Adaptive anchored banner, pinned to the bottom of Home and nowhere else.
 *
 * The height is computed before any ad is requested and reserved unconditionally, so the banner
 * cannot push content when it loads or leave a gap when it fails. Used as the Scaffold's
 * bottomBar, which means Home's list gets that height in its content padding automatically and
 * the last row is never covered.
 */
@Composable
fun AnchoredAdBanner(
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val widthDp = LocalConfiguration.current.screenWidthDp

    // A static calculation, no network involved, so the space is known on first composition.
    val adSize = remember(widthDp) { adaptiveAdSize(context, widthDp) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(adSize.height.dp)
            .background(MaterialTheme.colorScheme.background),
    ) {
        // Previews and screenshot tests have no Play Services; reserve the space and stop.
        if (LocalInspectionMode.current) return@Box

        AndroidView(
            modifier = Modifier.fillMaxWidth(),
            factory = { ctx ->
                AdView(ctx).apply {
                    setAdSize(adSize)
                    adUnitId = AdIds.bannerUnitId
                    loadAd(AdRequest.Builder().build())
                }
            },
            // Without this the AdView keeps refreshing after Home leaves composition.
            onRelease = { it.destroy() },
        )
    }
}

private fun adaptiveAdSize(context: Context, widthDp: Int): AdSize =
    AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(context, widthDp)
