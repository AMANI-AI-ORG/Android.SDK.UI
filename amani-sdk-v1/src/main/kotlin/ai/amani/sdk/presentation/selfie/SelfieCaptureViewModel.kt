package ai.amani.sdk.presentation.selfie

import ai.amani.sdk.modules.selfie.manual_capture.Selfie
import ai.amani.sdk.presentation.preview_screen.PreviewScreenState
import androidx.camera.core.Preview
import androidx.lifecycle.ViewModel
import datamanager.model.config.Version
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * @Author: zekiamani
 * @Date: 6.09.2022
 */
class SelfieCaptureViewModel: ViewModel() {

    private var _currentSelfieType: SelfieType = SelfieType.Manual

    private fun setCurrentSelfieType(selfieType: SelfieType) {
        _currentSelfieType = selfieType
    }

    private var _uiState : MutableStateFlow<SelfieCaptureUIState> = MutableStateFlow(SelfieCaptureUIState.FirstAnimation)
    var uiState: StateFlow<SelfieCaptureUIState> = _uiState

    private var version : Version? = null

    fun currentSelfieType(): SelfieType = _currentSelfieType

    fun confirmButtonClick(
    ) {
        when(_currentSelfieType) {

            SelfieType.Auto -> {
                // Auto Selfie State
                _uiState.value = SelfieCaptureUIState.Navigate(SelfieType.Auto)
            }

            SelfieType.Manual -> {
                // Manual Selfie state
                _uiState.value = SelfieCaptureUIState.Navigate(SelfieType.Manual)
            }

            else -> {
                // Pose Estimation state
                when(_uiState.value) {

                    is SelfieCaptureUIState.FirstAnimation -> {
                        _uiState.value = SelfieCaptureUIState.SecondAnimation
                    }

                    is SelfieCaptureUIState.SecondAnimation -> {
                        _uiState.value = SelfieCaptureUIState.Navigate(SelfieType.PoseEstimation(version!!.selfieType))
                    }

                    else -> {

                    }
                }
            }
        }
    }

    fun initialViewState(
        version: Version
    ) {
        this.version = version

        _uiState.value = SelfieCaptureUIState.FirstAnimation

        when(version.selfieType) {

            0 -> {
                setCurrentSelfieType(SelfieType.Auto)
            }

            -1 -> {
                setCurrentSelfieType(SelfieType.Manual)

            }

            else -> {
                setCurrentSelfieType(SelfieType.PoseEstimation(version.selfieType))
            }
        }
    }

}