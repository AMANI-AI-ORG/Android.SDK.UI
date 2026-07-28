package ai.amani.sdk.presentation_v2.signature

import ai.amani.sdk.Amani
import ai.amani.sdk.modules.signature.interfaces.ISignatureStartCallBack
import ai.amani.sdk.presentation_v2.components.PrimaryButton
import ai.amani.sdk.presentation_v2.components.ScreenHeader
import ai.amani.sdk.presentation_v2.components.SecondaryButton
import ai.amani.sdk.presentation_v2.id_capture.findFragmentActivity
import ai.amani.sdk.presentation_v2.theme.AmaniV2Dimens
import ai.amani.sdk.presentation_v2.theme.AmaniV2Theme
import ai.amani.sdk.presentation_v2.theme.AmaniV2Type
import ai.amani.sdk.presentation_v2.theme.scaled
import ai.amani.sdk.utils.AppConstant.SIGNATURE_NUMBER
import android.graphics.Bitmap
import android.view.View
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Draw
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentContainerView
import timber.log.Timber

/**
 * State backing [SignatureScreen]. Every string is config-driven with a fallback
 * (see [SignatureMapper]).
 */
data class SignatureUiState(
    val headerTitle: String,
    val instruction: String,
    val confirmButtonText: String,
    val tryAgainButtonText: String
)

/**
 * V2 sign-contract (digital signature) screen — the Compose counterpart of v1's
 * SignatureFragment with the same business flow: the shared AmaniAi signature pad is
 * hosted below the header, "try again" clears the pad (`Signature().clean()`), "confirm"
 * accepts the current stroke (`Signature().confirm()`), and once the required
 * [SIGNATURE_NUMBER] of signatures is taken the leg finishes via [onCompleted] — the host
 * then uploads through the shared [ai.amani.sdk.data.repository.signature.SignatureRepoImp]
 * and pops to Home (exactly v1's navigate-home-then-upload hand-off).
 *
 * In `@Preview`/inspection there is no Activity to host a Fragment, so a framed
 * placeholder stands in for the signature pad.
 */
@Composable
fun SignatureScreen(
    state: SignatureUiState,
    onCompleted: () -> Unit,
    modifier: Modifier = Modifier,
    onBack: () -> Unit = {}
) {
    val palette = AmaniV2Theme.palette
    val context = LocalContext.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(palette.background)
    ) {
        ScreenHeader(title = state.headerTitle, onBack = onBack)

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = AmaniV2Dimens.screenPadding)
        ) {
            Text(
                state.instruction,
                style = AmaniV2Type.bodySmall.scaled(),
                color = palette.inkMuted
            )
            Spacer(Modifier.height(12.dp))
            // Signature pad card: white surface with the soft border/radius the other V2
            // cards use; the AmaniAi pad fragment fills it.
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(AmaniV2Dimens.cardRadius.scaled()))
                    .background(palette.surface)
                    .border(
                        1.dp,
                        palette.border,
                        RoundedCornerShape(AmaniV2Dimens.cardRadius.scaled())
                    )
            ) {
                if (LocalInspectionMode.current) {
                    SignaturePadPlaceholder(Modifier.fillMaxSize())
                } else {
                    SignaturePadHost(
                        onCompleted = onCompleted,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                // Keep the buttons clear of the system navigation bar.
                .navigationBarsPadding()
                .padding(horizontal = AmaniV2Dimens.screenPadding)
                .padding(top = 16.dp, bottom = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(AmaniV2Dimens.gapSm)
        ) {
            // v1 tryAgainButton → Signature().clean(): wipes the pad for a fresh stroke.
            SecondaryButton(
                text = state.tryAgainButtonText,
                leadingIcon = Icons.Outlined.Refresh,
                modifier = Modifier.weight(1f),
                onClick = { runCatching { Amani.sharedInstance().Signature().clean() } }
            )
            // v1 confirmButton → Signature().confirm(): accepts the current signature; the
            // start() callback then fires and finishes the leg once the count is reached.
            PrimaryButton(
                text = state.confirmButtonText,
                modifier = Modifier.weight(1f),
                onClick = { runCatching { Amani.sharedInstance().Signature().confirm(context) } }
            )
        }
    }
}

/**
 * Hosts the shared AmaniAi signature pad Fragment inside Compose (same AndroidView +
 * FragmentContainerView pattern as the V2 camera hosts). Mirrors v1
 * SignatureFragment.initDigitalSignatureFragment: `Signature().start(context, SIGNATURE_NUMBER)`
 * and, when the callback reports the required count, hands control back via [onCompleted].
 * The Fragment is committed/removed with the composable's lifecycle.
 */
@Composable
private fun SignaturePadHost(
    onCompleted: () -> Unit,
    modifier: Modifier = Modifier
) {
    val activity = LocalContext.current.findFragmentActivity()
    val containerId = rememberSaveable { View.generateViewId() }
    val currentOnCompleted by rememberUpdatedState(onCompleted)

    AndroidView(
        modifier = modifier,
        factory = { ctx -> FragmentContainerView(ctx).apply { id = containerId } }
    )

    DisposableEffect(activity) {
        val fm = activity?.supportFragmentManager
        var padFragment: Fragment? = null

        if (fm != null && activity != null) {
            padFragment = Amani.sharedInstance().Signature().start(
                activity,
                SIGNATURE_NUMBER,
                object : ISignatureStartCallBack {
                    override fun cb(bitmap: Bitmap?, countOfSignature: Int) {
                        if (bitmap != null && countOfSignature == SIGNATURE_NUMBER) {
                            // All required signatures taken (v1 navigateHomeScreen()).
                            Timber.i("V2 signature: all required signatures taken")
                            activity.runOnUiThread { currentOnCompleted() }
                        }
                    }
                }
            )

            padFragment?.let { frag ->
                fm.beginTransaction()
                    .replace(containerId, frag)
                    .commitAllowingStateLoss()
            } ?: Timber.e("V2 signature: SDK returned no signature fragment")
        }

        onDispose {
            val manager = activity?.supportFragmentManager
            val frag = padFragment
            if (manager != null && frag != null && !manager.isStateSaved) {
                manager.beginTransaction().remove(frag).commitAllowingStateLoss()
            }
        }
    }
}

/** Static stand-in for the signature pad, shown only in previews/inspection. */
@Composable
private fun SignaturePadPlaceholder(modifier: Modifier = Modifier) {
    val palette = AmaniV2Theme.palette
    Box(modifier, contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Outlined.Draw,
                contentDescription = null,
                tint = palette.inkLight,
                modifier = Modifier.height(40.dp)
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Signature pad",
                style = AmaniV2Type.caption,
                color = palette.inkLight,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Preview(name = "Signature", showBackground = true, widthDp = 360, heightDp = 740)
@Composable
private fun SignatureScreenPreview() {
    AmaniV2Theme {
        SignatureScreen(
            state = SignatureUiState(
                headerTitle = "Sign the contract",
                instruction = "Sign inside the box below, then confirm.",
                confirmButtonText = "Confirm",
                tryAgainButtonText = "Try again"
            ),
            onCompleted = {}
        )
    }
}
