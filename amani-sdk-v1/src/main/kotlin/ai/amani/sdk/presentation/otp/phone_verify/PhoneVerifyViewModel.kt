package ai.amani.sdk.presentation.otp.phone_verify

import ai.amani.sdk.Amani
import ai.amani.sdk.extentions.Validator.Companion.isValidPhone
import ai.amani.sdk.interfaces.AmaniEventCallBack
import ai.amani.sdk.model.OTPScreenArgModel
import ai.amani.sdk.model.amani_events.error.AmaniError
import ai.amani.sdk.model.amani_events.error.AmaniErrorTypes
import ai.amani.sdk.model.amani_events.profile_status.ProfileStatus
import ai.amani.sdk.model.amani_events.steps_result.StepsResult
import ai.amani.sdk.presentation.common.BaseViewModel
import ai.amani.sdk.presentation.otp.phone_check.PhoneCheckFragmentArgs
import ai.amani.sdk.presentation.otp.phone_check.PhoneCheckStates
import androidx.lifecycle.viewModelScope
import datamanager.model.config.GeneralConfigs
import datamanager.model.config.StepConfig
import datamanager.model.config.Version
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * @Author: @zekiamani
 * @Date: 22.12.2023
 */
class PhoneVerifyViewModel: BaseViewModel() {

    private var enteredPhone = ""
    private var _uiState: MutableStateFlow<PhoneVerifyState> = MutableStateFlow(PhoneVerifyState.Empty)
    var uiState: StateFlow<PhoneVerifyState> = _uiState

    private var step: StepConfig? = null
    private var config: GeneralConfigs? = null
    private var steps: ArrayList<StepConfig> = arrayListOf()

    var version: Version? = null
    init {
        listenAmaniEvents()
    }
    fun setArgs(args: PhoneVerifyFragmentArgs) {
        this.step = args.data.steps.first()
        this.config = args.data.config
        this.steps = args.data.steps
        this.version = args.data.steps.first().mDocuments?.first()?.versions?.first()
    }
    fun enteredPhoneInput(phone : String?) {
        phone?.let {
            enteredPhone = it
            if (enteredPhone.isValidPhone()) {
                _uiState.value = PhoneVerifyState.Empty
            } else if (phone.isEmpty()) {
                _uiState.value = PhoneVerifyState.Empty
            } else _uiState.value = PhoneVerifyState.InvalidPhoneNumber(version?.invalidPhoneNumberError)
        }
    }

    fun onClickContinue() {
        viewModelScope.launch {
            updatePhoneNumber {
               if (it) sendPhoneOTP()
               else _uiState.value = PhoneVerifyState.SnakeMessage(
                   "Something gone wrong"
               )
            }
        }
    }

    private fun updatePhoneNumber(isSuccess: (Boolean) -> Unit){
        if (enteredPhone.isValidPhone()) {
            Amani.sharedInstance().CustomerInfo().setInfo(
                phoneNumber = enteredPhone
            )

            Amani.sharedInstance().CustomerInfo().upload {
                isSuccess.invoke(it)
            }
        } else {
            Timber.d("Entered phone is not valid")
            _uiState.value = PhoneVerifyState.InvalidPhoneNumber(version?.invalidPhoneNumberError)
        }
    }

    private fun sendPhoneOTP() {
        _uiState.value = PhoneVerifyState.Loading
        Amani.sharedInstance().CustomerInfo().sendPhoneOTP {
            if (it) {
                Timber.d("Send phone OTP process is success" +
                        "entered phone: $enteredPhone")
                _uiState.value = PhoneVerifyState.Success
                //Navigating to check phone screen
                navigateTo(PhoneVerifyFragmentDirections.actionVerifyPhoneFragmentToCheckPhoneFragment(
                    OTPScreenArgModel(
                        steps = steps,
                        config = config!!
                    )
                ))
            } else {
                Timber.d("Send phone OTP process is failed")
                _uiState.value = PhoneVerifyState.SnakeMessage(
                    "Something gone wrong"
                )
            }
        }
    }

    private fun listenAmaniEvents() {
        Amani.sharedInstance().AmaniEvent().setListener(
            object : AmaniEventCallBack {
                override fun onError(type: String?, error: ArrayList<AmaniError?>?) {
                    if (type == AmaniErrorTypes.CUSTOMER.name) {
                        _uiState.value = PhoneVerifyState.InvalidPhoneNumber(
                            error?.first()?.errorMessage.toString()
                        )
                    }
                }

                override fun profileStatus(profileStatus: ProfileStatus) {

                }

                override fun stepsResult(stepsResult: StepsResult?) {

                }
            }
        )
    }
}