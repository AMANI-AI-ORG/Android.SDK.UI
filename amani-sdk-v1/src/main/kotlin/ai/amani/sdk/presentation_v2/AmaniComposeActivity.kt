package ai.amani.sdk.presentation_v2

import ai.amani.sdk.model.FeatureConfig
import ai.amani.sdk.model.RegisterConfig
import ai.amani.sdk.presentation.home_kyc.CachingHomeKYC
import ai.amani.sdk.presentation_v2.approved.ApprovedMapper
import ai.amani.sdk.presentation_v2.approved.ApprovedScreen
import ai.amani.sdk.presentation_v2.home_kyc.HomeKYCEffect
import ai.amani.sdk.presentation_v2.home_kyc.HomeKYCScreen
import ai.amani.sdk.presentation_v2.home_kyc.HomeKYCScreenState
import ai.amani.sdk.presentation_v2.home_kyc.HomeKYCState
import ai.amani.sdk.presentation_v2.home_kyc.HomeKYCViewModel
import ai.amani.sdk.presentation_v2.navigation.AmaniV2Destination
import ai.amani.sdk.presentation_v2.navigation.AmaniV2NavHost
import ai.amani.sdk.presentation_v2.navigation.PreKycFlow
import ai.amani.sdk.presentation_v2.navigation.rememberAmaniV2Navigator
import ai.amani.sdk.presentation_v2.email_otp.EmailOtpRoute
import ai.amani.sdk.presentation_v2.phone_otp.PhoneOtpRoute
import ai.amani.sdk.presentation_v2.profile_info.ProfileInfoRoute
import ai.amani.sdk.presentation_v2.questionnaire.QuestionnaireRoute
import ai.amani.sdk.presentation_v2.nfc_scan.NfcIntentHost
import ai.amani.sdk.presentation_v2.theme.AmaniV2Palette
import ai.amani.sdk.presentation_v2.theme.AmaniV2Theme
import ai.amani.sdk.utils.AppConstant
import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.core.view.WindowCompat
import androidx.fragment.app.FragmentActivity
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import timber.log.Timber

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

    /**
     * Paints the status bar with the config-driven header/toolbar background and matching
     * icon contrast. Called ONLY once GeneralConfigs has resolved into a brand palette —
     * never with the static defaults — so the bar goes straight from the host app's color
     * to the dynamic brand color with no static-fallback flash in between. (The translucent
     * framework theme doesn't set windowDrawsSystemBarBackgrounds, and statusBarColor is
     * ignored without it, hence the flag.)
     */
    private fun applyDynamicStatusBar(background: androidx.compose.ui.graphics.Color) {
        window.addFlags(android.view.WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        window.statusBarColor = background.toArgb()
        WindowCompat.getInsetsController(window, window.decorView)
            .isAppearanceLightStatusBars = background.luminance() > 0.5f
    }

    /**
     * Finishes the flow returning KYCResult APPROVED to the caller — the V2 counterpart of
     * v1 CongratulationsFragment.finishActivity (same Intent contract).
     */
    private fun finishApproved() {
        val returnIntent = Intent()
        returnIntent.putExtra(
            AppConstant.KYC_RESULT,
            ai.amani.sdk.model.KYCResult(profileStatus = ai.amani.sdk.utils.ProfileStatus.APPROVED)
        )
        setResult(RESULT_OK, returnIntent)
        finish()
    }

    /**
     * Finishes the flow after the user closed it themselves (back / exit): an empty
     * `KYCResult` — INCOMPLETE profile status, no error code — exactly what v1 returns from
     * its back-press handler, so the host can tell "user quit" from "SDK failed".
     */
    private fun finishCancelled() {
        val returnIntent = Intent()
        returnIntent.putExtra(AppConstant.KYC_RESULT, ai.amani.sdk.model.KYCResult())
        setResult(RESULT_OK, returnIntent)
        finish()
    }

    /**
     * Finishes the flow reporting WHY it closed: the same `KYCResult` Intent contract v1 uses
     * on its error/exception exits (HomeKYCFragment `Finish.OnError` / `Finish.OnException`),
     * so a host that only ever sees the SDK close still gets the error code and, when the
     * failure carried one, the throwable.
     */
    private fun finishWithError(errorCode: Int, exception: Throwable?) {
        Timber.e("V2 KYC flow failed, errorCode: $errorCode, exception: $exception")
        val returnIntent = Intent()
        returnIntent.putExtra(
            AppConstant.KYC_RESULT,
            ai.amani.sdk.model.KYCResult(
                errorCode = errorCode,
                generalException = exception
            )
        )
        setResult(RESULT_OK, returnIntent)
        finish()
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

            // ProfileApproved is a one-shot effect (fired either right after the initial
            // fetch when everything is already approved, or when the last socket verdict
            // lands); latch it so the success screen survives recompositions.
            val approved = androidx.compose.runtime.saveable.rememberSaveable {
                androidx.compose.runtime.mutableStateOf(false)
            }
            // v1 after-KYC routing: identifier steps configured to run AFTER the KYC steps are
            // shown once they're all approved, before the final approved screen. Holds the
            // current post-KYC step (null = none / done).
            val postKycDest = androidx.compose.runtime.saveable.rememberSaveable {
                androidx.compose.runtime.mutableStateOf<AmaniV2Destination?>(null)
            }
            LaunchedEffect(viewModel) {
                viewModel.effects.collect { effect ->
                    when (effect) {
                        HomeKYCEffect.ProfileApproved -> {
                            val next = PreKycFlow.nextAfterKycStep()
                            if (next != null) postKycDest.value = next else approved.value = true
                        }
                        else -> {}
                    }
                }
            }

            // The brand palette is published by the view model at exactly one point: the
            // GeneralConfigs fetch onComplete. Null until the server values have actually
            // arrived — deriving it from UI state here raced the fetch and could paint
            // before the values existed. The theme falls back to the static defaults for
            // the loader, but the status bar deliberately does NOT (see below).
            val brandPalette by viewModel.brandPalette.collectAsStateWithLifecycle()
            val palette = (state as? HomeKYCState.Ready)?.palette ?: brandPalette ?: AmaniV2Palette()

            // Status bar = toolbar color (GeneralConfigs.topBarBackground via
            // palette.topBar — the same color ScreenHeader paints, so bar + header read as
            // one surface). Painted exclusively via applyDynamicStatusBar and only once
            // the config-driven palette has been published: while loading the bar stays
            // untouched (translucent window, host app still visible), so the static
            // default color never shows on it.
            LaunchedEffect(brandPalette) {
                brandPalette?.let { applyDynamicStatusBar(it.topBar) }
            }

            AmaniV2Theme(palette = palette) {
                if (approved.value) {
                    // Final success screen (v1 CongratulationsFragment): continue/close/back
                    // all return KYCResult APPROVED to the caller and finish.
                    BackHandler { finishApproved() }
                    ApprovedScreen(
                        state = ApprovedMapper.toUiState(
                            general = CachingHomeKYC.appConfig?.generalConfigs,
                            rules = CachingHomeKYC.onlyKYCRules
                        ),
                        onContinue = { finishApproved() }
                    )
                    return@AmaniV2Theme
                }
                val postKyc = postKycDest.value
                if (postKyc != null) {
                    // Post-KYC identifier chain (v1 after-KYC steps): shown standalone here since
                    // the KYC nav host is done. Each step advances to the next post-KYC step, then
                    // to the approved screen. Back exits the SDK (this step is required to finish).
                    BackHandler { finish() }
                    val advancePostKyc: (String) -> Unit = { identifier ->
                        PreKycFlow.markCompleted(identifier)
                        val next = PreKycFlow.nextAfterKycStep()
                        if (next != null) postKycDest.value = next
                        else { postKycDest.value = null; approved.value = true }
                    }
                    when (postKyc) {
                        AmaniV2Destination.Questionnaire -> QuestionnaireRoute(
                            headerTitle = CachingHomeKYC.appConfig?.generalConfigs?.mainTitleText
                                ?: "Verification",
                            onBack = { finish() },
                            onCompleted = { advancePostKyc(AppConstant.IDENTIFIER_QUESTIONNAIRE) }
                        )
                        AmaniV2Destination.ProfileInfo -> ProfileInfoRoute(
                            onBack = { finish() },
                            onCompleted = { advancePostKyc(AppConstant.IDENTIFIER_PROFILE_INFO) }
                        )
                        AmaniV2Destination.PhoneOtp -> PhoneOtpRoute(
                            onBack = { finish() },
                            onCompleted = { advancePostKyc(AppConstant.IDENTIFIER_PHONE_OTP) }
                        )
                        AmaniV2Destination.EmailOtp -> EmailOtpRoute(
                            onBack = { finish() },
                            onCompleted = { advancePostKyc(AppConstant.IDENTIFIER_EMAIL_OTP) }
                        )
                        else -> approved.value = true
                    }
                    return@AmaniV2Theme
                }
                when (val current = state) {
                    HomeKYCState.Loading -> HomeKYCScreen(state = HomeKYCScreenState.Loading)

                    is HomeKYCState.Ready -> {
                        val navigator = rememberAmaniV2Navigator()
                        AmaniV2NavHost(
                            navigator = navigator,
                            homeContent = current.content,
                            onExit = { finishCancelled() },
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
                            // Verify-address leg: photographed document or picked PDF uploads
                            // through the shared document repository (v1 uploadDocument).
                            onAddressLegFinished = { version, flow ->
                                viewModel.uploadAddressStep(this@AmaniComposeActivity, version, flow)
                            },
                            // Speech-verification leg finished: the optional module uploads
                            // its recorded session and the step verdict arrives over the
                            // socket (and the module's own result).
                            onSpeechLegFinished = { version ->
                                viewModel.uploadSpeechStep(this@AmaniComposeActivity, version)
                            },
                            // The home screen owns step selection; resolve the selected
                            // row's rule so the button starts that exact step.
                            resolveRuleById = viewModel::resolveRuleById,
                            // Transient errors (e.g. a failed speech upload) shown in the
                            // host snackbar.
                            snackbarMessages = viewModel.messages,
                            // A before-KYC identifier chain (profile_info / questionnaire) sets its
                            // own AmaniEvent listener; re-attach HomeKYC's when it returns to Home.
                            onReturnToHomeFromPreKyc = { viewModel.reattachAmaniEventListener() }
                        )
                    }

                    is HomeKYCState.Failed ->
                        finishWithError(current.errorCode, current.exception)
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
