package ai.amani.sdk.presentation_v2.nfc_scan

import ai.amani.sdk.extentions.setColor
import ai.amani.sdk.model.MRZModel
import ai.amani.sdk.presentation.home_kyc.CachingHomeKYC
import ai.amani.sdk.presentation_v2.components.AmaniV2Loader
import ai.amani.sdk.presentation_v2.components.PrimaryButton
import ai.amani.sdk.presentation_v2.components.ScreenHeader
import ai.amani.sdk.presentation_v2.components.SecondaryButton
import ai.amani.sdk.presentation_v2.id_capture.findFragmentActivity
import ai.amani.sdk.presentation_v2.theme.AmaniV2Dimens
import ai.amani.sdk.presentation_v2.theme.AmaniV2Palette
import ai.amani.sdk.presentation_v2.theme.AmaniV2Theme
import ai.amani.sdk.presentation_v2.theme.AmaniV2Type
import ai.amani.sdk.presentation_v2.theme.scaled
import ai.amani.sdk.presentation_v2.theme.toAmaniColorOrNull
import android.app.Activity
import android.app.PendingIntent
import android.content.Intent
import android.nfc.NfcAdapter
import android.os.Build
import android.provider.Settings
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.StartOffset
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Nfc
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
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
        Column(modifier = Modifier.fillMaxSize()) {
            ScreenHeader(title = state.texts.headerTitle, onBack = onBack)

            when (state.phase) {
                NfcPhase.FetchingMrz -> FetchingMrzContent(Modifier.weight(1f))
                NfcPhase.ReadyToScan -> ReadyToScanContent(
                    texts = state.texts,
                    accent = accent,
                    modifier = Modifier.weight(1f),
                    onScan = onScan
                )
                NfcPhase.MrzCheck -> MrzCheckContent(
                    state = state,
                    accent = accent,
                    modifier = Modifier.weight(1f),
                    onMrzChanged = onMrzChanged,
                    onConfirm = onMrzConfirmed
                )
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
    accent: Color,
    modifier: Modifier = Modifier,
    onScan: () -> Unit
) {
    val palette = AmaniV2Theme.palette
    Column(
        modifier = modifier
            .fillMaxWidth()
            // Keep the Start button clear of the system navigation bar.
            .navigationBarsPadding()
            .padding(horizontal = AmaniV2Dimens.screenPadding),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.weight(1f))
        NfcPulseRings(color = accent)
        Spacer(Modifier.height(28.dp))
        Text(
            texts.title,
            style = AmaniV2Type.heading.scaled(),
            color = palette.ink,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(10.dp))
        // Before the ID touches the phone we show the config instruction copy (like v1),
        // not a "scanning" status — scanning only starts once the user taps Start.
        texts.descriptions.forEach { line ->
            Text(
                line,
                style = AmaniV2Type.bodySmall.scaled(),
                color = palette.inkMuted,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 4.dp)
            )
        }
        Spacer(Modifier.weight(1f))
        PrimaryButton(text = texts.continueButtonText, onClick = onScan)
        Spacer(Modifier.height(AmaniV2Dimens.screenPadding))
    }
}

/** Concentric pink pulsing rings with the NFC chip badge — the HTML "brand moment". */
@Composable
private fun NfcPulseRings(color: Color, modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "nfcPulse")
    Box(
        modifier = modifier.size(160.dp),
        contentAlignment = Alignment.Center
    ) {
        listOf(0, 700).forEachIndexed { index, offsetMs ->
            val progress by transition.animateFloat(
                initialValue = 0f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1900, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart,
                    initialStartOffset = StartOffset(offsetMs)
                ),
                label = "ring$index"
            )
            val ringScale = 0.6f + progress * 0.8f
            val ringAlpha = (1f - progress) * 0.6f
            Box(
                Modifier
                    .size(160.dp)
                    .graphicsLayer {
                        scaleX = ringScale
                        scaleY = ringScale
                        alpha = ringAlpha
                    }
                    .clip(CircleShape)
                    .background(color)
            )
        }
        Box(
            Modifier
                .size(88.dp)
                .clip(CircleShape)
                .background(color),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Filled.Nfc, contentDescription = null, tint = Color.White, modifier = Modifier.size(40.dp))
        }
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
    // TODO: config-driven
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
    Column(
        modifier = modifier
            .fillMaxWidth()
            // Clear the nav bar and lift above the keyboard so the Continue button stays reachable.
            .navigationBarsPadding()
            .imePadding()
            .padding(horizontal = AmaniV2Dimens.screenPadding)
    ) {
        Spacer(Modifier.height(4.dp))
        Text(state.texts.mrzCheckTitle, style = AmaniV2Type.heading.scaled(), color = palette.ink)
        Spacer(Modifier.height(6.dp))
        Text(state.texts.mrzCheckDescription, style = AmaniV2Type.bodySmall.scaled(), color = palette.inkMuted)
        Spacer(Modifier.height(20.dp))

        MrzField(
            label = state.texts.birthDateLabel,
            value = mrz.birthDate,
            accent = accent,
            onValueChange = { onMrzChanged(mrz.copy(birthDate = it)) }
        )
        Spacer(Modifier.height(12.dp))
        MrzField(
            label = state.texts.expiryDateLabel,
            value = mrz.expireDate,
            accent = accent,
            onValueChange = { onMrzChanged(mrz.copy(expireDate = it)) }
        )
        Spacer(Modifier.height(12.dp))
        MrzField(
            label = state.texts.documentNoLabel,
            value = mrz.docNumber,
            accent = accent,
            keyboard = KeyboardType.Text,
            onValueChange = { onMrzChanged(mrz.copy(docNumber = it)) }
        )

        Spacer(Modifier.weight(1f))
        PrimaryButton(
            text = state.texts.continueButtonText,
            enabled = mrz.birthDate.isNotBlank() && mrz.expireDate.isNotBlank() && mrz.docNumber.isNotBlank(),
            onClick = onConfirm
        )
        Spacer(Modifier.height(AmaniV2Dimens.screenPadding))
    }
}

@Composable
private fun MrzField(
    label: String,
    value: String,
    accent: Color,
    modifier: Modifier = Modifier,
    keyboard: KeyboardType = KeyboardType.Number,
    onValueChange: (String) -> Unit
) {
    val palette = AmaniV2Theme.palette
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, style = AmaniV2Type.caption) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = keyboard),
        shape = RoundedCornerShape(AmaniV2Dimens.fieldRadius),
        colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
            focusedBorderColor = accent,
            focusedLabelColor = accent,
            cursorColor = accent,
            unfocusedBorderColor = palette.border
        ),
        modifier = modifier.fillMaxWidth()
    )
}

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
    mrzCheckTitle = "Check your document details",
    mrzCheckDescription = "We couldn't read the chip. Confirm these values and try again.",
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

@Preview(name = "NFC — MRZ check", showBackground = true, heightDp = 720)
@Composable
private fun NfcMrzCheckPreview() {
    AmaniV2Theme(AmaniV2Palette()) {
        NfcScanContent(
            NfcScanUiState(
                NfcPhase.MrzCheck,
                MRZModel(birthDate = "900101", expireDate = "300101", docNumber = "A12345678"),
                previewTexts
            )
        )
    }
}

@Preview(name = "NFC — disabled", showBackground = true, heightDp = 720)
@Composable
private fun NfcDisabledPreview() {
    AmaniV2Theme(AmaniV2Palette()) {
        NfcScanContent(NfcScanUiState(NfcPhase.ReadyToScan, MRZModel(), previewTexts, nfcDisabled = true))
    }
}
