package ai.amani.sdk.presentation.otp.profile_info

import ai.amani.base.utility.AppConstants
import ai.amani.sdk.Amani
import ai.amani.sdk.interfaces.AmaniEventCallBack
import ai.amani.sdk.model.OTPScreenArgModel
import ai.amani.sdk.model.amani_events.error.AmaniError
import ai.amani.sdk.model.amani_events.profile_status.ProfileStatus
import ai.amani.sdk.model.amani_events.steps_result.StepsResult
import ai.amani.sdk.presentation.common.BaseViewModel
import ai.amani.sdk.utils.AppConstant
import android.provider.ContactsContract.Profile
import androidx.navigation.NavDirections
import datamanager.model.config.GeneralConfigs
import datamanager.model.config.StepConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import timber.log.Timber
import java.lang.NullPointerException

/**
 * @Author: @zekiamani
 * @Date: 9.01.2024
 */
class ProfileInfoViewModel: BaseViewModel() {

    private var _uiState: MutableStateFlow<ProfileInfoState> = MutableStateFlow(ProfileInfoState.Empty)
    var uiState: StateFlow<ProfileInfoState> = _uiState
    private var step: StepConfig? = null
    private var config: GeneralConfigs? = null
    private var steps: ArrayList<StepConfig> = arrayListOf()
    private var name: String = ""
    private var surname: String = ""
    private var birthDate: String = ""

    init {
        setAmaniEventListener()
    }

    fun setArgs(args: OTPScreenArgModel) {
        steps = args.steps
        args.steps.forEach {
            if (it.identifier == "profile_info") {
                step = it
            }
        }

        config = args.config
    }

    fun currentStep(): StepConfig = step!!

    fun data(name: String? = "", surname: String? = "", birthDate: String? = "") {
        if (!name.isNullOrEmpty()) this.name = name
        if (!surname.isNullOrEmpty()) this.surname = surname
        if (!birthDate.isNullOrEmpty()) this.birthDate = birthDate
    }

    fun buttonClick() {
        if (name.isEmpty()) {
            _uiState.value = ProfileInfoState.NameIsEmpty
            return
        }
        if (surname.isEmpty()){
            _uiState.value = ProfileInfoState.SurnameIsEmpty
            return
        }
        if (birthDate.isEmpty()) {
            _uiState.value = ProfileInfoState.BirthdateIsEmpty
            return
        }

        //Update the profile
        updateProfile()
    }

    private fun updateProfile () {
        _uiState.value = ProfileInfoState.Loading
        Amani.sharedInstance().CustomerInfo().setInfo(
            fullName = "$name $surname",
            birthDate = birthDate
        )

        Amani.sharedInstance().CustomerInfo().upload{
            if(!it) _uiState.value = ProfileInfoState.Error("Something gone wrong")
        }
    }

    /**
     * Navigation direction will be decided accordingly sorted step list
     */
    private fun getNavDirection(
        returnToHomeKYC: () -> Unit,
        navDirection: (NavDirections) -> Unit
    ) {
        val steps: ArrayList<StepConfig> = this.steps
        steps.remove(step)
        if (steps.isEmpty()) {
            //If steps is empty returning to HomeKYC
            returnToHomeKYC.invoke()
            return
        }
       val direction: NavDirections? = when(steps.first().identifier) {
            AppConstant.IDENTIFIER_PHONE_OTP -> {
                ProfileInfoFragmentDirections.actionProfileInfoFragmentToVerifyPhoneFragment(
                    OTPScreenArgModel(
                        steps = steps,
                        config = config!!
                    )
                )
            }

            AppConstant.IDENTIFIER_EMAIL_OTP -> {
                ProfileInfoFragmentDirections.actionProfileInfoFragmentToVerifyEmailFragment(
                    OTPScreenArgModel(
                        steps = steps,
                        config = config!!
                    )
                )
            }

           AppConstant.IDENTIFIER_QUESTIONNAIRE -> {
               ProfileInfoFragmentDirections.actionProfileInfoFragmentToQuestionnaireFragment(
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

    private fun setAmaniEventListener() {
        Amani.sharedInstance().AmaniEvent().setListener(object : AmaniEventCallBack{
            override fun onError(type: String?, error: ArrayList<AmaniError?>?) {
            }

            override fun profileStatus(profileStatus: ProfileStatus) {
            }

            override fun stepsResult(stepsResult: StepsResult?) {
                stepsResult?.result?.forEach {
                    if (it.id == step?.id) {
                        when(it.status) {
                            AppConstant.STATUS_APPROVED -> {
                                getNavDirection(
                                    returnToHomeKYC = {
                                        navigateToHomeScreen()
                                    },

                                    navDirection = {
                                        navigateTo(it)
                                    }
                                )
                            }

                            AppConstant.STATUS_REJECTED -> {
                                _uiState.value = ProfileInfoState.Error(
                                    "Profile info step is rejected"
                                )
                            }
                        }
                    }
                }
            }
        })
    }
}

