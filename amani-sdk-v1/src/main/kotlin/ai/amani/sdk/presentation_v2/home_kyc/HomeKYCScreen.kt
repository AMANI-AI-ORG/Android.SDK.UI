package ai.amani.sdk.presentation_v2.home_kyc

import ai.amani.sdk.presentation_v2.components.AmaniV2Loader
import ai.amani.sdk.presentation_v2.components.DotStep
import ai.amani.sdk.presentation_v2.components.PrimaryButton
import ai.amani.sdk.presentation_v2.components.ScreenHeader
import ai.amani.sdk.presentation_v2.components.StepRow
import ai.amani.sdk.presentation_v2.components.StepRowStatus
import ai.amani.sdk.presentation_v2.components.StepStatus
import ai.amani.sdk.presentation_v2.components.StepError
import ai.amani.sdk.presentation_v2.components.VerificationStep
import ai.amani.sdk.presentation_v2.theme.AmaniV2Dimens
import ai.amani.sdk.presentation_v2.theme.AmaniV2Theme
import ai.amani.sdk.presentation_v2.theme.AmaniV2Type
import ai.amani.sdk.presentation_v2.theme.amaniV2ContentMaxWidth
import ai.amani.sdk.presentation_v2.theme.scaled
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

/**
 * State backing [HomeKYCScreen]. The same screen renders the start, mid-flow and
 * rejected states of the KYC overview — only the data changes.
 */
data class HomeKYCUiState(
    val headerTitle: String,
    val dots: List<DotStep>,
    // Blank/null means "no large heading" and the screen skips rendering it entirely.
    val title: String?,
    val subtitle: String,
    val steps: List<VerificationStep>,
    val primaryButtonText: String,
    // False disables the primary button: there is no step ready for the user right now
    // (e.g. the active step is uploading / awaiting its verdict and the next is locked).
    val primaryButtonEnabled: Boolean = true
)

/**
 * Rendering state for [HomeKYCScreen]. Opens in [Loading] while the SDK fetches
 * GeneralConfigs, then swaps in [Ready] with the config-driven [HomeKYCUiState].
 */
sealed interface HomeKYCScreenState {
    data object Loading : HomeKYCScreenState
    data class Ready(val content: HomeKYCUiState) : HomeKYCScreenState
}

/**
 * KYC overview / stepper screen. Stateless: receives a [HomeKYCScreenState] and emits
 * intents via callbacks. [HomeKYCScreenState.Loading] shows a centered loader on a
 * transparent background; [HomeKYCScreenState.Ready] renders the full content.
 */
@Composable
fun HomeKYCScreen(
    state: HomeKYCScreenState,
    modifier: Modifier = Modifier,
    onBack: () -> Unit = {},
    onPrimary: () -> Unit = {}
) {
    when (state) {
        HomeKYCScreenState.Loading -> AmaniV2Loader(modifier)
        is HomeKYCScreenState.Ready -> HomeKYCContent(
            state = state.content,
            modifier = modifier,
            onBack = onBack,
            onPrimary = onPrimary
        )
    }
}

/** Loaded content of the KYC overview screen — all strings come from [state]. */
@Composable
private fun HomeKYCContent(
    state: HomeKYCUiState,
    modifier: Modifier = Modifier,
    onBack: () -> Unit = {},
    onPrimary: () -> Unit = {}
) {
    val palette = AmaniV2Theme.palette
    val contentMaxWidth = amaniV2ContentMaxWidth()
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(palette.background),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        ScreenHeader(
            title = state.headerTitle,
            steps = state.dots,
            onBack = onBack
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .widthIn(max = contentMaxWidth)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = AmaniV2Dimens.screenPadding)
        ) {
            state.title?.takeIf { it.isNotBlank() }?.let { title ->
                Text(title, style = AmaniV2Type.title.scaled(), color = palette.ink)
                Spacer(Modifier.height(8.dp))
            }
            Text(
                state.subtitle,
                style = AmaniV2Type.body.scaled(),
                color = palette.inkMuted
            )
            Spacer(Modifier.height(24.dp))
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                state.steps.forEach { StepRow(step = it) }
            }
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = contentMaxWidth)
                // Clear the system navigation bar so the button isn't overlapped on devices
                // that draw a bottom nav bar (gesture / 3-button).
                .navigationBarsPadding()
                .padding(horizontal = AmaniV2Dimens.screenPadding)
                .padding(bottom = 20.dp)
        ) {
            PrimaryButton(
                text = state.primaryButtonText,
                enabled = state.primaryButtonEnabled,
                onClick = onPrimary
            )
        }
    }
}

// region Sample states (also used by previews)

internal val SampleHomeKYCStart = HomeKYCUiState(
    headerTitle = "Verification",
    dots = listOf(
        DotStep("ID", StepStatus.Current),
        DotStep("Selfie", StepStatus.Pending),
        DotStep("Address", StepStatus.Pending)
    ),
    title = "Let's get you verified",
    subtitle = "Three quick steps. Should take about 2 minutes.",
    steps = listOf(
        VerificationStep(1, "Upload your ID", "Start here · ~30 sec", StepRowStatus.Active),
        VerificationStep(2, "Take a selfie", "~30 sec", StepRowStatus.Locked),
        VerificationStep(3, "Verify address", "~1 min", StepRowStatus.Locked)
    ),
    primaryButtonText = "Start with ID"
)

internal val SampleHomeKYCMid = HomeKYCUiState(
    headerTitle = "Verification",
    dots = listOf(
        DotStep("ID", StepStatus.Completed),
        DotStep("Selfie", StepStatus.Current),
        DotStep("Address", StepStatus.Pending)
    ),
    title = "You're making progress",
    subtitle = "Two more steps to finish verification.",
    steps = listOf(
        VerificationStep(1, "ID uploaded", "Verified", StepRowStatus.Done),
        VerificationStep(2, "Take a selfie", "Up next · ~30 sec", StepRowStatus.Active),
        VerificationStep(3, "Verify address", "~1 min", StepRowStatus.Locked)
    ),
    primaryButtonText = "Continue with selfie"
)

internal val SampleHomeKYCRejected = HomeKYCUiState(
    headerTitle = "Verification",
    dots = listOf(
        DotStep("ID", StepStatus.Rejected),
        DotStep("Selfie", StepStatus.Pending),
        DotStep("Address", StepStatus.Pending)
    ),
    title = "Verification incomplete",
    subtitle = "One step needs your attention before we can continue.",
    steps = listOf(
        VerificationStep(
            1, "Upload your ID", "Rejected · Action needed", StepRowStatus.Rejected,
            error = StepError(
                title = "Photo too blurry to verify",
                message = "We couldn't read the document clearly. Retake in better lighting and hold steady."
            )
        ),
        VerificationStep(2, "Take a selfie", "~30 sec", StepRowStatus.Locked),
        VerificationStep(3, "Verify address", "~1 min", StepRowStatus.Locked)
    ),
    primaryButtonText = "Retake ID photo"
)

@Preview(name = "HomeKYC — loading", showBackground = true, widthDp = 360, heightDp = 740)
@Composable
private fun PreviewHomeKYCLoading() {
    AmaniV2Theme { HomeKYCScreen(state = HomeKYCScreenState.Loading) }
}

@Preview(name = "HomeKYC — start", showBackground = true, widthDp = 360, heightDp = 740)
@Composable
private fun PreviewHomeKYCStart() {
    AmaniV2Theme { HomeKYCScreen(state = HomeKYCScreenState.Ready(SampleHomeKYCStart)) }
}

@Preview(name = "HomeKYC — mid-flow", showBackground = true, widthDp = 360, heightDp = 740)
@Composable
private fun PreviewHomeKYCMid() {
    AmaniV2Theme { HomeKYCScreen(state = HomeKYCScreenState.Ready(SampleHomeKYCMid)) }
}

@Preview(name = "HomeKYC — rejected", showBackground = true, widthDp = 360, heightDp = 740)
@Composable
private fun PreviewHomeKYCRejected() {
    AmaniV2Theme { HomeKYCScreen(state = HomeKYCScreenState.Ready(SampleHomeKYCRejected)) }
}
