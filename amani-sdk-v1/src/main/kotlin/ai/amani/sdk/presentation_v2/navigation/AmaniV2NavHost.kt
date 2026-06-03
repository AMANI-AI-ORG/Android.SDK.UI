package ai.amani.sdk.presentation_v2.navigation

import ai.amani.sdk.presentation.home_kyc.CachingHomeKYC
import ai.amani.sdk.presentation_v2.home_kyc.HomeKYCScreen
import ai.amani.sdk.presentation_v2.home_kyc.HomeKYCScreenState
import ai.amani.sdk.presentation_v2.home_kyc.HomeKYCUiState
import ai.amani.sdk.presentation_v2.preview_screen.PreviewScreen
import ai.amani.sdk.presentation_v2.id_capture.CaptureMapper
import ai.amani.sdk.presentation_v2.id_capture.IdCaptureBackScreen
import ai.amani.sdk.presentation_v2.id_capture.IdCaptureFrontScreen
import ai.amani.sdk.presentation_v2.select_document_type.SelectDocumentTypeScreen
import ai.amani.sdk.presentation_v2.select_document_type.SelectDocumentTypeMapper
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier

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
    modifier: Modifier = Modifier
) {
    BackHandler {
        if (!navigator.popBackStack()) onExit()
    }

    AnimatedContent(
        targetState = navigator.current,
        // Opaque backdrop for the whole Ready phase. The host window is translucent (so the
        // initial loader can show the launching screen behind it), but once the nav graph is
        // up we paint a solid background here — otherwise the fade in/out during transitions
        // would briefly reveal that launching screen through the translucent window.
        modifier = modifier
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
                onPrimary = { startCaptureFlow(navigator) }
            )

            AmaniV2Destination.DocumentType -> DocumentTypeRoute(
                onBack = { if (!navigator.popBackStack()) onExit() },
                onContinue = { version ->
                    CaptureFlow.initialCaptureFor(version)?.let(navigator::navigateTo)
                }
            )

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
                    // The embedded AmaniAi camera persists the captured frame and hands back
                    // its path; carry it into the confirm/preview step.
                    val onCaptured: (String) -> Unit = { path ->
                        navigator.navigateTo(
                            AmaniV2Destination.CaptureConfirm(
                                versionType = destination.versionType,
                                side = destination.side,
                                imagePath = path
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
                    PreviewScreen(
                        state = CaptureMapper.toPreviewScreenState(
                            version = version,
                            side = destination.side,
                            general = CachingHomeKYC.appConfig?.generalConfigs,
                            imagePath = destination.imagePath
                        ),
                        onBack = { if (!navigator.popBackStack()) onExit() },
                        // The core "skip the back side" rule lives in CaptureFlow: a
                        // two-sided document advances to its back-side capture; anything
                        // else (single-sided, or the back already confirmed) finishes the
                        // leg — hand the version to the host to upload, then pop to Home so
                        // the step shows its uploading/processing spinner there.
                        onConfirm = {
                            val next = CaptureFlow.resolveAfterConfirm(version, destination.side)
                            if (next != null) {
                                navigator.navigateTo(next)
                            } else {
                                onCaptureLegFinished(version)
                                navigator.popToRoot()
                            }
                        },
                        onRetake = { navigator.popBackStack() }
                    )
                }
            }
        }
    }
}

/**
 * Entry into the capture leg from Home, mirroring v1's single-vs-multi version branch
 * (`HomeKYCViewModel.navigateScreen`): prepare the first actionable step's versions and,
 * when only one is selectable, skip the document picker and route straight into capture;
 * otherwise open the [AmaniV2Destination.DocumentType] chooser.
 */
private fun startCaptureFlow(navigator: AmaniV2Navigator) {
    val rule = CaptureFlow.firstActionableRule() ?: return
    CaptureFlow.prepareVersions(rule)
    val visible = CaptureFlow.visibleVersions()
    val single = visible.singleOrNull()
    val directCapture = single?.let(CaptureFlow::initialCaptureFor)
    if (directCapture != null) {
        navigator.navigateTo(directCapture)
    } else {
        navigator.navigateTo(AmaniV2Destination.DocumentType)
    }
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
            config = CachingHomeKYC.appConfig
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
