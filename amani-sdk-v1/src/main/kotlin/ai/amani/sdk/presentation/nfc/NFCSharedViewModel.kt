package ai.amani.sdk.presentation.nfc

import ai.amani.sdk.data.repository.nfc.NFCRepositoryImp
import ai.amani.sdk.extentions.deviceNFCState
import ai.amani.sdk.extentions.parcelable
import ai.amani.sdk.model.MRZModel
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
    private val _nfcScanState = MutableLiveData<NFCScanState>(NFCScanState.ReadyToScan)
    private val maxAttempt = 3
    private var currentAttempt = 0

    val get: LiveData<Intent?> = _intent

    val nfcActivationState: LiveData<NFCActivationState> = _nfcActivationState

    val nfcScanState: LiveData<NFCScanState> = _nfcScanState

    var mrzData = MRZModel()

    fun setMRZ(mrzModel: MRZModel) {
        mrzData = mrzModel
    }

    fun set(intent: Intent?) { _intent.value = intent }

    fun setNfcEnable(boolean: Boolean) {
        if (boolean) {
            _nfcActivationState.value = NFCActivationState.Enable
        } else _nfcActivationState.value = NFCActivationState.Disable
    }

    fun scanNFC(
        intent: Intent,
        context: Context
    ) {
        Timber.d("NFC Scan is triggered")

        val tag = intent.extras!!.parcelable<Tag>(NfcAdapter.EXTRA_TAG)

        nfcRepository.scan(
            tag = tag!!,
            context = context,
            birthDate = mrzData.birthDate,
            expireDate = mrzData.expireDate,
            documentNumber = mrzData.docNumber,
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
                    it?.let { error ->
                        _nfcScanState.value = NFCScanState.ShowMRZCheck
                        return@let
                    }
                    _nfcScanState.value = NFCScanState.Failure
                }
            }
        )
    }

    fun setState(state: NFCScanState) {
        _nfcScanState.value = state
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
     * Continue button is clicked. MRZ values are checked.
     * State ready to scan NFC
     */
    fun continueBtnClick() {
        _nfcScanState.value = NFCScanState.ReadyToScan
    }

    /**
     * It should be called while navigate screen to another screen due to android life cycle
     * that why its activity view model that alive all activity life cycle that cause
     * NFCScan is still same when user come back to this screen if its not cleared while navigation.
     */
    fun clearNFCState() {
        _nfcScanState.value = NFCScanState.ReadyToScan
    }
}

sealed interface NFCActivationState {
    object Empty : NFCActivationState
    object Enable: NFCActivationState
    object Disable: NFCActivationState
}

sealed interface NFCScanState {
    data object ReadyToScan : NFCScanState
    data object ShowMRZCheck: NFCScanState
    data object Success: NFCScanState
    data object Failure : NFCScanState
    data object OutOfMaxAttempt: NFCScanState
}