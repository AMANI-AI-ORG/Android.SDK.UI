package ai.amani.sdk.presentation.nfc

import ai.amani.sdk.data.repository.nfc.NFCRepositoryImp
import ai.amani.sdk.extentions.deviceNFCState
import ai.amani.sdk.extentions.parcelable
import android.content.Context
import android.content.Intent
import android.nfc.NfcAdapter
import android.nfc.Tag
import androidx.lifecycle.*
import androidx.lifecycle.viewmodel.CreationExtras
import timber.log.Timber

/**
 * @Author: zekiamani
 * @Date: 26.09.2022
 */
class NFCSharedViewModel constructor(private val nfcRepository: NFCRepositoryImp): ViewModel() {

    private val _intent = MutableLiveData<Intent?>(null)
    private val _nfcActivationState = MutableLiveData<NFCActivationState>(NFCActivationState.Empty)
    private val _nfcScanState = MutableLiveData<NFCScanState>(NFCScanState.Empty)
    private val maxAttempt = 3
    private var currentAttempt = 0

    val get: LiveData<Intent?> = _intent

    val nfcActivationState: LiveData<NFCActivationState> = _nfcActivationState

    val nfcScanState: LiveData<NFCScanState> = _nfcScanState

    fun set(intent: Intent?) { _intent.value = intent }

    fun setNfcEnable(boolean: Boolean) {
        if (boolean) {
            _nfcActivationState.value = NFCActivationState.Enable
        } else _nfcActivationState.value = NFCActivationState.Disable
    }

    fun scanNFC(
        intent: Intent,
        context: Context,
        birthDate: String,
        expireDate: String,
        documentNumber: String
    ) {
        Timber.d("NFC Scan is triggered")

        val tag = intent.extras!!.parcelable<Tag>(NfcAdapter.EXTRA_TAG)

        nfcRepository.scan(
            tag!!,
            context,
            birthDate,
            expireDate,
            documentNumber,
            onComplete = {
                Timber.d("NFC onComplete is triggered")
                _nfcScanState.value = NFCScanState.Success
            },
            
            onFailure = {
                Timber.d("NFC onFailure is triggered")
                currentAttempt += 1
                if (currentAttempt >= maxAttempt) {
                    currentAttempt = 0
                    _nfcScanState.value = NFCScanState.OutOfMaxAttempt
                } else {
                    _nfcScanState.value = NFCScanState.Failure
                }
            }
        )
    }

    companion object {
        val Factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(
                modelClass: Class<T>,
                extras: CreationExtras
            ): T {

                return NFCSharedViewModel(
                    NFCRepositoryImp()
                ) as T
            }
        }
    }

    fun checkNFCState(
        context: Context,
        disable: () -> Unit,
        notSupported: () -> Unit
    ) {
        deviceNFCState(
            context,
            available = {
               setNfcEnable(true)
            },
            disable,
            notSupported
        )
    }

    /**
     * It should be called while navigate screen to another screen due to android life cycle
     * that why its activity view model that alive all activity life cycle that cause
     * NFCScan is still same when user come back to this screen if its not cleared while navigation.
     */
    fun clearNFCState() {
        _nfcScanState.value = NFCScanState.Empty
    }
}

sealed interface NFCActivationState {
    object Empty : NFCActivationState
    object Enable: NFCActivationState
    object Disable: NFCActivationState
}

sealed interface NFCScanState {
    object Empty : NFCScanState
    object Success: NFCScanState
    object Failure: NFCScanState
    object OutOfMaxAttempt: NFCScanState
}