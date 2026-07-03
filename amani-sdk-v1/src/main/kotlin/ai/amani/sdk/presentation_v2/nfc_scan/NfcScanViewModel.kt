package ai.amani.sdk.presentation_v2.nfc_scan

import ai.amani.sdk.data.repository.nfc.NFCRepositoryImp
import ai.amani.sdk.extentions.deviceNFCState
import ai.amani.sdk.extentions.parcelable
import ai.amani.sdk.model.MRZModel
import android.content.Context
import android.content.Intent
import android.nfc.NfcAdapter
import android.nfc.Tag
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import datamanager.model.config.GeneralConfigs
import datamanager.model.config.Version
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * Compose-friendly view model for the V2 NFC leg. Re-authored for a unidirectional
 * Compose flow, but the logic is a faithful port of v1's NFCSharedViewModel +
 * PreviewScreenViewModel MRZ hand-off — and it reuses the very same shared
 * [NFCRepositoryImp] (`getMRZ` OCR + `scan` chip read), so no NFC logic is duplicated.
 *
 * State machine (mirrors v1):
 *  - [start] reads the MRZ off the just-captured ID (`getMRZ`). Complete MRZ →
 *    [NfcPhase.ReadyToScan] (the HTML pulsing-ring screen, prefilled); an incomplete/failed
 *    read → [NfcPhase.MrzCheck] so the user can correct the values by hand.
 *  - [onScanClicked] arms the reader and shows the scanning modal ([NfcModalPhase.Waiting]).
 *  - The host activity forwards the discovered tag to [onNfcTag], which runs `scan`:
 *    success → [NfcModalPhase.Done] then [NfcScanEffect.Finished] (`success = true`);
 *    a read failure (or [onCancelScan]) counts an attempt and, once `maxAttempt` is hit,
 *    finishes with `success = false` (upload the ID only) — otherwise it drops back to
 *    [NfcPhase.MrzCheck] (v1 `ShowMRZCheck`).
 */
class NfcScanViewModel(
    private val repository: NFCRepositoryImp
) : ViewModel() {

    private val _state = MutableStateFlow<NfcScanUiState?>(null)
    val state: StateFlow<NfcScanUiState?> = _state.asStateFlow()

    private val _effects = Channel<NfcScanEffect>(Channel.BUFFERED)
    val effects: Flow<NfcScanEffect> = _effects.receiveAsFlow()

    private lateinit var version: Version
    private var nfcOnly = false
    private var maxAttempt = 3
    private var currentAttempt = 0
    private var scanInProgress = false
    private var started = false

    /**
     * One-shot entry: resolves config texts, checks whether NFC is enabled, and reads the
     * MRZ off the just-captured ID. Idempotent across recompositions.
     *
     * @param initialMrz a client-supplied MRZ (NFC-only flow); when null the MRZ is OCR'd
     *   from the captured ID via the shared repository (the ID → NFC flow).
     */
    fun start(
        context: Context,
        version: Version,
        general: GeneralConfigs?,
        nfcOnly: Boolean,
        initialMrz: MRZModel?
    ) {
        if (started) return
        started = true
        this.version = version
        this.nfcOnly = nfcOnly
        this.maxAttempt = version.maxAttempt.takeIf { it > 0 } ?: 3
        // Fresh entry always starts from a clean slate (see [onLeave]).
        currentAttempt = 0
        scanInProgress = false

        val texts = NfcMapper.texts(version, general)
        val disabled = isNfcDisabled(context)

        if (initialMrz != null) {
            _state.value = NfcScanUiState(NfcPhase.ReadyToScan, initialMrz, texts, nfcDisabled = disabled)
            return
        }

        _state.value = NfcScanUiState(NfcPhase.FetchingMrz, MRZModel(), texts, nfcDisabled = disabled)
        fetchMrz()
    }

    /** Re-check the device NFC state (e.g. after returning from NFC settings). */
    fun refreshNfcState(context: Context) {
        update { it.copy(nfcDisabled = isNfcDisabled(context)) }
    }

    /**
     * Called when the screen leaves composition. This view model is Activity-scoped, so it
     * survives navigating away and back; without this reset, re-entering the NFC screen would
     * show the *previous* result (e.g. a completed scan). Clearing here guarantees a fresh
     * initial state (MRZ read → ReadyToScan) on every re-entry.
     */
    fun onLeave() {
        started = false
        currentAttempt = 0
        scanInProgress = false
        _state.value = null
    }

    private fun fetchMrz() {
        val type = version.type
        if (type.isNullOrEmpty()) {
            Timber.e("V2 NFC: version.type is null, cannot read MRZ — uploading ID only")
            finish(success = false)
            return
        }
        repository.getMRZ(
            type = type,
            onComplete = { mrz ->
                val birth = mrz.mRZBirthDate
                val expiry = mrz.mRZExpiryDate
                val docNo = mrz.mRZDocumentNumber
                if (!birth.isNullOrEmpty() && !expiry.isNullOrEmpty() && !docNo.isNullOrEmpty()) {
                    Timber.i("V2 NFC: MRZ read from ID")
                    update { it.copy(phase = NfcPhase.ReadyToScan, mrz = MRZModel(birth, expiry, docNo)) }
                } else {
                    // Partial OCR — let the user complete/correct it by hand.
                    Timber.w("V2 NFC: MRZ incomplete, showing manual correction")
                    update { it.copy(phase = NfcPhase.MrzCheck, mrz = MRZModel(birth ?: "", expiry ?: "", docNo ?: "")) }
                }
            },
            onError = {
                Timber.e("V2 NFC: MRZ read error, showing manual correction")
                update { it.copy(phase = NfcPhase.MrzCheck) }
            }
        )
    }

    /** MRZ correction: field edits and confirm (v1 continue → back to ReadyToScan). */
    fun onMrzChanged(mrz: MRZModel) = update { it.copy(mrz = mrz) }

    fun onMrzConfirmed() = update { it.copy(phase = NfcPhase.ReadyToScan) }

    /** Start button (v1 infoContinueBtn): open the scanning modal and arm the reader. */
    fun onScanClicked() = update { it.copy(modal = NfcModalPhase.Waiting) }

    /**
     * A tag was discovered while the scanning modal was armed ([NfcModalPhase.Waiting]) —
     * read the chip with the current MRZ. Called by the host activity's onNewIntent bridge.
     * The modal switches to [NfcModalPhase.Scanning] (animated dots) for the read.
     */
    fun onNfcTag(intent: Intent, context: Context) {
        val current = _state.value ?: return
        // Only once the user tapped Start (modal armed & waiting) and no read is running.
        if (current.modal != NfcModalPhase.Waiting || scanInProgress) return
        val tag = intent.parcelable<Tag>(NfcAdapter.EXTRA_TAG)
        if (tag == null) {
            Timber.e("V2 NFC: intent carried no tag")
            return
        }
        scanInProgress = true
        update { it.copy(modal = NfcModalPhase.Scanning) }
        repository.scan(
            tag = tag,
            context = context,
            birthDate = current.mrz.birthDate,
            expireDate = current.mrz.expireDate,
            documentNumber = current.mrz.docNumber,
            onComplete = {
                Timber.i("V2 NFC: chip read")
                scanInProgress = false
                update { it.copy(modal = NfcModalPhase.Done) }
                viewModelScope.launch {
                    delay(DONE_DWELL_MS)
                    finish(success = true)
                }
            },
            onFailure = { error ->
                // Guard: a cancel already counted this scan, so ignore its late callback.
                if (!scanInProgress) return@scan
                scanInProgress = false
                onScanFailure(error)
            }
        )
    }

    /** Cancel from the scanning modal — counts an attempt (v1 cancelScan). */
    fun onCancelScan() {
        scanInProgress = false
        currentAttempt += 1
        if (currentAttempt >= maxAttempt) {
            currentAttempt = 0
            finish(success = false)
        } else {
            update { it.copy(modal = null) }
        }
    }

    /** User asked to enable NFC — surfaced to the screen to open system settings. */
    fun onEnableNfcClicked() = sendEffect(NfcScanEffect.OpenNfcSettings)

    private fun onScanFailure(error: String?) {
        currentAttempt += 1
        if (currentAttempt >= maxAttempt) {
            currentAttempt = 0
            finish(success = false)
            return
        }
        if (error != null) {
            // A specific read error → straight to MRZ correction (v1 ShowMRZCheck path).
            update { it.copy(modal = null, phase = NfcPhase.MrzCheck) }
        } else {
            // Generic failure → flash the modal error, then correction.
            update { it.copy(modal = NfcModalPhase.Error) }
            viewModelScope.launch {
                delay(ERROR_DWELL_MS)
                update { it.copy(modal = null, phase = NfcPhase.MrzCheck) }
            }
        }
    }

    private fun finish(success: Boolean) {
        Timber.i("V2 NFC: leg finished, success=$success (nfcOnly=$nfcOnly)")
        sendEffect(NfcScanEffect.Finished(success))
    }

    /** True when the device has an NFC adapter that is currently *disabled* (v1 disable branch). */
    private fun isNfcDisabled(context: Context): Boolean {
        var disabled = false
        deviceNFCState(
            context,
            available = { disabled = false },
            disable = { disabled = true },
            notSupported = { disabled = false }
        )
        return disabled
    }

    private inline fun update(transform: (NfcScanUiState) -> NfcScanUiState) {
        _state.value = _state.value?.let(transform)
    }

    private fun sendEffect(effect: NfcScanEffect) {
        viewModelScope.launch { _effects.send(effect) }
    }

    companion object {
        private const val DONE_DWELL_MS = 900L
        private const val ERROR_DWELL_MS = 900L

        val Factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T =
                NfcScanViewModel(NFCRepositoryImp()) as T
        }
    }
}
