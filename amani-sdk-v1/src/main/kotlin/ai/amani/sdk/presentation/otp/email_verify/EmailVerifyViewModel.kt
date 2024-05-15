package ai.amani.sdk.presentation.otp.email_verify

import ai.amani.amani_sdk.R
import ai.amani.sdk.Amani
import ai.amani.sdk.extentions.Validator.Companion.isValidEmail
import ai.amani.sdk.interfaces.AmaniEventCallBack
import ai.amani.sdk.interfaces.IUploadCallBack
import ai.amani.sdk.model.OTPScreenArgModel
import ai.amani.sdk.model.amani_events.error.AmaniError
import ai.amani.sdk.model.amani_events.error.AmaniErrorTypes
import ai.amani.sdk.model.amani_events.profile_status.ProfileStatus
import ai.amani.sdk.model.amani_events.steps_result.StepsResult
import ai.amani.sdk.presentation.common.BaseViewModel
import androidx.lifecycle.viewModelScope
import datamanager.model.config.GeneralConfigs
import datamanager.model.config.StepConfig
import datamanager.model.config.Version
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * @Author: @zekiamani
 * @Date: 21.12.2023
 */
class EmailVerifyViewModel: BaseViewModel() {

    private var enteredEmail = ""
    private var _uiState: MutableStateFlow<EmailVerifyStates> = MutableStateFlow(EmailVerifyStates.Empty)
    var uiState: StateFlow<EmailVerifyStates> = _uiState

    var step: StepConfig? = null
    private var config: GeneralConfigs? = null
    private var steps: ArrayList<StepConfig> = arrayListOf()

    var version: Version? = null

    init {
        listenAmaniEvents()
    }

    fun setArgs(args: EmailVerifyFragmentArgs) {
        this.step = args.data.steps.first()
        this.config = args.data.config
        this.steps = args.data.steps
        this.version = args.data.steps.first().mDocuments?.first()?.versions?.first()
    }
    fun enteredEmailInput(email: String?) {
        email?.let {
            enteredEmail = it
            if (enteredEmail.isValidEmail()) {
                _uiState.value = EmailVerifyStates.Empty
            } else if (email.isEmpty()) {
                _uiState.value = EmailVerifyStates.Empty
            } else _uiState.value = EmailVerifyStates.InvalidEmail(version?.invalidEmailError?: "")
        }
    }

    fun onClickContinue() {
        viewModelScope.launch {
            updateUserEmail (
                succeed = {
                    sendEmailOTP()
                }
            )
        }
    }

    private fun updateUserEmail(
        succeed: () -> Unit
    ) {
        if (enteredEmail.isValidEmail()) {
            _uiState.value = EmailVerifyStates.Loading
            Amani.sharedInstance().CustomerInfo().setInfo(
                email = enteredEmail
            )

            Amani.sharedInstance().CustomerInfo().upload {
                if (it) {
                    succeed.invoke()
                    Timber.d("Use email is updated successfully")
                } else {
                    Timber.d("User email could not updated")
                    _uiState.value = EmailVerifyStates.SnakeMessage("Something gone wrong please try again")
                }
            }
        } else {
            Timber.d("Entered email is not valid")
            _uiState.value = EmailVerifyStates.InvalidEmail(version?.invalidEmailError?: "")
        }
    }

    private fun sendEmailOTP() {
        Amani.sharedInstance().CustomerInfo().sendEmailOTP {
            if (it) {
                _uiState.value = EmailVerifyStates.Success
                navigateTo(EmailVerifyFragmentDirections.actionVerifyEmailFragmentToCheckEmailFragment(
                    OTPScreenArgModel(
                        steps = steps,
                        config = config!!
                    )
                ))
            } else {
                _uiState.value = EmailVerifyStates.SnakeMessage("Error happened during send email OTP")
            }
        }
    }

    private fun listenAmaniEvents() {
        Amani.sharedInstance().AmaniEvent().setListener(
            object : AmaniEventCallBack{
                override fun onError(type: String?, error: ArrayList<AmaniError?>?) {
                    if (type == AmaniErrorTypes.CUSTOMER.name) {
                        _uiState.value = EmailVerifyStates.InvalidEmail(error?.first()?.errorMessage.toString())
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