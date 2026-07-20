package ai.amani.sdk.presentation_v2.speech_verify

import ai.amani.sdk.presentation_v2.components.ScreenHeader
import ai.amani.sdk.presentation_v2.id_capture.findFragmentActivity
import ai.amani.sdk.presentation_v2.theme.AmaniV2Theme
import android.view.View
import android.widget.FrameLayout
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.viewinterop.AndroidView
import androidx.fragment.app.FragmentContainerView
import timber.log.Timber

/**
 * Speech-verification screen (`ST` document). Same chrome as the other capture legs — a
 * toolbar over the embedded module beneath it — hosting the standalone AmaniSpeechVerifier
 * fragment inside Compose via a [FragmentContainerView] (the same technique the ID/selfie
 * camera hosts use).
 *
 * This composable references the optional module only through [SpeechVerifierLauncher];
 * callers MUST have confirmed the module is present (see [SpeechVerifierAvailability]) before
 * routing here — the nav host does exactly that.
 *
 * @param headerTitle toolbar title.
 * @param docType the version type (e.g. `XXX_ST_0`) handed to the module.
 * @param onCompleted verification succeeded (all steps passed, video secured) — the host then
 *   triggers the upload and returns to Home.
 */
@Composable
fun SpeechVerifyScreen(
    headerTitle: String,
    docType: String,
    modifier: Modifier = Modifier,
    onBack: () -> Unit = {},
    onCompleted: () -> Unit = {},
    // A non-recoverable module error (recognizer/recording). The host shows it in a snackbar
    // and returns to Home.
    onError: (message: String) -> Unit = {}
) {
    val palette = AmaniV2Theme.palette
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(palette.background)
    ) {
        ScreenHeader(title = headerTitle, onBack = onBack)
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                // Edge-to-edge: end the module above the system navigation bar so its own
                // controls are never overlapped by it (mirrors the camera hosts).
                .navigationBarsPadding()
                .clipToBounds()
        ) {
            if (LocalInspectionMode.current) {
                SpeechPlaceholder(Modifier.fillMaxSize())
            } else {
                SpeechVerifierHost(
                    docType = docType,
                    onCompleted = onCompleted,
                    onError = onError,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

/**
 * Embeds the AmaniSpeechVerifier fragment into Compose: builds it through
 * [SpeechVerifierLauncher] and commits it into a hosted container, torn down with the
 * composable's lifecycle so navigating away (or a retake) removes the module cleanly.
 *
 * The success callback fires once all steps pass; failures are handled inside the module's
 * own retry UI (we only log them here).
 */
@Composable
private fun SpeechVerifierHost(
    docType: String,
    onCompleted: () -> Unit,
    onError: (message: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val activity = LocalContext.current.findFragmentActivity()
    val containerId = rememberSaveable { View.generateViewId() }
    val currentOnCompleted by rememberUpdatedState(onCompleted)
    val currentOnError by rememberUpdatedState(onError)
    // Shown while the module runs a blocking preparation step (e.g. identity-question fetch).
    var isPreparing by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx -> FragmentContainerView(ctx).apply { id = containerId } }
        )
        if (isPreparing) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(AmaniV2Theme.palette.background),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = AmaniV2Theme.palette.loader)
            }
        }
    }

    DisposableEffect(activity, docType) {
        val fm = activity?.supportFragmentManager
        var speechFragment: androidx.fragment.app.Fragment? = null

        if (fm != null) {
            speechFragment = SpeechVerifierLauncher.buildFragment(
                docType = docType,
                onPreparing = { activity.runOnUiThread { isPreparing = true } },
                onReady = { activity.runOnUiThread { isPreparing = false } },
                onSuccess = { activity.runOnUiThread { currentOnCompleted() } },
                onFailure = { reason, attempt ->
                    Timber.d("V2 speech: attempt $attempt failed ($reason)")
                },
                onError = { message ->
                    Timber.e("V2 speech: error — $message")
                    activity.runOnUiThread { currentOnError(message) }
                }
            )
            speechFragment?.let { frag ->
                fm.beginTransaction()
                    .replace(containerId, frag)
                    .commitAllowingStateLoss()
            }
        }

        onDispose {
            val manager = activity?.supportFragmentManager
            val frag = speechFragment
            if (manager != null && frag != null && !manager.isStateSaved) {
                manager.beginTransaction().remove(frag).commitAllowingStateLoss()
            }
        }
    }
}

/** Static stand-in for `@Preview`/inspection where no Activity can host a Fragment. */
@Composable
private fun SpeechPlaceholder(modifier: Modifier = Modifier) {
    Box(modifier = modifier.background(AmaniV2Theme.palette.backgroundWarm))
}
