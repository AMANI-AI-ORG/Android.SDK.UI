package ai.amani.sample.presentation.entry

import ai.amani.sample.domain.model.COUNTRIES
import ai.amani.sample.domain.model.Country
import ai.amani.sample.domain.model.ProfileUrlInfo

/** Screen state for the entry screen. */
data class QrEntryUiState(
    val countries: List<Country> = COUNTRIES,
    val selected: Country = COUNTRIES.first(),
    val isResolving: Boolean = false
)

/** One-shot effects the Activity performs (SDK start / error message). */
sealed interface QrEntryEvent {
    data class StartKyc(val info: ProfileUrlInfo, val language: String) : QrEntryEvent
    data class Error(val message: String) : QrEntryEvent
}
