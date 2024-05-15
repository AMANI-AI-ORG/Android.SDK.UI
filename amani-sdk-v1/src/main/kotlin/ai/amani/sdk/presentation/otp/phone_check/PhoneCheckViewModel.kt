package ai.amani.sdk.presentation.otp.phone_check

import ai.amani.sdk.Amani
import ai.amani.sdk.model.OTPScreenArgModel
import ai.amani.sdk.presentation.common.BaseViewModel
import ai.amani.sdk.presentation.common.CountdownTimerView
import ai.amani.sdk.utils.AppConstant
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavDirections
import datamanager.model.config.GeneralConfigs
import datamanager.model.config.StepConfig
import datamanager.model.config.Version
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.lang.NullPointerException

/**
 * @Author: @zekiamani
 * @Date: 22.12.2023
 */
class PhoneCheckViewModel: BaseViewModel() {

    private var enteredOTP = ""
    private var _uiState: MutableStateFlow<PhoneCheckStates> = MutableStateFlow(PhoneCheckStates.Empty)
    var uiState: StateFlow<PhoneCheckStates> = _uiState

    private var step: StepConfig? = null
    private var config: GeneralConfigs? = null
    private var steps: ArrayList<StepConfig> = arrayListOf()

    var version: Version? = null

    val timerState = object : CountdownTimerView.CountDownState {
        override fun started() {
            _uiState.value = PhoneCheckStates.BackPressAvailability(false)
        }

        override fun finished() {
            _uiState.value = PhoneCheckStates.BackPressAvailability(true)
        }
    }

    fun setArgs(args: PhoneCheckFragmentArgs) {
        this.step = args.data.steps.first()
        this.config = args.data.config
        this.steps = args.data.steps
        this.version = args.data.steps.first().mDocuments?.first()?.versions?.first()
    }

    fun enteredOTPInput(otp: String?) {
        otp?.let {
            enteredOTP = it
        }
    }

    fun onClickContinue() {
        viewModelScope.launch {
            submitOTP()
        }
    }


    /**
     * Navigation direction will be decided accordingly sorted step list
     */
    private fun getNavDirection(returnToHomeKYC: () -> Unit, navDirection: (NavDirections) -> Unit) {
        val steps: ArrayList<StepConfig> = this.steps
        steps.remove(step)
        if (steps.isEmpty()) {
            //When steps is empty, it means there is no more other OTP screen so time to navigate back to
            //KYC Home Screen
            returnToHomeKYC.invoke()
            return
        }
        val direction: NavDirections? = when(steps.first().identifier) {
            AppConstant.IDENTIFIER_QUESTIONNAIRE -> {
                PhoneCheckFragmentDirections.actionCheckPhoneFragmentToQuestionnaireFragment(
                    OTPScreenArgModel(
                        steps = steps,
                        config = config!!
                    )
                )
            }

            AppConstant.IDENTIFIER_PROFILE_INFO -> {
                PhoneCheckFragmentDirections.actionCheckPhoneFragmentToProfileInfoFragment(
                    OTPScreenArgModel(
                        steps = steps,
                        config = config!!
                    )
                )
            }

            AppConstant.IDENTIFIER_EMAIL_OTP -> {
                PhoneCheckFragmentDirections.actionCheckPhoneFragmentToVerifyEmailFragment(
                    OTPScreenArgModel(
                        steps = steps,
                        config = config!!
                    )
                )
            }

            else -> {
                null
            }
        }

        if (direction == null) throw Exception(NullPointerException("Nav direction is null"))
        else navDirection.invoke(direction)
    }

    private fun submitOTP() {
        _uiState.value = PhoneCheckStates.Loading
        Amani.sharedInstance().CustomerInfo().submitPhoneOTP(
            otp = enteredOTP
        ) {
            if (it) {
                _uiState.value = PhoneCheckStates.Success
                getNavDirection(
                    returnToHomeKYC = {
                        navigateToHomeScreen()
                    },
                    navDirection = {direction ->
                        navigateTo(direction)
                    }
                )
            } else {
                _uiState.value = PhoneCheckStates.InvalidOTP
            }
        }
    }
}