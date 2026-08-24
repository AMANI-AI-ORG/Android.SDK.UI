package ai.amani.sdk.presentation_v2.nfc_scan

import datamanager.model.config.NfcV2AnimationStates
import datamanager.model.config.Version

/**
 * Server copy for the V2 NFC animation states, read from the document version's `nfcV2` block:
 *
 * ```json
 * "nfcV2": {
 *   "animationHint": "Follow the instructions in the animation below",
 *   "animationStates": {
 *     "place": "Place the document behind your phone",
 *     "detected": "Chip located",
 *     "hold": "Hold still",
 *     "reading": "Reading…",
 *     "dontMove": "Don't move your phone",
 *     "remove": "You can take the phone away",
 *     "retry": "Try again and reposition the phone",
 *     "success": "Read complete"
 *   }
 * }
 * ```
 *
 * A missing block, or a blank value on it, falls back to the SDK default for that state.
 */
internal object NfcV2AnimationCopy {

    /** SDK defaults, used for any state the server didn't provide. */
    val DEFAULTS: Map<String, String> = NfcV2AnimationStates().asMap()

    /**
     * Server copy for [version], merged over [DEFAULTS] so every state always resolves to a
     * non-blank string.
     */
    fun statesFor(version: Version?): Map<String, String> {
        val server = version?.nfcV2?.animationStates?.asMap() ?: return DEFAULTS
        return DEFAULTS.mapValues { (key, default) ->
            server[key]?.takeIf { it.isNotBlank() } ?: default
        }
    }

    /** State key → copy, in the order the animation plays the states. */
    private fun NfcV2AnimationStates.asMap(): Map<String, String> = mapOf(
        "place" to place,
        "detected" to detected,
        "hold" to hold,
        "reading" to reading,
        "dontMove" to dontMove,
        "remove" to remove,
        "retry" to retry,
        "success" to success
    )
}
