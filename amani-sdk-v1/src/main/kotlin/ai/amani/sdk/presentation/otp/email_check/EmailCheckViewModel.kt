package ai.amani.sdk.presentation.otp.email_check

import ai.amani.amani_sdk.R
import ai.amani.sdk.Amani
import ai.amani.sdk.model.OTPScreenArgModel
import ai.amani.sdk.presentation.common.BaseViewModel
import ai.amani.sdk.presentation.common.CountdownTimerView
import ai.amani.sdk.presentation.otp.profile_info.ProfileInfoFragmentDirections
import ai.amani.sdk.utils.AppConstant
import androidx.navigation.NavDirections
import datamanager.model.config.GeneralConfigs
import datamanager.model.config.StepConfig
import datamanager.model.config.Version
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.lang.NullPointerException

/**
 * @Author: @zekiamani
 * @Date: 22.12.2023
 */
class EmailCheckViewModel: BaseViewModel() {

    private var _uiState: MutableStateFlow<EmailCheckStates> = MutableStateFlow(EmailCheckStates.Empty)
    var uiState: StateFlow<EmailCheckStates> = _uiState

    private var step: StepConfig? = null
    private var config: GeneralConfigs? = null
    private var steps: ArrayList<StepConfig> = arrayListOf()

    var version: Version? = null

    val timerState = object : CountdownTimerView.CountDownState {
        override fun started() {
            _uiState.value = EmailCheckStates.BackPressAvailability(false)
        }

        override fun finished() {
            _uiState.value = EmailCheckStates.BackPressAvailability(true)
        }
    }

    fun setArgs(args: EmailCheckFragmentArgs) {
        this.step = args.data.steps.first()
        this.config = args.data.config
        this.steps = args.data.steps
        this.version = args.data.steps.first().mDocuments?.first()?.versions?.first()
    }

    fun onClickVerifyButton(otp: String) {
        _uiState.value = EmailCheckStates.Loading
        Amani.sharedInstance().CustomerInfo().submitEmailOTP(
            otp = otp
        ) {
            if (it) {
                //OTP submit is success, ready to navigate next screen
                _uiState.value = EmailCheckStates.Success

                //If phone OTP is enabled navigate to Phone OTP fragment
                //If not navigate to Home KYC fragment
                getNavDirection(
                    returnToHomeKYC = {
                        navigateToHomeScreen()
                    },
                    navDirection = { direction ->
                        navigateTo(direction)
                    }

                )
            } else {
                //OTP submit is failed handle ui state errors
                _uiState.value = EmailCheckStates.InvalidOTP(version?.invalidEmailError?: "")
            }
        }
    }

    /**
     * Navigation direction will be decided accordingly sorted step list
     */
    private fun getNavDirection(returnToHomeKYC: () -> Unit, navDirection: (NavDirections) -> Unit) {
        val steps: ArrayList<StepConfig> = this.steps
        steps.remove(step)
        if (steps.isEmpty()) {
            //When steps is empty, it means there is not more OTP screen so time to navigate back to
            //KYC Home Screen
            returnToHomeKYC.invoke()
        }
        val direction: NavDirections? = when(steps.first().identifier) {
            AppConstant.IDENTIFIER_PHONE_OTP -> {
                EmailCheckFragmentDirections.actionCheckEmailFragmentToVerifyPhoneFragment(
                    OTPScreenArgModel(
                        steps = steps,
                        config = config!!
                    )
                )
            }

            AppConstant.IDENTIFIER_QUESTIONNAIRE -> {
                EmailCheckFragmentDirections.actionCheckEmailFragmentToQuestionnaireFragment(
                    OTPScreenArgModel(
                        steps = steps,
                        config = config!!
                    )
                )
            }

            AppConstant.IDENTIFIER_PROFILE_INFO -> {
                EmailCheckFragmentDirections.actionCheckEmailFragmentToProfileInfoFragment(
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

}