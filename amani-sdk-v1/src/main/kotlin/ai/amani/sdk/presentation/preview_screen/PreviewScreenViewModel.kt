package ai.amani.sdk.presentation.preview_screen

import ai.amani.amani_sdk.R
import ai.amani.sdk.data.repository.nfc.NFCRepository
import ai.amani.sdk.data.repository.nfc.NFCRepositoryImp
import ai.amani.sdk.extentions.deviceHasNFC
import ai.amani.sdk.model.MRZModel
import ai.amani.sdk.presentation.home_kyc.ScreenRoutes
import ai.amani.sdk.utils.AmaniDocumentTypes
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import datamanager.model.config.Version
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import timber.log.Timber

/**
 * @Author: zekiamani
 * @Date: 13.09.2022
 */
class PreviewScreenViewModel constructor(private val nfcRepository: NFCRepository) : ViewModel() {

    enum class Step {
        FRONT_SIDE,
        BACK_SIDE
    }

    private var _uiState: MutableStateFlow<PreviewScreenState> = MutableStateFlow(PreviewScreenState.Loaded)
    private var currentStep: Step = Step.FRONT_SIDE // Default value is FrontSide

    private var maxAttempt = 3 //Max attempt to request MRZ as default 3, while out of max attempt
    private var currentAttempt = 0 //Current attempt of requesting MRZ

    var uiState : StateFlow<PreviewScreenState> = _uiState
    var mrzModel: MRZModel? = null

    /** Checks whether the current step is FrontSide or BackSide
     * @param frontSide : Boolean equation of current displayed image is front
     * side of document or not */
    private fun setStep(
        frontSide: Boolean?
    ) {
        currentStep = when {
            frontSide == null -> Step.FRONT_SIDE
            frontSide -> Step.FRONT_SIDE
            else -> Step.BACK_SIDE
        }
    }

    /** Navigates screen accordingly version && frontSide
     *
     * @param version : Version of current document
     * @param frontSide: Boolean equation of CurrentDisplayed document image
     */
    fun navigateScreen(
        context: Context,
        version: Version,
        frontSide: Boolean?,
        navigateTo: (route: ScreenRoutes) -> Unit
    ) {

        setStep(frontSide)

        version.steps.apply {
            if (!this.isNullOrEmpty()) {
                when {
                    version.getDocumentId().equals(AmaniDocumentTypes.SELFIE) -> {
                        // Means; current document is Selfie, there is no back side to care about
                        // so navigating HomeScreen to start upload process
                        navigateTo.invoke(ScreenRoutes.HomeKYCScreen)
                    }
                    this.size > 1 -> {
                        // If steps size bigger than 1 that means
                        // current document (ID.PA,DL) has multiple side front && back.

                        // If current step is back side there is no other side
                        // navigate HomeScreen for upload process
                        if (currentStep == Step.BACK_SIDE) {
                            if (version.nfc && deviceHasNFC(context)) {
                                // NFC is active, navigate to NFC screen
                                _uiState.value = PreviewScreenState.Loading
                                nfcRepository.getMRZ(
                                    version.type,
                                    onComplete = {

                                        if (!it.mRZBirthDate.isNullOrEmpty() &&
                                               !it.mRZDocumentNumber.isNullOrEmpty() &&
                                            !it.mRZExpiryDate.isNullOrEmpty()) {

                                            Timber.i("MRZ fetched")
                                            mrzModel = MRZModel(
                                                it.mRZBirthDate!!,
                                                it.mRZExpiryDate!!,
                                                it.mRZDocumentNumber!!
                                            )
                                            _uiState.value = PreviewScreenState.Loaded
                                            navigateTo(ScreenRoutes.NFCScanScreen)

                                        } else {
                                            currentAttempt += 1
                                            Timber.e("MRZ could not fetch, current attempt: $currentAttempt")
                                            if (currentAttempt >= maxAttempt) {
                                                Timber.e("Out of max attempt")
                                                resetCurrentAttempt()
                                                _uiState.value = PreviewScreenState.OutOfMaxAttempt
                                                resetCurrentAttempt()
                                            } else{
                                                _uiState.value = PreviewScreenState.Error(R.string.mrz_fetch_error)
                                            }
                                        }
                                    },
                                    onError = {
                                        currentAttempt += 1
                                        Timber.e("MRZ could not fetch, current attempt: $currentAttempt")
                                        if (currentAttempt >= maxAttempt) {
                                            Timber.e("Out of max attempt")
                                            resetCurrentAttempt()
                                            _uiState.value = PreviewScreenState.OutOfMaxAttempt
                                        } else{
                                            _uiState.value = PreviewScreenState.Error(R.string.mrz_fetch_error)
                                        }
                                    }
                                )

                            } else navigateTo.invoke(ScreenRoutes.HomeKYCScreen)
                        } else if (currentStep == Step.FRONT_SIDE) {
                            // Current step is FrontSide navigate for back side of document
                            navigateTo.invoke(ScreenRoutes.IDBackSideScreen)
                        }
                    }
                    else -> {
                        if (version.nfc && deviceHasNFC(context)) {
                            // NFC is active, navigate to NFC screen
                            _uiState.value = PreviewScreenState.Loading
                            nfcRepository.getMRZ(
                                version.type,
                                onComplete = {

                                    if (!it.mRZBirthDate.isNullOrEmpty() &&
                                        !it.mRZDocumentNumber.isNullOrEmpty() &&
                                        !it.mRZExpiryDate.isNullOrEmpty()) {

                                        Timber.i("MRZ fetched")
                                        mrzModel = MRZModel(
                                            it.mRZBirthDate!!,
                                            it.mRZExpiryDate!!,
                                            it.mRZDocumentNumber!!
                                        )
                                        _uiState.value = PreviewScreenState.Loaded
                                        navigateTo(ScreenRoutes.NFCScanScreen)

                                    } else {
                                        currentAttempt += 1
                                        Timber.e("MRZ could not fetch, current attempt: $currentAttempt")
                                        if (currentAttempt >= maxAttempt) {
                                            Timber.e("Out of max attempt")
                                            resetCurrentAttempt()
                                            _uiState.value = PreviewScreenState.OutOfMaxAttempt
                                            resetCurrentAttempt()
                                        } else{
                                            _uiState.value = PreviewScreenState.Error(R.string.mrz_fetch_error)
                                        }
                                    }
                                },
                                onError = {
                                    currentAttempt += 1
                                    Timber.e("MRZ could not fetch, current attempt: $currentAttempt")
                                    if (currentAttempt >= maxAttempt) {
                                        Timber.e("Out of max attempt")
                                        resetCurrentAttempt()
                                        _uiState.value = PreviewScreenState.OutOfMaxAttempt
                                    } else{
                                        _uiState.value = PreviewScreenState.Error(R.string.mrz_fetch_error)
                                    }
                                }
                            )
                        }
                        else navigateTo.invoke(ScreenRoutes.HomeKYCScreen)

                    }
                }
            }
        }
    }

    fun resetUIState() {
        _uiState.value = PreviewScreenState.Loaded
    }

    fun setMaxAttempt(
        version: Version?
    ) {
        version?.let {
            it.maxAttempt?.let { maxAttempt ->
                this.maxAttempt = maxAttempt
            }
        }
    }

    private fun resetCurrentAttempt() {
        currentAttempt = 0
    }


    companion object {
        val Factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(
                modelClass: Class<T>,
                extras: CreationExtras
            ): T {

                return PreviewScreenViewModel(
                    NFCRepositoryImp()
                ) as T
            }
        }
    }
}