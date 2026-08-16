package dev.randyapps.lapse.ads

import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.android.gms.ads.MobileAds
import com.google.android.ump.ConsentDebugSettings
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.randyapps.lapse.BuildConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Google's User Messaging Platform consent flow.
 *
 * Order matters and is a legal requirement, not a preference: consent is resolved first, and the
 * Mobile Ads SDK is only initialised once UMP says ads may be requested. Initialising first would
 * mean an ad request could go out before the user had been asked.
 *
 * Declining does not remove ads — it produces non-personalised ones. UMP records the choice and
 * persists it, so the form is not shown again on later launches unless consent expires or the
 * user resets it.
 */
@Singleton
class ConsentManager @Inject constructor(
    @ApplicationContext private val context: Context,
) : AdsReadiness, ConsentOptions {

    private val consentInformation: ConsentInformation =
        UserMessagingPlatform.getConsentInformation(context)

    private val _adsReady = MutableStateFlow(false)

    /** True once the Mobile Ads SDK has been initialised and a banner may be requested. */
    override val adsReady: StateFlow<Boolean> = _adsReady.asStateFlow()

    // MobileAds.initialize is not idempotent-safe to call repeatedly across rotations.
    private val initialized = AtomicBoolean(false)
    private val scope = CoroutineScope(Dispatchers.IO)

    /**
     * Called from the Activity, because UMP needs one to present the form. Safe to call on every
     * create; the SDK caches the consent state.
     */
    fun gatherConsent(activity: Activity) {
        val params = ConsentRequestParameters.Builder()
            .setConsentDebugSettings(debugSettings(activity))
            .build()

        consentInformation.requestConsentInfoUpdate(
            activity,
            params,
            {
                // Shows the form only where one is required; otherwise returns immediately.
                UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) { formError ->
                    if (formError != null) {
                        Log.w(TAG, "Consent form error: ${formError.errorCode} ${formError.message}")
                    }
                    refreshPrivacyOptions()
                    initializeAdsIfAllowed()
                }
            },
            { requestError ->
                // A failed consent lookup must not silently enable ads.
                Log.w(TAG, "Consent info failed: ${requestError.errorCode} ${requestError.message}")
                initializeAdsIfAllowed()
            },
        )

        // Returning users whose consent is already on file can start immediately, without
        // waiting for the network round trip above.
        refreshPrivacyOptions()
        initializeAdsIfAllowed()
    }

    private fun initializeAdsIfAllowed() {
        if (!consentInformation.canRequestAds()) return
        if (!initialized.compareAndSet(false, true)) {
            _adsReady.value = true
            return
        }
        // Initialisation does disk and network I/O, so it must stay off the main thread.
        scope.launch {
            MobileAds.initialize(context) {
                _adsReady.value = true
            }
        }
    }

    private fun debugSettings(context: Context): ConsentDebugSettings? {
        if (!BuildConfig.DEBUG) return null
        // Forces the EEA form on a test device so the flow can actually be exercised outside
        // Europe. Has no effect on a release build or on a non-test device.
        val builder = ConsentDebugSettings.Builder(context)
            .setDebugGeography(ConsentDebugSettings.DebugGeography.DEBUG_GEOGRAPHY_EEA)
        AdIds.TEST_DEVICE_HASHED_IDS.forEach(builder::addTestDeviceHashedId)
        return builder.build()
    }

    private val _privacyOptionsRequired = MutableStateFlow(false)

    /**
     * Whether the app must offer a way back into the consent form.
     *
     * Under the TCF this is not optional where consent was collected: the form itself tells the
     * user to look for it in the app, so Settings shows an entry whenever this is true.
     */
    override val privacyOptionsRequired: StateFlow<Boolean> = _privacyOptionsRequired.asStateFlow()

    private fun refreshPrivacyOptions() {
        _privacyOptionsRequired.value = consentInformation.privacyOptionsRequirementStatus ==
            ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED
    }

    /** Reopens the consent form so a choice can be changed or withdrawn. */
    override fun showPrivacyOptionsForm(activity: Activity) {
        UserMessagingPlatform.showPrivacyOptionsForm(activity) { formError ->
            if (formError != null) {
                Log.w(TAG, "Privacy options error: ${formError.errorCode} ${formError.message}")
            }
            refreshPrivacyOptions()
            initializeAdsIfAllowed()
        }
    }

    private companion object {
        const val TAG = "LapseConsent"
    }
}
