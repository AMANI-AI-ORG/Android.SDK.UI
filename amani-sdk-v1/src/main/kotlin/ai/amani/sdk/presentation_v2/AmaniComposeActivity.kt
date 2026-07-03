package ai.amani.sdk.presentation_v2

import ai.amani.sdk.model.FeatureConfig
import ai.amani.sdk.model.RegisterConfig
import ai.amani.sdk.presentation_v2.home_kyc.HomeKYCScreen
import ai.amani.sdk.presentation_v2.home_kyc.HomeKYCScreenState
import ai.amani.sdk.presentation_v2.home_kyc.HomeKYCState
import ai.amani.sdk.presentation_v2.home_kyc.HomeKYCViewModel
import ai.amani.sdk.presentation_v2.navigation.AmaniV2NavHost
import ai.amani.sdk.presentation_v2.navigation.rememberAmaniV2Navigator
import ai.amani.sdk.presentation_v2.nfc_scan.NfcIntentHost
import ai.amani.sdk.presentation_v2.theme.AmaniV2Palette
import ai.amani.sdk.presentation_v2.theme.AmaniV2Theme
import ai.amani.sdk.utils.AppConstant
import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.fragment.app.FragmentActivity
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

/**
 * Host activity for the V2 (Jetpack Compose) UI style. Launched by
 * [AmaniSDKUI.goToKycActivity] when the caller selected [ai.amani.sdk.model.UIStyle.V2];
 * the V1 flow keeps using AmaniMainActivity.
 *
 * Shares the same Intent contract as AmaniMainActivity (RegisterConfig + FeatureConfig),
 * the same data/repository layer and core SDK logic. Only the presentation is new.
 *
 * The window is translucent (see Theme.AmaniV2.Transparent): while the SDK fetches
 * GeneralConfigs, the screen renders only a centered loader on a transparent background,
 * so the screen that launched KYC stays visible behind it. Once config resolves, the
 * brand palette is built from the config hex colors and the real, opaque content opens.
 *
 * NOTE: ViewModel/repository wiring and the full screen graph land in the wiring phase.
 * The GeneralConfigs fetch + palette build is stubbed below with a TODO.
 */
// Extends FragmentActivity (not ComponentActivity) so the V2 capture screens can host the
// shared AmaniAi camera Fragment through supportFragmentManager — the same Fragment v1 mounts,
// embedded into Compose via AndroidView (see IdCaptureCameraHost). setContent still works since
// FragmentActivity is a ComponentActivity.
class AmaniComposeActivity : FragmentActivity(), NfcIntentHost {

    private var registerConfig: RegisterConfig? = null
    private var featureConfig: FeatureConfig? = null

    /**
     * Set by the NFC screen while it is armed. NFC foreground dispatch delivers the
     * discovered tag Intent to this activity via [onNewIntent]; we forward it here so the
     * NFC view model can read the chip. Null when no NFC screen is armed.
     */
    override var onNfcIntent: ((Intent) -> Unit)? = null

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // The pending intent for NFC dispatch is SINGLE_TOP, so tags arrive here while the
        // NFC screen is in the foreground; hand them to the registered NFC handler.
        onNfcIntent?.invoke(intent)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // A translucent activity may not fix its orientation on API 26/27 (throws
        // IllegalStateException). Lock portrait on every other level; 26/27 follow the
        // system. Setting it in code (not the manifest) avoids the launch-time crash.
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O ||
            Build.VERSION.SDK_INT > Build.VERSION_CODES.O_MR1
        ) {
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }

        registerConfig = intent.extras?.parcelable(AppConstant.REGISTER_CONFIG)
        featureConfig = intent.extras?.parcelable(AppConstant.FEATURE_CONFIG)

        setContent {
            val viewModel: HomeKYCViewModel = viewModel(factory = HomeKYCViewModel.Factory)

            // Start the login → config → customer-detail load once. The palette and the
            // screen content both come out of this single fetch (see HomeKYCMapper).
            LaunchedEffect(Unit) {
                viewModel.start(this@AmaniComposeActivity, registerConfig, featureConfig)
            }

            val state by viewModel.state.collectAsStateWithLifecycle()

            // While loading, the palette stays at the static defaults and the window is
            // translucent, so only the centered loader shows over the launching screen.
            // Once Ready, the config-driven palette is applied to the whole graph.
            val palette = (state as? HomeKYCState.Ready)?.palette ?: AmaniV2Palette()

            AmaniV2Theme(palette = palette) {
                when (val current = state) {
                    HomeKYCState.Loading -> HomeKYCScreen(state = HomeKYCScreenState.Loading)

                    is HomeKYCState.Ready -> {
                        val navigator = rememberAmaniV2Navigator()
                        AmaniV2NavHost(
                            navigator = navigator,
                            homeContent = current.content,
                            onExit = { finish() },
                            // Capture leg finished (final side confirmed): upload through the
                            // shared SDK layer. The view model marks the matching home step as
                            // processing and listens to AmaniEvents for the verdict.
                            onCaptureLegFinished = { version ->
                                viewModel.uploadStep(this@AmaniComposeActivity, version)
                            },
                            // NFC leg outcome: success uploads ID + NFC together, false the
                            // ID only (v1 HomeKYCFragment withNFC(true/false) → uploadID).
                            onNfcLegFinished = { version, success ->
                                viewModel.finishNfcLeg(this@AmaniComposeActivity, version, success)
                            },
                            // The home primary button starts whatever step is actionable now,
                            // resolved from the view model's live overlays (processing / verdict
                            // / mandatory lock) so it never re-opens an approved step or jumps a
                            // step still locked while another processes.
                            resolveActiveRule = viewModel::resolveActiveRule
                        )
                    }

                    // TODO(wiring): surface the SDK error code to the caller before exit.
                    is HomeKYCState.Failed -> finish()
                }
            }
        }
    }
}

private inline fun <reified T : android.os.Parcelable> Bundle.parcelable(key: String): T? =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        getParcelable(key, T::class.java)
    } else {
        @Suppress("DEPRECATION")
        getParcelable(key)
    }
