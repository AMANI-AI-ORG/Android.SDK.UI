package ai.amani.sdk.presentation_v2.home_kyc

import ai.amani.sdk.presentation_v2.theme.AmaniV2Palette

/**
 * Top-level UI state for the V2 HomeKYC flow, emitted by [HomeKYCViewModel].
 *
 * This is a superset of the stateless [HomeKYCScreenState] used by the screen +
 * previews: it also carries the [AmaniV2Palette] resolved from the server
 * GeneralConfigs, because colors and content arrive from the *same* config fetch.
 * The host activity applies [Ready.palette] to [ai.amani.sdk.presentation_v2.theme.AmaniV2Theme]
 * and feeds [Ready.content] into [HomeKYCScreen].
 *
 * While [Loading] the activity window stays translucent and only a centered loader
 * shows (the launching screen remains visible behind it). [Failed] surfaces the
 * SDK error code so the host can finish/report — it mirrors the v1
 * [ai.amani.sdk.presentation.home_kyc.HomeKYCState.Error] path.
 */
sealed interface HomeKYCState {

    data object Loading : HomeKYCState

    data class Ready(
        val palette: AmaniV2Palette,
        val content: HomeKYCUiState
    ) : HomeKYCState

    /**
     * Terminal failure. [errorCode] is the SDK/HTTP code and [exception] the throwable when
     * the failure came from one (a customer-detail fetch, say) — both are handed back to the
     * caller in the exit `KYCResult` so a closed SDK always reports why it closed.
     */
    data class Failed(
        val errorCode: Int,
        val exception: Throwable? = null
    ) : HomeKYCState
}
