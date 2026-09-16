package ai.amani.sdk.presentation_v2.nfc_scan

import ai.amani.sdk.extentions.setColor
import ai.amani.sdk.model.MRZModel
import ai.amani.sdk.presentation.home_kyc.CachingHomeKYC
import ai.amani.sdk.presentation_v2.components.AmaniV2Loader
import ai.amani.sdk.presentation_v2.components.PrimaryButton
import ai.amani.sdk.presentation_v2.components.ScreenHeader
import ai.amani.sdk.presentation_v2.components.WheelDatePickerDialog
import ai.amani.sdk.presentation_v2.components.WheelDate
import ai.amani.sdk.presentation_v2.components.SecondaryButton
import ai.amani.sdk.presentation_v2.id_capture.findFragmentActivity
import ai.amani.sdk.presentation_v2.theme.AmaniV2Dimens
import ai.amani.sdk.presentation_v2.theme.AmaniV2Palette
import ai.amani.sdk.presentation_v2.theme.AmaniV2Theme
import ai.amani.sdk.presentation_v2.theme.CappedCornerShape
import ai.amani.sdk.presentation_v2.theme.configCornerRadius
import ai.amani.sdk.presentation_v2.theme.AmaniV2Type
import ai.amani.sdk.presentation_v2.theme.scaled
import ai.amani.sdk.presentation_v2.theme.toAmaniColorOrNull
import android.app.Activity
import android.app.PendingIntent
import android.content.Intent
import android.nfc.NfcAdapter
import android.os.Build
import android.provider.Settings
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Nfc
import androidx.compose.material.icons.outlined.Cake
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import timber.log.Timber
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.FocusRequester
import java.util.Calendar
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.airbnb.lottie.LottieProperty
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.rememberLottieAnimatable
import com.airbnb.lottie.compose.rememberLottieComposition
import com.airbnb.lottie.compose.rememberLottieDynamicProperties
import com.airbnb.lottie.compose.rememberLottieDynamicProperty
import datamanager.model.config.Version

/**
 * Bridge so the NFC screen can receive the tag Intent the host activity gets in
 * `onNewIntent` (NFC foreground dispatch delivers the discovered tag to the Activity).
 * [AmaniComposeActivity] implements this; the screen registers a handler while armed.
 */
interface NfcIntentHost {
    var onNfcIntent: ((Intent) -> Unit)?
}

/**
 * V2 NFC leg. The main screen is the HTML "hold your ID to your phone" design (pulsing
 * pink rings + config copy); a chip read overlays the v1-style scanning modal, and a
 * misread drops to an editable MRZ-correction screen — the whole state machine lives in
 * [NfcScanViewModel] (a faithful port of v1's NFC flow), reusing the shared NFC repository.
 *
 * All copy and the ring color come from the server config via [NfcMapper]; nothing here is
 * hardcoded brand-side. In `@Preview`/inspection there is no host activity, so the NFC
 * foreground-dispatch plumbing is simply skipped.
 *
 * @param onFinished the leg is over — `success = true` uploads ID + NFC together, `false`
 *   uploads the ID only (see [ai.amani.sdk.presentation_v2.home_kyc.HomeKYCViewModel.finishNfcLeg]).
 */
@Composable
fun NfcScanScreen(
    version: Version,
    nfcOnly: Boolean,
    mrz: MRZModel,
    onFinished: (success: Boolean) -> Unit,
    modifier: Modifier = Modifier,
    onBack: () -> Unit = {}
) {
    val context = LocalContext.current
    val activity = context.findFragmentActivity()
    val vm: NfcScanViewModel = viewModel(factory = NfcScanViewModel.Factory)

    DisposableEffect(Unit) {
        vm.start(
            context = context,
            version = version,
            general = CachingHomeKYC.appConfig?.generalConfigs,
            nfcOnly = nfcOnly,
            // MRZ was already read on the confirm screen, so open ready to scan.
            initialMrz = mrz
        )
        // The VM is Activity-scoped and outlives this screen; reset on leave so a later
        // re-entry always starts NFC from its initial state (never a stale "scanned" state).
        onDispose { vm.onLeave() }
    }

    val state by vm.state.collectAsStateWithLifecycle()

    // One-shot effects: finish the leg / open NFC settings.
    androidx.compose.runtime.LaunchedEffect(vm) {
        vm.effects.collect { effect ->
            when (effect) {
                is NfcScanEffect.Finished -> onFinished(effect.success)
                NfcScanEffect.OpenNfcSettings -> {
                    runCatching { context.startActivity(Intent(Settings.ACTION_NFC_SETTINGS)) }
                }
            }
        }
    }

    val s = state
    if (s == null) {
        AmaniV2Loader(modifier)
        return
    }

    // Arm NFC foreground dispatch only after the user tapped Start (the scanning modal is
    // up in Waiting/Scanning) — matching v1, where dispatch is enabled when the modal shows.
    // The activity forwards the discovered tag back to the VM.
    val armed = (s.modal == NfcModalPhase.Waiting || s.modal == NfcModalPhase.Scanning) && !s.nfcDisabled
    DisposableEffect(armed, activity) {
        val host = activity as? NfcIntentHost
        if (armed && activity != null) {
            host?.onNfcIntent = { intent -> vm.onNfcTag(intent, context) }
            enableNfcForegroundDispatch(activity)
        }
        onDispose {
            if (activity != null) {
                disableNfcForegroundDispatch(activity)
                if (host?.onNfcIntent != null) host.onNfcIntent = null
            }
        }
    }

    NfcScanContent(
        state = s,
        modifier = modifier,
        onBack = onBack,
        onScan = vm::onScanClicked,
        onCancelScan = vm::onCancelScan,
        onMrzChanged = vm::onMrzChanged,
        onMrzConfirmed = vm::onMrzConfirmed,
        onEnableNfc = vm::onEnableNfcClicked
    )
}

/**
 * Stateless NFC content — drives every visual from [NfcScanUiState] so `@Preview` can
 * render each phase without a device or NFC hardware.
 */
@Composable
internal fun NfcScanContent(
    state: NfcScanUiState,
    modifier: Modifier = Modifier,
    onBack: () -> Unit = {},
    onScan: () -> Unit = {},
    onCancelScan: () -> Unit = {},
    onMrzChanged: (MRZModel) -> Unit = {},
    onMrzConfirmed: () -> Unit = {},
    onEnableNfc: () -> Unit = {}
) {
    val palette = AmaniV2Theme.palette
    val accent = state.texts.animationColorHex.toAmaniColorOrNull() ?: palette.accent

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(palette.background)
    ) {
        // While the scan modal is up, blur the live animation + copy underneath it; the blur
        // clears automatically once the modal closes (cancel/success/error all set modal = null).
        val contentBlur = if (state.modal != null) 18.dp else 0.dp
        Column(modifier = Modifier.fillMaxSize()) {
            ScreenHeader(
                title = if (state.phase == NfcPhase.MrzCheck) state.texts.mrzCheckHeaderTitle
                else state.texts.headerTitle,
                onBack = onBack
            )

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .blur(contentBlur)
            ) {
                when (state.phase) {
                    NfcPhase.FetchingMrz -> FetchingMrzContent(Modifier.fillMaxSize())
                    NfcPhase.ReadyToScan -> ReadyToScanContent(
                        texts = state.texts,
                        modifier = Modifier.fillMaxSize(),
                        onScan = onScan
                    )
                    NfcPhase.MrzCheck -> MrzCheckContent(
                        state = state,
                        // The chip-data screen is a form, not the scan animation: it follows the
                        // app's primary button color instead of the NFC animation tint.
                        accent = palette.accent,
                        modifier = Modifier.fillMaxSize(),
                        onMrzChanged = onMrzChanged,
                        onConfirm = onMrzConfirmed
                    )
                }
            }
        }

        // v1-style scanning modal: overlays once a chip read starts.
        if (state.modal != null) {
            NfcScanningModal(
                phase = state.modal,
                texts = state.texts,
                accent = accent,
                onCancel = onCancelScan
            )
        }

        if (state.nfcDisabled) {
            EnableNfcDialog(texts = state.texts, accent = accent, onEnable = onEnableNfc, onBack = onBack)
        }
    }
}

@Composable
private fun FetchingMrzContent(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = AmaniV2Theme.palette.accent, strokeWidth = 3.dp)
    }
}

@Composable
private fun ReadyToScanContent(
    texts: NfcTexts,
    modifier: Modifier = Modifier,
    onScan: () -> Unit
) {
    val palette = AmaniV2Theme.palette

    // Start stays visible from the start in its config color, but at 50% opacity and
    // non-clickable until the animation has played through once; after that it becomes fully
    // opaque and clickable.
    var animationCompleted by remember { mutableStateOf(false) }
    val buttonAlpha by animateFloatAsState(
        targetValue = if (animationCompleted) 1f else 0.5f,
        animationSpec = tween(durationMillis = 300),
        label = "nfcButtonAlpha"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            // Keep the Start button clear of the system navigation bar.
            .navigationBarsPadding()
            .padding(horizontal = AmaniV2Dimens.screenPadding),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(24.dp))

        // ── Text area (above the animation) ─────────────────────────────────────────────
        // Intro line for the animation (server `nfcV2.animationHint`).
        Text(
            text = texts.animationHint,
            style = AmaniV2Type.rowTitle.scaled(),
            color = palette.ink,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(16.dp))
        // V2 design animation (nfc_animation_v2.json): the full NFC-read explainer with a native,
        // per-state caption. Enables Start once its first play-through completes. Given weight(1f)
        // so it fills all the vertical space between the hint and the Start button (scaled up
        // proportionally, see NfcThemedAnimation) instead of floating small between fixed gaps —
        // the hint above and the button below sit outside the weight, so they keep their size.
        NfcThemedAnimation(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            stateTexts = texts.animationStates,
            onFirstLoopComplete = { animationCompleted = true }
        )
        Spacer(Modifier.height(16.dp))

        PrimaryButton(
            text = texts.continueButtonText,
            enabled = animationCompleted,
            onClick = onScan,
            modifier = Modifier.alpha(buttonAlpha),
            // Keep the config color while disabled (dimmed to 70% by the alpha above), rather
            // than falling back to Material's greyed disabled color.
            disabledContainerColor = palette.accent,
            disabledContentColor = palette.primaryButtonText
        )
        Spacer(Modifier.height(AmaniV2Dimens.screenPadding))
    }
}

/**
 * Marker timeline of the v2 NFC animation. The baked caption PNGs were stripped from the JSON,
 * so the caption is rendered natively and switched on these markers; the copy itself is
 * server-driven (`nfcV2.animationStates`, see [NfcV2AnimationCopy]).
 */
private object NfcAnimationStates {
    /** `startFrame to stateKey` on the 20 fps timeline, from the JSON's `state:*` markers. */
    val frames: List<Pair<Int, String>> = listOf(
        0 to "place", 36 to "detected", 66 to "hold", 96 to "reading",
        132 to "dontMove", 164 to "remove", 224 to "retry", 267 to "success"
    )
}

/**
 * The nfc_animation_v2 subject sits inside its 800x600 canvas with baked padding on every side,
 * so at full container width it still shows left/right (and top/bottom) gaps. Rendering the art
 * scaled by this factor crops that padding and lets the subject grow to fill the (now taller,
 * weight-driven) container both ways. The scale is uniform, so the art's natural aspect ratio is
 * preserved — the overflow is clipped by the container rather than stretched.
 */
private const val NFC_ART_FILL_SCALE = 1.5f

/**
 * The nfc_animation_v2 art has baked bottom padding, so the subject's visual end sits above the
 * animation box's bottom edge. Lift the native caption up by this much so it hugs the subject's
 * end (~20 dp above it) instead of floating in that empty space below. Tune to taste. The lift is
 * a draw-time offset only — it does not reduce the layout height, so the Start button stays put.
 */
private val NFC_CAPTION_LIFT = 40.dp

/**
 * V2 NFC brand animation: plays `nfc_animation_v2.json` on a loop with its original authored
 * colors kept fixed — only the baked background fill is made transparent so the screen
 * background shows through — with a native caption below that tracks the current `state:*`
 * window (derived from the playhead frame). Renders in `@Preview` (lottie-compose decodes the
 * raw asset in the inspection host).
 */
@Composable
private fun NfcThemedAnimation(
    modifier: Modifier = Modifier,
    // Per-state captions, server-driven (`nfcV2.animationStates`) with the SDK defaults merged
    // in by [NfcV2AnimationCopy].
    stateTexts: Map<String, String> = NfcV2AnimationCopy.DEFAULTS,
    // When set, freeze the animation on this state's start frame instead of looping, so each
    // `state:*` window can be inspected as its own @Preview card.
    previewStateKey: String? = null,
    // Fired once, after the very first full play-through — used to gate the Start button.
    onFirstLoopComplete: () -> Unit = {}
) {
    val palette = AmaniV2Theme.palette
    val composition by rememberLottieComposition(
        LottieCompositionSpec.RawRes(ai.amani.amani_sdk.R.raw.nfc_animation_v2)
    )
    val animatable = rememberLottieAnimatable()

    // Play through exactly once (then notify), and only after that loop forever. Skipped in
    // @Preview, where a state is pinned to a static frame instead.
    androidx.compose.runtime.LaunchedEffect(composition, previewStateKey) {
        val comp = composition ?: return@LaunchedEffect
        if (previewStateKey != null) return@LaunchedEffect
        animatable.animate(comp, iterations = 1)
        onFirstLoopComplete()
        animatable.animate(comp, iterations = LottieConstants.IterateForever)
    }

    val totalFrames = composition?.durationFrames ?: 300f
    // Live playhead while running; a frozen per-state frame when a preview pins a state.
    val progress = if (previewStateKey != null) {
        val frame = NfcAnimationStates.frames.firstOrNull { it.second == previewStateKey }?.first ?: 0
        if (totalFrames > 0f) frame / totalFrames else 0f
    } else {
        animatable.progress
    }

    // Keep every color exactly as authored in the JSON; only zero out the two baked "background"
    // fills so the screen background shows through (transparent). OPACITY is 0-100 per fill; the
    // "**" head hits both background fills at once.
    val dynamicProperties = rememberLottieDynamicProperties(
        rememberLottieDynamicProperty(LottieProperty.OPACITY, 0, "**", "background")
    )

    // Native caption for the current state: the pinned preview state, else the last marker
    // whose start frame is at/behind the playhead.
    val currentFrame = progress * totalFrames
    val stateKey = previewStateKey
        ?: NfcAnimationStates.frames.lastOrNull { it.first <= currentFrame }?.second
        ?: "place"
    val caption = stateTexts[stateKey].orEmpty()

    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                // Fill the vertical space the parent hands down (weight from ReadyToScanContent),
                // so the animation grows to fill the screen height between the hint and the button.
                .weight(1f)
                // The scaled art overflows this box; clip so the cropped side/top/bottom padding
                // is cut rather than drawn over the caption.
                .clipToBounds()
        ) {
            LottieAnimation(
                composition = composition,
                progress = { progress },
                dynamicProperties = dynamicProperties,
                contentScale = ContentScale.Fit,
                // The art sits inside its 800x600 canvas with baked side padding, so at full
                // width it still shows left/right gaps. Scale it up uniformly (aspect preserved,
                // natural height) to crop that padding and fill the width edge-to-edge.
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = NFC_ART_FILL_SCALE
                        scaleY = NFC_ART_FILL_SCALE
                    }
            )
        }
        // Pull the caption up into the empty space under the art so it sits at the animation's
        // visual end (~20 dp above it), not far below the box — see [NFC_CAPTION_LIFT].
        Text(
            caption,
            style = AmaniV2Type.rowTitle.scaled(),
            color = palette.ink,
            textAlign = TextAlign.Center,
            modifier = Modifier.offset(y = -NFC_CAPTION_LIFT)
        )
    }
}

/**
 * Row of dots that fill with the accent color left→right, one at a time, on a loop — the
 * Compose port of v1's [ai.amani.sdk.presentation.nfc.LoadingDotView] shown while the chip
 * is being read. When [animating] is false all dots stay idle (grey), which is the modal's
 * "waiting for the ID" state before a tag arrives.
 */
@Composable
private fun NfcDotLoader(accent: Color, animating: Boolean, modifier: Modifier = Modifier) {
    val dotCount = 5
    val idle = AmaniV2Theme.palette.inkLight.copy(alpha = 0.35f)
    val filled = androidx.compose.runtime.remember { androidx.compose.runtime.mutableIntStateOf(0) }

    androidx.compose.runtime.LaunchedEffect(animating) {
        if (!animating) {
            filled.intValue = 0
            return@LaunchedEffect
        }
        // Sweep the fill left→right, then reset — same cadence idea as v1 (one step at a time).
        while (true) {
            for (i in 1..dotCount) {
                filled.intValue = i
                kotlinx.coroutines.delay(450)
            }
            filled.intValue = 0
            kotlinx.coroutines.delay(300)
        }
    }

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(dotCount) { index ->
            Box(
                Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(if (index < filled.intValue) accent else idle)
            )
        }
    }
}

/**
 * The v1 Lottie scan animation, hosted in Compose via [AndroidView] (reusing the same
 * `nfc_animation` / `nfc_done` raw assets the v1 dialog uses, shipped in the AmaniAi aar).
 * [animationColorHex] tints the whole animation via [setColor] (SRC_ATOP over every layer).
 * In `@Preview`/inspection there is no real view host, so a blank spacer stands in.
 */
// TODO: config-driven
@Composable
private fun NfcLottie(
    rawRes: Int,
    loop: Boolean,
    playing: Boolean,
    animationColorHex: String?,
    modifier: Modifier = Modifier
) {
    if (androidx.compose.ui.platform.LocalInspectionMode.current) {
        androidx.compose.foundation.layout.Spacer(modifier)
        return
    }
    androidx.compose.ui.viewinterop.AndroidView(
        modifier = modifier,
        factory = { ctx ->
            com.airbnb.lottie.LottieAnimationView(ctx).apply {
                setAnimation(rawRes)
                repeatCount = if (loop) com.airbnb.lottie.LottieDrawable.INFINITE else 0
                // Config-driven tint (no-op when null).
                setColor(animationColorHex)
                // Start paused on the first frame; the update lambda plays it when scanning.
                progress = 0f
            }
        },
        // React to [playing] changes: opening the modal shows the static first frame, and
        // the animation only plays once scanning actually begins.
        update = { view ->
            if (playing) {
                if (!view.isAnimating) view.playAnimation()
            } else {
                view.pauseAnimation()
                view.progress = 0f
            }
        }
    )
}

/** Editable MRZ fields shown after a misread (v1 `ShowMRZCheck`). */
@Composable
private fun MrzCheckContent(
    state: NfcScanUiState,
    accent: Color,
    modifier: Modifier = Modifier,
    onMrzChanged: (MRZModel) -> Unit,
    onConfirm: () -> Unit
) {
    val palette = AmaniV2Theme.palette
    val mrz = state.mrz

    // Which row is being corrected by hand; null = every row shows its read-only value. The
    // values come off the chip, so editing is the exception and starts on a tap (the hint under
    // the rows says so).
    var editingField by remember { mutableStateOf<MrzField?>(null) }

    /** Which date wheel is open; null = no picker. */
    var pickingDate by remember { mutableStateOf<MrzField?>(null) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            // Clear the nav bar and lift above the keyboard so the confirm button stays reachable.
            .navigationBarsPadding()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = AmaniV2Dimens.screenPadding)
    ) {
        Spacer(Modifier.height(8.dp))
        ChipDataEyebrow(text = state.texts.mrzCheckEyebrow, accent = accent)

        Spacer(Modifier.height(20.dp))
        Text(
            state.texts.mrzCheckTitle,
            style = AmaniV2Type.title.scaled().copy(fontWeight = FontWeight.Bold),
            color = palette.ink
        )
        Spacer(Modifier.height(8.dp))
        Text(
            state.texts.mrzCheckDescription,
            style = AmaniV2Type.body.scaled(),
            color = palette.inkMuted
        )

        Spacer(Modifier.height(24.dp))
        ChipDataRow(
            icon = Icons.Outlined.CreditCard,
            label = state.texts.documentNoLabel,
            value = mrz.docNumber,
            displayValue = mrz.docNumber,
            accent = accent,
            editing = editingField == MrzField.DocumentNumber,
            keyboard = KeyboardType.Text,
            onEdit = { editingField = MrzField.DocumentNumber },
            onEditDone = { editingField = null },
            onValueChange = { onMrzChanged(mrz.copy(docNumber = it)) }
        )
        Spacer(Modifier.height(12.dp))
        ChipDataRow(
            icon = Icons.Outlined.Cake,
            label = state.texts.birthDateLabel,
            value = mrz.birthDate,
            displayValue = mrz.birthDate.asReadableDate(isBirthDate = true),
            accent = accent,
            // A date is picked on a wheel, never typed: it rules out an impossible day and keeps
            // the six-digit MRZ format out of the user's hands.
            editing = false,
            onEdit = { pickingDate = MrzField.BirthDate },
            onEditDone = {},
            onValueChange = { onMrzChanged(mrz.copy(birthDate = it)) }
        )
        Spacer(Modifier.height(12.dp))
        ChipDataRow(
            icon = Icons.Outlined.CalendarMonth,
            label = state.texts.expiryDateLabel,
            value = mrz.expireDate,
            displayValue = mrz.expireDate.asReadableDate(isBirthDate = false),
            accent = accent,
            editing = false,
            onEdit = { pickingDate = MrzField.ExpiryDate },
            onEditDone = {},
            onValueChange = { onMrzChanged(mrz.copy(expireDate = it)) }
        )

        Spacer(Modifier.height(16.dp))
        ChipDataHint(text = state.texts.mrzCheckHint)

        Spacer(Modifier.height(32.dp))
        PrimaryButton(
            text = state.texts.mrzConfirmButtonText,
            // The chip is addressed with the MRZ key, which is only built from six-digit dates:
            // a half-read or hand-typed value must not leave this screen.
            enabled = mrz.docNumber.isNotBlank() &&
                mrz.birthDate.isMrzDate() &&
                mrz.expireDate.isMrzDate(),
            onClick = {
                // Hand the reader exactly `yyMMdd`, whatever shape the chip reported.
                onMrzChanged(
                    mrz.copy(
                        birthDate = mrz.birthDate.asMrzDigits(),
                        expireDate = mrz.expireDate.asMrzDigits(),
                        docNumber = mrz.docNumber.trim()
                    )
                )
                onConfirm()
            }
        )
        Spacer(Modifier.height(AmaniV2Dimens.screenPadding))
    }

    pickingDate?.let { field ->
        key(field) {
        val isBirthDate = field == MrzField.BirthDate
        val raw = if (isBirthDate) mrz.birthDate else mrz.expireDate
        val currentYear = Calendar.getInstance().get(Calendar.YEAR)

        WheelDatePickerDialog(
            title = if (isBirthDate) state.texts.birthDateLabel else state.texts.expiryDateLabel,
            initial = raw.asWheelDate(isBirthDate)
                // An unreadable value opens on something plausible rather than on today: a birth
                // date decades back, an expiry a few years ahead.
                ?: WheelDate(1, 1, if (isBirthDate) currentYear - 30 else currentYear + 5),
            yearRange = if (isBirthDate) (currentYear - 100)..currentYear
            else (currentYear - 10)..(currentYear + 30),
            confirmText = state.texts.mrzConfirmButtonText,
            cancelText = state.texts.cancelButtonText,
            onDismiss = { pickingDate = null },
            onConfirm = { picked ->
                val mrzValue = picked.asMrzDate()
                Timber.i(
                    "V2 chip data: ${if (isBirthDate) "birth" else "expiry"} date picked " +
                        "$picked → $mrzValue (was $raw)"
                )
                if (isBirthDate) onMrzChanged(mrz.copy(birthDate = mrzValue))
                else onMrzChanged(mrz.copy(expireDate = mrzValue))
                pickingDate = null
            }
        )
        }
    }
}

/** The three correctable values, so only one row is in edit mode at a time. */
private enum class MrzField { DocumentNumber, BirthDate, ExpiryDate }

/** Accent pill above the title: this data was read off the chip, not typed by the user. */
@Composable
private fun ChipDataEyebrow(text: String, accent: Color) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(AmaniV2Dimens.pillRadius))
            .background(accent.copy(alpha = 0.12f))
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(Icons.Filled.Nfc, contentDescription = null, tint = accent, modifier = Modifier.size(16.dp))
        Text(
            text.uppercase(),
            style = AmaniV2Type.label.scaled().copy(fontWeight = FontWeight.Bold, letterSpacing = 0.8.sp),
            color = accent
        )
    }
}

/**
 * One chip-read value: icon, its label, the value itself and a check badge. Tapping the card
 * turns the value into a text field so a misread can be corrected by hand (v1 ShowMRZCheck).
 */
@Composable
private fun ChipDataRow(
    icon: ImageVector,
    label: String,
    value: String,
    displayValue: String,
    accent: Color,
    editing: Boolean,
    onEdit: () -> Unit,
    onEditDone: () -> Unit,
    onValueChange: (String) -> Unit,
    keyboard: KeyboardType = KeyboardType.Number
) {
    val palette = AmaniV2Theme.palette
    val shape = CappedCornerShape(configCornerRadius())

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(palette.ink.copy(alpha = 0.04f))
            .clickable(enabled = !editing, onClick = onEdit)
            .padding(horizontal = 14.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .background(accent.copy(alpha = 0.14f), CappedCornerShape(configCornerRadius())),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(22.dp))
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                label.uppercase(),
                style = AmaniV2Type.label.scaled().copy(letterSpacing = 0.6.sp),
                color = palette.inkMuted
            )
            Spacer(Modifier.height(2.dp))
            // The value keeps its own typography whether it is read or written: editing turns it
            // into a bare text field with the same style, so correcting a value never swaps the
            // card's look for a platform input box.
            val valueStyle = AmaniV2Type.heading.scaled().copy(
                fontWeight = FontWeight.SemiBold,
                color = palette.ink
            )
            if (editing) {
                val focusRequester = remember { FocusRequester() }
                LaunchedEffect(Unit) { focusRequester.requestFocus() }

                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    textStyle = valueStyle,
                    singleLine = true,
                    cursorBrush = SolidColor(accent),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = keyboard,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(onDone = { onEditDone() }),
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester)
                )
            } else {
                Text(
                    displayValue,
                    style = valueStyle,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // The badge doubles as the commit action while a value is being corrected, so the row
        // keeps the same shape in both modes.
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(if (editing) accent else accent.copy(alpha = 0.14f))
                .clickable(enabled = editing, onClick = onEditDone),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Filled.Check,
                contentDescription = null,
                tint = if (editing) palette.primaryButtonText else accent,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

/** Footer note explaining where the values came from and that a tap corrects them. */
@Composable
private fun ChipDataHint(text: String) {
    val palette = AmaniV2Theme.palette
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(CappedCornerShape(configCornerRadius()))
            .background(palette.ink.copy(alpha = 0.05f))
            .padding(horizontal = 14.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(
            Icons.Outlined.Lock,
            contentDescription = null,
            tint = palette.inkMuted,
            modifier = Modifier.size(16.dp)
        )
        Text(text, style = AmaniV2Type.bodySmall.scaled(), color = palette.inkMuted)
    }
}

/**
 * MRZ dates arrive as `yyMMdd`; the screen shows them the way they are printed on the document.
 * A birth year in the future belongs to the previous century — an expiry never does.
 */
private fun String.asReadableDate(isBirthDate: Boolean): String {
    val digits = filter { it.isDigit() }
    if (digits.length != MRZ_DATE_LENGTH) return this

    val yy = digits.substring(0, 2).toIntOrNull() ?: return this
    val currentYy = Calendar.getInstance().get(Calendar.YEAR) % 100
    val year = when {
        !isBirthDate -> 2000 + yy
        yy > currentYy -> 1900 + yy
        else -> 2000 + yy
    }
    return "${digits.substring(4, 6)}/${digits.substring(2, 4)}/$year"
}

private const val MRZ_DATE_LENGTH = 6

/** `yyMMdd` → the wheel's date, or null when the chip value is not a date at all. */
private fun String.asWheelDate(isBirthDate: Boolean): WheelDate? {
    val digits = filter { it.isDigit() }
    if (digits.length != MRZ_DATE_LENGTH) return null

    val yy = digits.substring(0, 2).toIntOrNull() ?: return null
    val month = digits.substring(2, 4).toIntOrNull()?.takeIf { it in 1..12 } ?: return null
    val day = digits.substring(4, 6).toIntOrNull()?.takeIf { it in 1..31 } ?: return null
    val currentYy = Calendar.getInstance().get(Calendar.YEAR) % 100
    val year = when {
        !isBirthDate -> 2000 + yy
        yy > currentYy -> 1900 + yy
        else -> 2000 + yy
    }
    return WheelDate(day = day, month = month, year = year)
}

/** True when this value is the `yyMMdd` the MRZ key is built from. */
private fun String.isMrzDate(): Boolean {
    val digits = asMrzDigits()
    if (digits.length != MRZ_DATE_LENGTH) return false
    val month = digits.substring(2, 4).toIntOrNull() ?: return false
    val day = digits.substring(4, 6).toIntOrNull() ?: return false
    return month in 1..12 && day in 1..31
}

/** Drops anything the chip may have added around the six digits (separators, spaces). */
private fun String.asMrzDigits(): String = filter { it.isDigit() }

/** The wheel's date → the `yyMMdd` the MRZ carries. */
private fun WheelDate.asMrzDate(): String =
    "%02d%02d%02d".format(year % 100, month, day)


/** v1 NFCScanningBottomDialog counterpart: a bottom card that overlays during the read. */
@Composable
private fun NfcScanningModal(
    phase: NfcModalPhase,
    texts: NfcTexts,
    accent: Color,
    onCancel: () -> Unit
) {
    val palette = AmaniV2Theme.palette
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.45f)),
        contentAlignment = Alignment.BottomCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(palette.surface, RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                // Lift the modal content above the system navigation bar.
                .navigationBarsPadding()
                .padding(horizontal = AmaniV2Dimens.screenPadding, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                Modifier
                    .width(36.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(palette.backgroundWarm)
            )
            Spacer(Modifier.height(20.dp))

            val title = when (phase) {
                NfcModalPhase.Waiting -> texts.modalReadyTitle
                NfcModalPhase.Scanning -> texts.modalScanningTitle
                NfcModalPhase.Error -> texts.modalFailedText
                NfcModalPhase.Done -> texts.modalDoneText
            }
            Text(
                title,
                style = AmaniV2Type.rowTitle.scaled(),
                color = palette.ink,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(20.dp))

            // Middle: the v1 Lottie scan animation (nfc_animation while waiting/scanning,
            // nfc_done on success); a static error glyph on failure. [accent] folds the config
            // nfcAnimationColor (→ brand accent fallback), so the animation is tinted from config.
            val animationColorHex = accent.toColorHex()
            when (phase) {
                NfcModalPhase.Waiting, NfcModalPhase.Scanning ->
                    // Waiting = paused on its first frame (modal open ≠ scanning); it only
                    // starts playing once the chip is being read (Scanning) — together with
                    // the dot loader below.
                    NfcLottie(
                        rawRes = ai.amani.R.raw.nfc_animation,
                        loop = true,
                        playing = phase == NfcModalPhase.Scanning,
                        animationColorHex = animationColorHex,
                        modifier = Modifier.fillMaxWidth().height(140.dp)
                    )
                NfcModalPhase.Done ->
                    NfcLottie(
                        rawRes = ai.amani.R.raw.nfc_done,
                        loop = false,
                        playing = true,
                        animationColorHex = animationColorHex,
                        modifier = Modifier.fillMaxWidth().height(140.dp)
                    )
                NfcModalPhase.Error ->
                    androidx.compose.foundation.Image(
                        painter = androidx.compose.ui.res.painterResource(ai.amani.amani_sdk.R.drawable.ic_nfc_error),
                        contentDescription = null,
                        modifier = Modifier.size(120.dp)
                    )
            }

            if (phase == NfcModalPhase.Waiting || phase == NfcModalPhase.Scanning) {
                Spacer(Modifier.height(24.dp))
                NfcDotLoader(accent = accent, animating = phase == NfcModalPhase.Scanning)
                Spacer(Modifier.height(20.dp))
                SecondaryButton(text = texts.cancelButtonText, onClick = onCancel)
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

/** NFC-off prompt — a full-screen scrim with a card offering to open system NFC settings. */
@Composable
private fun EnableNfcDialog(
    texts: NfcTexts,
    accent: Color,
    onEnable: () -> Unit,
    onBack: () -> Unit
) {
    val palette = AmaniV2Theme.palette
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.45f))
            .clickable(enabled = false) {},
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .padding(AmaniV2Dimens.screenPadding)
                .fillMaxWidth()
                .background(palette.surface, RoundedCornerShape(AmaniV2Dimens.cardRadius))
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                Modifier.size(56.dp).clip(CircleShape).background(accent.copy(alpha = 0.10f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.Nfc, null, tint = accent, modifier = Modifier.size(28.dp))
            }
            Spacer(Modifier.height(16.dp))
            Text(
                texts.enableNfcHeader,
                style = AmaniV2Type.rowTitle.scaled(),
                color = palette.ink,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(6.dp))
            Text(
                texts.enableNfcDescription,
                style = AmaniV2Type.bodySmall.scaled(),
                color = palette.inkMuted,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(20.dp))
            PrimaryButton(text = texts.enableNfcButton, onClick = onEnable)
            Spacer(Modifier.height(10.dp))
            SecondaryButton(text = texts.cancelButtonText, onClick = onBack)
        }
    }
}

/**
 * Whether the device physically has an NFC adapter (enabled or not). Used by the nav host
 * to decide whether to route to the NFC screen at all; if present-but-off, the screen shows
 * the enable-NFC prompt. Mirrors the intent of v1's `deviceHasNFC`.
 */
internal fun deviceHasNfcHardware(context: android.content.Context): Boolean {
    val manager = context.getSystemService(android.content.Context.NFC_SERVICE) as? android.nfc.NfcManager
    return manager?.defaultAdapter != null
}

/** `#RRGGBB` hex for [setColor] / `Color.parseColor`, dropping alpha. */
private fun Color.toColorHex(): String = String.format("#%06X", 0xFFFFFF and toArgb())

private fun enableNfcForegroundDispatch(activity: Activity) {
    runCatching {
        val adapter = NfcAdapter.getDefaultAdapter(activity) ?: return
        val intent = Intent(activity, activity.javaClass).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) PendingIntent.FLAG_MUTABLE else 0
        val pendingIntent = PendingIntent.getActivity(activity, 0, intent, flags)
        val techLists = arrayOf(arrayOf("android.nfc.tech.IsoDep"))
        adapter.enableForegroundDispatch(activity, pendingIntent, null, techLists)
    }
}

private fun disableNfcForegroundDispatch(activity: Activity) {
    runCatching { NfcAdapter.getDefaultAdapter(activity)?.disableForegroundDispatch(activity) }
}

// ── Previews ─────────────────────────────────────────────────────────────────────────

private val previewTexts = NfcTexts(
    headerTitle = "NFC",
    title = "Hold your ID to your phone",
    descriptions = listOf("Place the top back of your phone on the chip side of your ID and hold it still."),
    searchingLabel = "Searching for chip…",
    cancelButtonText = "Cancel",
    continueButtonText = "Start scan",
    mrzCheckHeaderTitle = "Chip data",
    mrzCheckEyebrow = "Read from chip",
    mrzCheckTitle = "Check your details",
    mrzCheckDescription = "This was read securely from your ID's chip. Confirm it matches your document.",
    mrzCheckHint = "These values were read from your ID. Tap any field to correct it if something looks wrong.",
    mrzConfirmButtonText = "Confirm",
    birthDateLabel = "Date of birth",
    expiryDateLabel = "Date of expiry",
    documentNoLabel = "Document number",
    modalReadyTitle = "Ready to scan",
    modalReadyDescription = "Hold your phone near the chip.",
    modalScanningTitle = "Scanning…",
    modalScanningDescription = "Keep the document steady.",
    modalFailedText = "Couldn't read the chip. Try again.",
    modalDoneText = "Chip verified",
    enableNfcHeader = "Turn on NFC",
    enableNfcDescription = "NFC is off. Turn it on to scan your document's chip.",
    enableNfcButton = "Open settings",
    animationColorHex = null
)

@Preview(name = "NFC — ready", showBackground = true, heightDp = 720)
@Composable
private fun NfcReadyPreview() {
    AmaniV2Theme(AmaniV2Palette()) {
        NfcScanContent(NfcScanUiState(NfcPhase.ReadyToScan, MRZModel(), previewTexts))
    }
}

@Preview(name = "NFC — scanning modal", showBackground = true, heightDp = 720)
@Composable
private fun NfcScanningPreview() {
    AmaniV2Theme(AmaniV2Palette()) {
        NfcScanContent(NfcScanUiState(NfcPhase.ReadyToScan, MRZModel(), previewTexts, modal = NfcModalPhase.Scanning))
    }
}

@Preview(name = "NFC — chip data", showBackground = true, heightDp = 860)
@Composable
private fun NfcMrzCheckPreview() {
    AmaniV2Theme(AmaniV2Palette()) {
        NfcScanContent(
            NfcScanUiState(
                NfcPhase.MrzCheck,
                MRZModel(birthDate = "970423", expireDate = "360510", docNumber = "A54P40583"),
                previewTexts
            )
        )
    }
}

/**
 * The same screen with a config radius no card can take, so the cards and the footer note stay
 * rounded rectangles instead of deforming.
 */
@Preview(name = "NFC — chip data, oversized radius", showBackground = true, heightDp = 860)
@Composable
private fun NfcMrzCheckLargeRadiusPreview() {
    AmaniV2Theme(AmaniV2Palette(buttonRadius = 220)) {
        NfcScanContent(
            NfcScanUiState(
                NfcPhase.MrzCheck,
                MRZModel(birthDate = "970423", expireDate = "360510", docNumber = "A54P40583"),
                previewTexts
            )
        )
    }
}

/** A value the chip could not give up: the row still reads, waiting to be corrected. */
@Preview(name = "NFC — chip data, unreadable value", showBackground = true, heightDp = 860)
@Composable
private fun NfcMrzCheckEmptyValuePreview() {
    AmaniV2Theme(AmaniV2Palette()) {
        NfcScanContent(
            NfcScanUiState(
                NfcPhase.MrzCheck,
                MRZModel(birthDate = "970423", expireDate = "", docNumber = "A54P40583"),
                previewTexts
            )
        )
    }
}

/** One chip-data row in each of its two modes: read from the chip, and being corrected. */
@Preview(name = "Chip data row — read vs edit", showBackground = true, widthDp = 390)
@Composable
private fun ChipDataRowPreview() {
    AmaniV2Theme(AmaniV2Palette()) {
        Column(
            modifier = Modifier
                .background(AmaniV2Theme.palette.background)
                .padding(16.dp)
        ) {
            ChipDataRow(
                icon = Icons.Outlined.CreditCard,
                label = "Document number",
                value = "A54P40583",
                displayValue = "A54P40583",
                accent = AmaniV2Theme.palette.accent,
                editing = false,
                onEdit = {},
                onEditDone = {},
                onValueChange = {}
            )
            Spacer(Modifier.height(12.dp))
            ChipDataRow(
                icon = Icons.Outlined.CreditCard,
                label = "Document number",
                value = "A54P40583",
                displayValue = "A54P40583",
                accent = AmaniV2Theme.palette.accent,
                editing = true,
                onEdit = {},
                onEditDone = {},
                onValueChange = {}
            )
        }
    }
}

@Preview(name = "NFC — disabled", showBackground = true, heightDp = 720)
@Composable
private fun NfcDisabledPreview() {
    AmaniV2Theme(AmaniV2Palette()) {
        NfcScanContent(NfcScanUiState(NfcPhase.ReadyToScan, MRZModel(), previewTexts, nfcDisabled = true))
    }
}

/** Feeds every `state:*` key of the v2 NFC animation so each renders as its own preview card. */
private class NfcAnimationStateProvider : PreviewParameterProvider<String> {
    override val values: Sequence<String> = NfcAnimationStates.frames.asSequence().map { it.second }
}

/**
 * One preview card per animation state (place → success): the Lottie frozen on that state's
 * start frame with its native caption, so every `state:*` window is inspectable at design time.
 */
@Preview(name = "NFC animation states", showBackground = true, widthDp = 320, heightDp = 320)
@Composable
private fun NfcAnimationStatePreview(
    @PreviewParameter(NfcAnimationStateProvider::class) stateKey: String
) {
    AmaniV2Theme(AmaniV2Palette()) {
        // The art is dark-themed (white "Success check"/labels, gold document, etc.), so on the
        // default light palette the transparent-background success/retry states would render
        // white-on-white. Preview on the animation's native dark surface so every state shows.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF0C0E14))
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            NfcThemedAnimation(
                modifier = Modifier.fillMaxSize(),
                previewStateKey = stateKey
            )
        }
    }
}
