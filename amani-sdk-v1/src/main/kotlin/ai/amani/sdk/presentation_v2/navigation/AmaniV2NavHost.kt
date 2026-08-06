package ai.amani.sdk.presentation_v2.navigation

import ai.amani.sdk.presentation.home_kyc.CachingHomeKYC
import ai.amani.sdk.presentation_v2.address_verify.AddressVerifyMapper
import ai.amani.sdk.presentation_v2.address_verify.AddressVerifyScreen
import ai.amani.sdk.presentation_v2.home_kyc.HomeKYCScreen
import ai.amani.sdk.presentation_v2.home_kyc.HomeKYCScreenState
import ai.amani.sdk.presentation_v2.home_kyc.HomeKYCUiState
import ai.amani.sdk.presentation_v2.preview_screen.PreviewScreen
import ai.amani.sdk.presentation_v2.email_otp.EmailOtpRoute
import ai.amani.sdk.presentation_v2.phone_otp.PhoneOtpRoute
import ai.amani.sdk.presentation_v2.profile_info.ProfileInfoRoute
import ai.amani.sdk.presentation_v2.questionnaire.QuestionnaireRoute
import ai.amani.sdk.utils.AppConstant
import ai.amani.amani_sdk.R
import ai.amani.sdk.presentation_v2.id_capture.CaptureMapper
import ai.amani.sdk.presentation_v2.id_capture.IdCaptureBackScreen
import ai.amani.sdk.presentation_v2.id_capture.IdCaptureFrontScreen
import ai.amani.sdk.presentation_v2.id_capture.IdCaptureGuideScreen
import ai.amani.sdk.presentation_v2.id_capture.idGuideAnimationRes
import ai.amani.sdk.presentation_v2.nfc_scan.NfcScanScreen
import ai.amani.sdk.presentation_v2.nfc_scan.deviceHasNfcHardware
import ai.amani.sdk.presentation_v2.select_document_type.SelectDocumentTypeScreen
import ai.amani.sdk.presentation_v2.select_document_type.SelectDocumentTypeMapper
import ai.amani.sdk.presentation_v2.selfie_capture.SelfieCaptureScreen
import ai.amani.sdk.presentation_v2.selfie_capture.SelfieGuideScreen
import ai.amani.sdk.presentation_v2.selfie_capture.SelfieMapper
import ai.amani.sdk.presentation_v2.signature.SignatureMapper
import ai.amani.sdk.presentation_v2.signature.SignatureScreen
import ai.amani.sdk.presentation_v2.speech_verify.SpeechVerifierAvailability
import ai.amani.sdk.presentation_v2.speech_verify.SpeechVerifyScreen
import ai.amani.sdk.presentation_v2.theme.AmaniV2Theme
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

/**
 * Host for the V2 (Compose) KYC flow. The single place that maps a
 * [AmaniV2Destination] to its screen, so screens stay free of navigation
 * concerns — they only emit intents, and this host turns them into
 * [AmaniV2Navigator] calls (unidirectional flow).
 *
 * Back handling is centralized here: the system back button pops the stack, and
 * once at the root it delegates to [onExit] (finishing the activity). Forward and
 * back moves get a directional slide based on [AmaniV2Navigator.depth].
 *
 * The capture leg (HomeKYC → DocumentType → Capture → CaptureConfirm → …) reuses the
 * shared SDK data layer through [CaptureFlow]; only the live camera/NFC mounting is
 * deferred to the wiring phase (the framed viewport is a placeholder for now).
 */
@Composable
fun AmaniV2NavHost(
    navigator: AmaniV2Navigator,
    homeContent: HomeKYCUiState,
    onExit: () -> Unit,
    // Invoked when the capture leg is fully confirmed (single-sided, or the back side of a
    // two-sided document). The host (which owns the activity + view model) decides whether
    // to upload now or run the NFC leg; this composable just pops back to Home afterwards
    // so the user watches the step upload from the overview.
    onCaptureLegFinished: (datamanager.model.config.Version) -> Unit,
    // Invoked when the NFC leg is over: success = chip read (upload ID + NFC together),
    // false = out of attempts (upload the ID only). The host uploads and this composable
    // pops back to Home so the step shows its processing spinner there.
    onNfcLegFinished: (datamanager.model.config.Version, Boolean) -> Unit,
    // Invoked when the verify-address leg finishes: the flow says whether the document was
    // photographed (DataFromCamera) or picked as a PDF (DataFromGallery). The host uploads
    // through the shared document repository and this composable pops back to Home.
    onAddressLegFinished: (datamanager.model.config.Version, ai.amani.sdk.presentation.physical_contract_screen.GenericDocumentFlow) -> Unit,
    // Invoked when the speech-verification leg succeeds (all passphrase steps passed, the
    // session video secured). The host uploads through SpeechVerifier.upload and this
    // composable pops back to Home so the step shows its processing spinner there.
    onSpeechLegFinished: (datamanager.model.config.Version) -> Unit,
    // Resolves the step the home primary button should start, applying the view model's
    // live overlays (processing / verdict / mandatory lock). Returns null when nothing is
    // actionable right now, so the button is a no-op while a step processes.
    // Resolves a KYC rule by its id — the home screen hands back the selected step's id and
    // we start that exact step.
    resolveRuleById: (String?) -> ai.amani.sdk.model.customer.Rule?,
    // Transient user-facing messages to surface in the host snackbar (e.g. a speech-upload
    // failure emitted by the view model once we're back on Home). Optional.
    snackbarMessages: Flow<String>? = null,
    // Invoked when a before-KYC identifier chain (profile_info / questionnaire / …) finishes and
    // pops back to Home. Pre-KYC screens set their own AmaniEvent listener (v1 parity), so the
    // host re-attaches HomeKYC's listener here before the user starts the KYC steps.
    onReturnToHomeFromPreKyc: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    BackHandler {
        if (!navigator.popBackStack()) onExit()
    }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // Advance the before-KYC identifier chain (v1's step-by-step `getNavDirection`): mark this
    // step done, move to the next pending step, or pop to Home (re-attaching Home's listener)
    // when the chain is complete.
    fun advancePreKyc(identifier: String) {
        PreKycFlow.markCompleted(identifier)
        val next = PreKycFlow.nextBeforeKycStep()
        if (next != null) {
            navigator.replaceCurrent(next)
        } else {
            onReturnToHomeFromPreKyc()
            navigator.popToRoot()
        }
    }
    // Show any message the host pushes (view-model-emitted errors) in the snackbar.
    LaunchedEffect(snackbarMessages) {
        snackbarMessages?.collect { snackbarHostState.showSnackbar(it) }
    }

    Box(modifier = modifier.fillMaxSize()) {
    AnimatedContent(
        targetState = navigator.current,
        // Opaque backdrop for the whole Ready phase. The host window is translucent (so the
        // initial loader can show the launching screen behind it), but once the nav graph is
        // up we paint a solid background here — otherwise the fade in/out during transitions
        // would briefly reveal that launching screen through the translucent window.
        modifier = Modifier
            .fillMaxSize()
            .background(AmaniV2Theme.palette.background),
        transitionSpec = {
            // A forward push slides the new screen in from the right; a back pop slides it
            // in from the left. Direction comes from the navigator (set on the push/pop
            // itself), so back navigation reliably animates as "back".
            val direction = if (navigator.isMovingForward) 1 else -1
            (slideInHorizontally(tween(300)) { full -> direction * full } + fadeIn(tween(300)))
                .togetherWith(
                    slideOutHorizontally(tween(300)) { full -> -direction * full } + fadeOut(tween(300))
                )
        },
        label = "AmaniV2NavHost"
    ) { destination ->
        when (destination) {
            AmaniV2Destination.HomeKYC -> HomeKYCScreen(
                // Config is already resolved before the nav graph starts, so the home
                // screen renders Ready with the config-driven content here; the Loading
                // state is shown by the activity during the initial GeneralConfigs fetch.
                state = HomeKYCScreenState.Ready(homeContent),
                onBack = { if (!navigator.popBackStack()) onExit() },
                // The screen owns the selection; the button hands back the selected step's
                // rule id and we start exactly that step.
                onStartStep = { ruleId ->
                    startCaptureFlowForRule(navigator, resolveRuleById(ruleId))
                }
            )

            AmaniV2Destination.DocumentType -> DocumentTypeRoute(
                onBack = { if (!navigator.popBackStack()) onExit() },
                onContinue = { version ->
                    CaptureFlow.directDestinationFor(version)?.let(navigator::navigateTo)
                }
            )

            AmaniV2Destination.Questionnaire -> QuestionnaireRoute(
                // Config-driven header (GeneralConfigs.mainTitleText), same value the home
                // screen shows; falls back to a generic title upstream.
                headerTitle = homeContent.headerTitle,
                // Back on a before-KYC step exits the SDK (v1 ProfileInfoFragment.finishActivity).
                onBack = onExit,
                onCompleted = { advancePreKyc(AppConstant.IDENTIFIER_QUESTIONNAIRE) }
            )

            AmaniV2Destination.ProfileInfo -> ProfileInfoRoute(
                onBack = onExit,
                onCompleted = { advancePreKyc(AppConstant.IDENTIFIER_PROFILE_INFO) }
            )

            AmaniV2Destination.PhoneOtp -> PhoneOtpRoute(
                onBack = onExit,
                onCompleted = { advancePreKyc(AppConstant.IDENTIFIER_PHONE_OTP) }
            )

            AmaniV2Destination.EmailOtp -> EmailOtpRoute(
                onBack = onExit,
                onCompleted = { advancePreKyc(AppConstant.IDENTIFIER_EMAIL_OTP) }
            )

            is AmaniV2Destination.CaptureGuide -> {
                val version = CaptureFlow.versionByType(destination.versionType)
                if (version == null) {
                    // Stale arg (e.g. config reload) — fall back to the root rather than crash.
                    navigator.popToRoot()
                } else {
                    // Animation is resolved per document type + side (e.g. tur_id_0_front,
                    // tur_dl_0_back), mirroring iOS; it falls back to the generic xx_id_{side}
                    // when the type has no dedicated asset.
                    val isFront = destination.side == CaptureSide.Front
                    IdCaptureGuideScreen(
                        state = CaptureMapper.toGuideState(version = version, side = destination.side),
                        animationRes = idGuideAnimationRes(context, version.type, destination.side),
                        badgeIcon = if (isFront) Icons.Outlined.PhotoCamera else Icons.Filled.Autorenew,
                        onBack = { if (!navigator.popBackStack()) onExit() },
                        // "Open camera" advances to the actual capture for the same side.
                        onContinue = {
                            navigator.navigateTo(
                                AmaniV2Destination.Capture(
                                    versionType = destination.versionType,
                                    side = destination.side
                                )
                            )
                        }
                    )
                }
            }

            is AmaniV2Destination.Capture -> {
                val version = CaptureFlow.versionByType(destination.versionType)
                if (version == null) {
                    // Stale arg (e.g. config reload) — fall back to the root rather than crash.
                    navigator.popToRoot()
                } else {
                    val state = CaptureMapper.toIdCaptureState(
                        version = version,
                        side = destination.side
                    )
                    val onBack = { if (!navigator.popBackStack()) onExit() }
                    // The embedded AmaniAi camera hands the captured frame over in memory
                    // (CapturedFrame); the confirm/preview step reads it from there.
                    val onCaptured: () -> Unit = {
                        navigator.navigateTo(
                            AmaniV2Destination.CaptureConfirm(
                                versionType = destination.versionType,
                                side = destination.side
                            )
                        )
                    }
                    // Front and back are separate screens (like v1's IDCaptureFront/BackSideFrag):
                    // the back leg carries no hologram flag — it forces detection off internally.
                    when (destination.side) {
                        CaptureSide.Front -> IdCaptureFrontScreen(
                            state = state,
                            versionType = destination.versionType,
                            videoRecord = version.videoRecord,
                            hologramDetection = version.hologramDetection,
                            onBack = onBack,
                            onCaptured = onCaptured
                        )

                        CaptureSide.Back -> IdCaptureBackScreen(
                            state = state,
                            versionType = destination.versionType,
                            videoRecord = version.videoRecord,
                            onBack = onBack,
                            onCaptured = onCaptured
                        )
                    }
                }
            }

            is AmaniV2Destination.CaptureConfirm -> {
                val version = CaptureFlow.versionByType(destination.versionType)
                if (version == null) {
                    navigator.popToRoot()
                } else {
                    CaptureConfirmRoute(
                        destination = destination,
                        version = version,
                        navigator = navigator,
                        onExit = onExit,
                        onCaptureLegFinished = onCaptureLegFinished
                    )
                }
            }

            is AmaniV2Destination.NfcScan -> {
                val version = CaptureFlow.versionByType(destination.versionType)
                if (version == null) {
                    navigator.popToRoot()
                } else {
                    NfcScanScreen(
                        version = version,
                        nfcOnly = destination.nfcOnly,
                        mrz = destination.mrz,
                        onBack = { if (!navigator.popBackStack()) onExit() },
                        // Chip read (success) → upload ID + NFC together; out of attempts →
                        // upload the ID only. Either way pop to Home to watch it process.
                        onFinished = { success ->
                            onNfcLegFinished(version, success)
                            navigator.popToRoot()
                        }
                    )
                }
            }

            is AmaniV2Destination.Signature -> {
                val version = CaptureFlow.versionByType(destination.versionType)
                if (version == null) {
                    navigator.popToRoot()
                } else {
                    SignatureScreen(
                        state = SignatureMapper.toUiState(
                            version = version,
                            general = CachingHomeKYC.appConfig?.generalConfigs
                        ),
                        onBack = { if (!navigator.popBackStack()) onExit() },
                        // All required signatures taken: hand the version to the host to
                        // upload through the signature repository, then pop to Home so the
                        // step shows its processing spinner (v1 navigate-home-then-upload).
                        onCompleted = {
                            onCaptureLegFinished(version)
                            navigator.popToRoot()
                        }
                    )
                }
            }

            is AmaniV2Destination.AddressVerify -> {
                val version = CaptureFlow.versionByType(destination.versionType)
                if (version == null) {
                    navigator.popToRoot()
                } else {
                    AddressVerifyScreen(
                        state = AddressVerifyMapper.toUiState(
                            version = version,
                            general = CachingHomeKYC.appConfig?.generalConfigs
                        ),
                        versionType = destination.versionType,
                        onBack = { if (!navigator.popBackStack()) onExit() },
                        // Capture confirmed in the shared document fragment → upload the
                        // photographed document (v1 DataFromCamera hand-off) and pop to Home.
                        onCaptured = {
                            onAddressLegFinished(
                                version,
                                ai.amani.sdk.presentation.physical_contract_screen.GenericDocumentFlow.DataFromCamera
                            )
                            navigator.popToRoot()
                        },
                        // PDF picked from storage → upload it (v1 DataFromGallery hand-off).
                        onPdfPicked = { uri ->
                            onAddressLegFinished(
                                version,
                                ai.amani.sdk.presentation.physical_contract_screen.GenericDocumentFlow.DataFromGallery(arrayListOf(uri))
                            )
                            navigator.popToRoot()
                        }
                    )
                }
            }

            is AmaniV2Destination.SpeechVerify -> {
                val version = CaptureFlow.versionByType(destination.versionType)
                if (version == null) {
                    navigator.popToRoot()
                } else {
                    // The speech verifier is an optional (compileOnly) module. A flow that
                    // reaches here without the artifact on the classpath is a misconfiguration
                    // — fail loudly and actionably instead of crashing deeper in.
                    SpeechVerifierAvailability.requirePresent()
                    SpeechVerifyScreen(
                        headerTitle = homeContent.headerTitle,
                        version = version,
                        onBack = { if (!navigator.popBackStack()) onExit() },
                        // Verification passed + video secured: hand the version to the host to
                        // upload (SpeechVerifier.upload), then pop to Home so the step shows
                        // its processing spinner (same navigate-home-then-upload hand-off).
                        onCompleted = {
                            onSpeechLegFinished(version)
                            navigator.popToRoot()
                        },
                        // Non-recoverable module error: return to Home and surface it in the
                        // host snackbar.
                        onError = { message ->
                            navigator.popToRoot()
                            scope.launch { snackbarHostState.showSnackbar(message) }
                        }
                    )
                }
            }

            is AmaniV2Destination.SelfieGuide -> {
                val version = CaptureFlow.versionByType(destination.versionType)
                if (version == null) {
                    navigator.popToRoot()
                } else {
                    // First step plays animation_first_selfie_instruction; the pose-estimation
                    // flow chains a second step with animation_second_selfie_instruction — the
                    // same assets the legacy v1 selfie screen played.
                    val isFirst = destination.step == SelfieGuideStep.First
                    SelfieGuideScreen(
                        state = SelfieMapper.toGuideState(version = version, step = destination.step),
                        animationRes = if (isFirst) {
                            R.raw.animation_first_selfie_instruction
                        } else {
                            R.raw.animation_second_selfie_instruction
                        },
                        onBack = { if (!navigator.popBackStack()) onExit() },
                        // "Open camera" advances to the next guide step (pose estimation) or
                        // straight to the selfie camera.
                        onContinue = {
                            navigator.navigateTo(CaptureFlow.selfieAfterGuide(version, destination.step))
                        }
                    )
                }
            }

            is AmaniV2Destination.SelfieCapture -> {
                val version = CaptureFlow.versionByType(destination.versionType)
                if (version == null) {
                    navigator.popToRoot()
                } else {
                    SelfieCaptureScreen(
                        state = SelfieMapper.toSelfieCaptureState(version),
                        version = version,
                        onBack = { if (!navigator.popBackStack()) onExit() },
                        // The embedded AmaniAi selfie camera hands the captured frame over
                        // in memory (CapturedFrame); the confirm step reads it from there.
                        onCaptured = {
                            navigator.navigateTo(
                                AmaniV2Destination.SelfieConfirm(
                                    versionType = destination.versionType
                                )
                            )
                        }
                    )
                }
            }

            is AmaniV2Destination.SelfieConfirm -> {
                val version = CaptureFlow.versionByType(destination.versionType)
                if (version == null) {
                    navigator.popToRoot()
                } else {
                    PreviewScreen(
                        state = SelfieMapper.toPreviewScreenState(
                            version = version,
                            general = CachingHomeKYC.appConfig?.generalConfigs,
                            bitmap = ai.amani.sdk.presentation_v2.preview_screen.CapturedFrame.latest
                        ),
                        onBack = { if (!navigator.popBackStack()) onExit() },
                        // Selfies are single-sided: confirming always finishes the leg —
                        // hand the version to the host (which uploads through the selfie SDK
                        // path) and pop to Home so the step shows its processing spinner.
                        onConfirm = {
                            onCaptureLegFinished(version)
                            navigator.popToRoot()
                        },
                        onRetake = { navigator.popBackStack() }
                    )
                }
            }
        }
    }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(16.dp)
        )
    }
}

/**
 * Entry into the capture leg from Home: prepares the selected [rule]'s versions and lets
 * [CaptureFlow.startDestination] decide where to go — the V2 port of v1's
 * `HomeKYCViewModel.navigateScreen` `when (documentID)`: a single selectable document routes
 * straight into its capture screen (Selfie → selfie, ID family → ID capture, …), while
 * several photo-ID documents open the [AmaniV2Destination.DocumentType] chooser. No-op when
 * [rule] is null.
 */
private fun startCaptureFlowForRule(
    navigator: AmaniV2Navigator,
    rule: ai.amani.sdk.model.customer.Rule?
) {
    rule ?: return
    CaptureFlow.prepareVersions(rule)
    CaptureFlow.startDestination()?.let(navigator::navigateTo)
}

/**
 * Document type destination. Maps the prepared step's selectable versions into the
 * stateless [SelectDocumentTypeScreen] (the same server data v1's
 * SelectDocumentTypeFragment renders) and holds the local selection so the user can pick
 * a card and continue. [onContinue] receives the chosen [datamanager.model.config.Version].
 */
@Composable
private fun DocumentTypeRoute(
    onBack: () -> Unit,
    onContinue: (datamanager.model.config.Version) -> Unit,
    modifier: Modifier = Modifier
) {
    val state = remember {
        SelectDocumentTypeMapper.toUiState(
            versions = CaptureFlow.visibleVersions(),
            config = CachingHomeKYC.appConfig,
            // Nav title = the KYC step's name ("Identification"), captured when the
            // capture leg was prepared from Home.
            ruleTitle = CaptureFlow.currentRuleTitle
        )
    }
    var selectedId by rememberSaveable { mutableStateOf(state.selectedId) }

    SelectDocumentTypeScreen(
        state = state.copy(selectedId = selectedId),
        modifier = modifier,
        onBack = onBack,
        onSelect = { selectedId = it.id },
        onContinue = {
            val versionType = selectedId ?: return@SelectDocumentTypeScreen
            CaptureFlow.versionByType(versionType)?.let(onContinue)
        }
    )
}

/**
 * Captured-image confirmation. Beyond the stateless [PreviewScreen] this owns the
 * NFC hand-off: on confirm of the *final* side of an NFC-enabled ID it reads the MRZ off
 * the captured document (v1 PreviewScreenViewModel) — showing a loader *on this screen*
 * meanwhile — and only then navigates to the NFC screen. If the MRZ can't be read it drops
 * back to re-capture (retake) rather than proceeding.
 */
@Composable
private fun CaptureConfirmRoute(
    destination: AmaniV2Destination.CaptureConfirm,
    version: datamanager.model.config.Version,
    navigator: AmaniV2Navigator,
    onExit: () -> Unit,
    onCaptureLegFinished: (datamanager.model.config.Version) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    val nfcRepository = remember { ai.amani.sdk.data.repository.nfc.NFCRepositoryImp() }
    var loadingMrz by remember { mutableStateOf(false) }

    androidx.compose.foundation.layout.Box(modifier.fillMaxSize()) {
        PreviewScreen(
            state = CaptureMapper.toPreviewScreenState(
                version = version,
                side = destination.side,
                general = CachingHomeKYC.appConfig?.generalConfigs,
                bitmap = ai.amani.sdk.presentation_v2.preview_screen.CapturedFrame.latest
            ),
            onBack = { if (!navigator.popBackStack()) onExit() },
            onConfirm = onConfirm@{
                if (loadingMrz) return@onConfirm
                val next = CaptureFlow.resolveAfterConfirm(version, destination.side)
                when {
                    // Another side to capture (front of a two-sided doc) — go there.
                    next != null -> navigator.navigateTo(next)

                    // Final side confirmed and NFC is enabled + available: read the MRZ off
                    // the captured ID *here* (loader shown over this screen). Success → NFC
                    // screen with the MRZ; failure → back to re-capture. (v1 PreviewScreen.)
                    CaptureFlow.isNfcEnabled(version) && deviceHasNfcHardware(context) -> {
                        val type = version.type
                        if (type.isNullOrEmpty()) {
                            onCaptureLegFinished(version)
                            navigator.popToRoot()
                            return@onConfirm
                        }
                        loadingMrz = true
                        nfcRepository.getMRZ(
                            type = type,
                            onComplete = { result ->
                                scope.launch {
                                    loadingMrz = false
                                    val birth = result.mRZBirthDate
                                    val expiry = result.mRZExpiryDate
                                    val docNo = result.mRZDocumentNumber
                                    if (!birth.isNullOrEmpty() && !expiry.isNullOrEmpty() && !docNo.isNullOrEmpty()) {
                                        navigator.navigateTo(
                                            AmaniV2Destination.NfcScan(
                                                versionType = destination.versionType,
                                                mrz = ai.amani.sdk.model.MRZModel(birth, expiry, docNo),
                                                nfcOnly = false
                                            )
                                        )
                                    } else {
                                        // MRZ unreadable → make the user re-capture the side.
                                        navigator.popBackStack()
                                    }
                                }
                            },
                            onError = {
                                scope.launch {
                                    loadingMrz = false
                                    navigator.popBackStack()
                                }
                            }
                        )
                    }

                    // No NFC — finish the leg and upload the ID.
                    else -> {
                        onCaptureLegFinished(version)
                        navigator.popToRoot()
                    }
                }
            },
            onRetake = { if (!loadingMrz) navigator.popBackStack() }
        )

        // MRZ read loader: dim the confirm screen and block interaction while reading.
        if (loadingMrz) {
            androidx.compose.foundation.layout.Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(AmaniV2Theme.palette.ink.copy(alpha = 0.35f)),
                contentAlignment = androidx.compose.ui.Alignment.Center
            ) {
                androidx.compose.material3.CircularProgressIndicator(
                    color = AmaniV2Theme.palette.accent,
                    strokeWidth = 3.dp
                )
            }
        }
    }
}
