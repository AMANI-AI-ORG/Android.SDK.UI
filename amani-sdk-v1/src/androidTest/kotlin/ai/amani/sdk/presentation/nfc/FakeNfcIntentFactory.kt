package ai.amani.sdk.presentation.nfc

import android.content.Intent
import android.nfc.NdefMessage
import android.nfc.NdefRecord
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.IsoDep
import io.mockk.every
import io.mockk.mockk

/**
 * Factory for constructing fake NFC [Intent]s and [Tag]s used in instrumentation tests.
 *
 * NFC hardware is unavailable in CI and emulators, so we synthesise the same
 * [Intent] shape that the Android NFC framework delivers via foreground dispatch:
 *
 *   action  = [NfcAdapter.ACTION_TAG_DISCOVERED] or [NfcAdapter.ACTION_NDEF_DISCOVERED]
 *   extra   = [NfcAdapter.EXTRA_TAG]  → a (mocked) [Tag]
 *   extra   = [NfcAdapter.EXTRA_NDEF_MESSAGES] → optional [NdefMessage] array
 *
 * Usage:
 * ```kotlin
 * val intent = FakeNfcIntentFactory.tagDiscoveredIntent()
 * viewModel.set(intent)
 * ```
 */
object FakeNfcIntentFactory {

    // ---------------------------------------------------------------------------
    // Public factory methods
    // ---------------------------------------------------------------------------

    /**
     * Minimal [NfcAdapter.ACTION_TAG_DISCOVERED] intent with a mock [Tag].
     * This is what the activity receives in [android.app.Activity.onNewIntent]
     * when an NFC tag enters the foreground dispatch field.
     */
    fun tagDiscoveredIntent(tag: Tag = mockTag()): Intent =
        buildNfcIntent(NfcAdapter.ACTION_TAG_DISCOVERED, tag)

    /**
     * An [NfcAdapter.ACTION_NDEF_DISCOVERED] intent carrying a plain-text NDEF record.
     */
    fun ndefDiscoveredIntent(
        text: String = "AMANI_TEST",
        tag: Tag = mockTag()
    ): Intent {
        val ndefRecord = NdefRecord.createTextRecord("en", text)
        val ndefMessage = NdefMessage(arrayOf(ndefRecord))
        return buildNfcIntent(NfcAdapter.ACTION_NDEF_DISCOVERED, tag).apply {
            putExtra(NfcAdapter.EXTRA_NDEF_MESSAGES, arrayOf(ndefMessage))
        }
    }

    /**
     * An intent with an [IsoDep]-technology tag — the technology used by ePassport chips.
     * The [NFCSharedViewModel] and [AmaniMainActivity] foreground dispatch both expect IsoDep.
     */
    fun isoDepIntent(tag: Tag = mockTag()): Intent =
        buildNfcIntent(NfcAdapter.ACTION_TECH_DISCOVERED, tag)

    /**
     * An intent that deliberately carries **no** [NfcAdapter.EXTRA_TAG] extra,
     * representing a corrupted or malformed NFC delivery.
     */
    fun corruptedIntent(): Intent =
        Intent(NfcAdapter.ACTION_TAG_DISCOVERED)
        // No EXTRA_TAG — triggers NPE path in ViewModel

    /**
     * Returns a sequence of [n] distinct tag-discovered intents to simulate
     * rapid repeated scans without removing the tag from the field.
     */
    fun rapidScanIntents(n: Int): List<Intent> =
        (1..n).map { tagDiscoveredIntent() }

    // ---------------------------------------------------------------------------
    // Mock Tag
    // ---------------------------------------------------------------------------

    /**
     * Creates a MockK-backed [Tag] with the minimum surface needed by the SDK.
     *
     * [Tag] is a final Android framework class; we use MockK's `mockk<Tag>(relaxed = true)`
     * so that unspecified calls return safe defaults automatically.
     *
     * Note: if the core AmaniSDK inspects [Tag.getTechList], stub it here.
     */
    fun mockTag(): Tag = mockk<Tag>(relaxed = true) {
        every { techList } returns arrayOf(IsoDep::class.java.name)
    }

    // ---------------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------------

    private fun buildNfcIntent(action: String, tag: Tag): Intent =
        Intent(action).apply {
            putExtra(NfcAdapter.EXTRA_TAG, tag)
        }
}
