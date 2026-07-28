package ai.amani.sdk.presentation_v2.home_kyc

import ai.amani.sdk.model.customer.Rule

/**
 * One-shot side effects emitted by [HomeKYCViewModel] — things that should happen
 * once (navigation, completion, terminal error), as opposed to durable UI state in
 * [HomeKYCState]. Collected by the host as a flow so each effect fires exactly once
 * across recompositions/config changes (the v2 counterpart of the v1 `_logicEvent` +
 * `navigateTo` split).
 */
sealed interface HomeKYCEffect {

    /**
     * The user picked a step that is ready to start. The concrete capture/route
     * decision (single vs. multi document, ID/Selfie/NFC, etc.) is resolved during
     * the wiring phase from [rule] + the cached version list; the host then pushes the
     * matching [ai.amani.sdk.presentation_v2.navigation.AmaniV2Destination].
     */
    data class StartStep(val rule: Rule) : HomeKYCEffect

    /** All KYC steps are approved — the flow is complete. */
    data object ProfileApproved : HomeKYCEffect

    /** Terminal error (login/config/customer-detail). Carries the SDK error code. */
    data class Error(val errorCode: Int) : HomeKYCEffect
}
