package ai.amani.sdk.presentation_v2.profile_info

import ai.amani.sdk.Amani
import ai.amani.sdk.interfaces.AmaniEventCallBack
import ai.amani.sdk.model.amani_events.error.AmaniError
import ai.amani.sdk.model.amani_events.profile_status.ProfileStatus
import ai.amani.sdk.model.amani_events.steps_result.StepsResult
import ai.amani.sdk.presentation.home_kyc.CachingHomeKYC
import ai.amani.sdk.utils.AppConstant
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * UI state for [ProfileInfoScreen]. Config-driven labels/hints come from the profile_info
 * step's [Version][datamanager.model.config.Version]; the three field values are edited in
 * place. [submitEnabled] mirrors v1's validation (all three fields required).
 */
data class ProfileInfoUiState(
    val headerTitle: String,
    val description: String,
    val nameTitle: String,
    val nameHint: String,
    val surnameTitle: String,
    val surnameHint: String,
    val birthDateTitle: String,
    val birthDateHint: String,
    val continueText: String,
    val name: String = "",
    val surname: String = "",
    val birthDate: String = "",
    val submitting: Boolean = false,
    val error: String? = null
) {
    val submitEnabled: Boolean
        get() = name.isNotBlank() && surname.isNotBlank() && birthDate.isNotBlank() && !submitting
}

/**
 * V2 profile-info view model — the Compose port of v1's ProfileInfoViewModel.
 *
 * Behaviour mirrors v1:
 *  - config labels/hints are read from the profile_info step's version
 *  - "Continue" requires all three fields (name / surname / birth date)
 *  - submit calls `CustomerInfo().setInfo(fullName, birthDate)` + `upload()`, then waits for the
 *    step's AmaniEvent `stepsResult` verdict: APPROVED emits [completed] (advance the flow),
 *    REJECTED / a failed upload surfaces an error.
 *
 * Like v1 this registers its own AmaniEvent listener while active; the host re-attaches the
 * HomeKYC listener when the pre-KYC chain returns to Home (see AmaniV2NavHost).
 */
class ProfileInfoViewModel : ViewModel() {

    private val step = CachingHomeKYC.appConfig?.stepConfigs
        ?.firstOrNull { it.identifier == AppConstant.IDENTIFIER_PROFILE_INFO }
    private val version = step?.mDocuments?.firstOrNull()?.versions?.firstOrNull()
    private val stepId = step?.id

    private val _state = MutableStateFlow(initialState())
    val state: StateFlow<ProfileInfoUiState> = _state.asStateFlow()

    private val _completed = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val completed: SharedFlow<Unit> = _completed.asSharedFlow()

    init {
        setAmaniEventListener()
    }

    private fun initialState(): ProfileInfoUiState {
        val general = CachingHomeKYC.appConfig?.generalConfigs
        val captureStep = version?.steps?.firstOrNull()
        return ProfileInfoUiState(
            headerTitle = captureStep?.captureTitle.orFallback("Profile"),
            description = version?.steps?.firstOrNull()?.captureDescription.orEmpty(),
            nameTitle = version?.nameTitle.orEmpty(),
            nameHint = version?.nameHint.orEmpty(),
            surnameTitle = version?.surnameTitle.orEmpty(),
            surnameHint = version?.surnameHint.orEmpty(),
            birthDateTitle = version?.birthDateTitle.orEmpty(),
            birthDateHint = version?.birthDateHint.orEmpty(),
            continueText = general?.continueText.orFallback("Continue")
        )
    }

    fun onNameChange(value: String) = _state.update { it.copy(name = value, error = null) }
    fun onSurnameChange(value: String) = _state.update { it.copy(surname = value, error = null) }
    fun onBirthDateChange(value: String) = _state.update { it.copy(birthDate = value, error = null) }

    fun submit() {
        val s = _state.value
        if (!s.submitEnabled) return
        _state.update { it.copy(submitting = true, error = null) }

        // Same SDK calls v1 uses: set the full name + birth date, then upload. Navigation waits
        // for the step's socket verdict (see the listener below).
        Amani.sharedInstance().CustomerInfo().setInfo(
            fullName = "${s.name} ${s.surname}",
            birthDate = s.birthDate
        )
        Amani.sharedInstance().CustomerInfo().upload { success ->
            if (!success) {
                _state.update { it.copy(submitting = false, error = "Something went wrong. Please try again.") }
            }
        }
    }

    private fun setAmaniEventListener() {
        Amani.sharedInstance().AmaniEvent().setListener(object : AmaniEventCallBack {
            override fun onError(type: String?, error: ArrayList<AmaniError?>?) {}

            override fun profileStatus(profileStatus: ProfileStatus) {}

            override fun stepsResult(stepsResult: StepsResult?) {
                stepsResult?.result?.forEach { result ->
                    if (result.id != null && result.id == stepId) {
                        when (result.status) {
                            AppConstant.STATUS_APPROVED -> _completed.tryEmit(Unit)
                            AppConstant.STATUS_REJECTED ->
                                _state.update { it.copy(submitting = false, error = "Profile info step is rejected.") }
                        }
                    }
                }
            }
        })
    }

    private fun String?.orFallback(fallback: String): String =
        this?.takeIf { it.isNotBlank() } ?: fallback
}
