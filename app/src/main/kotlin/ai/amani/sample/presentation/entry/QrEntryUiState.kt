package ai.amani.sample.presentation.entry

import ai.amani.sample.domain.model.COUNTRIES
import ai.amani.sample.domain.model.Country
import ai.amani.sample.domain.model.ProfileUrlInfo
import ai.amani.sdk.model.UIStyle

/** Screen state for the entry screen. */
data class QrEntryUiState(
    val countries: List<Country> = COUNTRIES,
    val selected: Country = COUNTRIES.first(),
    val isResolving: Boolean = false,
    /** Which UI SDK style the KYC flow is started with. Matches the SDK's own default. */
    val uiStyle: UIStyle = UIStyle.V1
)

/** One-shot effects the Activity performs (SDK start / error message). */
sealed interface QrEntryEvent {
    data class StartKyc(
        val info: ProfileUrlInfo,
        val language: String,
        val uiStyle: UIStyle
    ) : QrEntryEvent
    data class Error(val message: String) : QrEntryEvent
}
