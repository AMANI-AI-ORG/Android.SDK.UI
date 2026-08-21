package ai.amani.sample.presentation.entry

import ai.amani.sample.data.remote.ProfileUrlRemoteDataSource
import ai.amani.sample.data.repository.ProfileUrlRepositoryImpl
import ai.amani.sample.domain.model.Country
import ai.amani.sample.domain.usecase.GetProfileUrlUseCase
import ai.amani.sdk.model.UIStyle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Owns the entry screen state and the QR → profile resolution. The Activity keeps only the
 * platform bits (scanning, launching the SDK) and reacts to [events].
 */
class QrEntryViewModel(
    private val getProfileUrl: GetProfileUrlUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(QrEntryUiState())
    val uiState = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<QrEntryEvent>(extraBufferCapacity = 1)
    val events = _events.asSharedFlow()

    fun onCountrySelected(country: Country) {
        _uiState.update { it.copy(selected = country) }
    }

    fun onUiStyleSelected(style: UIStyle) {
        _uiState.update { it.copy(uiStyle = style) }
    }

    fun onQrScanned(scannedUrl: String) {
        if (_uiState.value.isResolving) return
        _uiState.update { it.copy(isResolving = true) }
        viewModelScope.launch {
            val info = getProfileUrl(scannedUrl)
            _uiState.update { it.copy(isResolving = false) }
            if (info == null) {
                _events.emit(QrEntryEvent.Error("Invalid QR or could not resolve profile"))
            } else {
                _events.emit(
                    QrEntryEvent.StartKyc(
                        info = info,
                        language = _uiState.value.selected.language,
                        uiStyle = _uiState.value.uiStyle
                    )
                )
            }
        }
    }

    /** Manual DI factory (no Hilt in this demo). */
    class Factory : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val repository = ProfileUrlRepositoryImpl(ProfileUrlRemoteDataSource())
            return QrEntryViewModel(GetProfileUrlUseCase(repository)) as T
        }
    }
}
