package ai.amani.sdk.model

/**
 * Selects which UI implementation runs the KYC flow.
 *
 * - [V1] is the existing XML/Fragment UI (default, backward compatible).
 * - [V2] is the Jetpack Compose redesign.
 */
enum class UIStyle {
    V1,
    V2
}
