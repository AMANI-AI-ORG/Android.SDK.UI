package ai.amani.sdk.presentation_v2.phone_otp

import ai.amani.sdk.Amani
import ai.amani.sdk.extentions.Validator.Companion.isValidPhone
import ai.amani.sdk.interfaces.AmaniEventCallBack
import ai.amani.sdk.model.amani_events.error.AmaniError
import ai.amani.sdk.model.amani_events.error.AmaniErrorTypes
import ai.amani.sdk.model.amani_events.profile_status.ProfileStatus
import ai.amani.sdk.model.amani_events.steps_result.StepsResult
import ai.amani.sdk.presentation.home_kyc.CachingHomeKYC
import ai.amani.sdk.utils.AppConstant
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Which sub-screen of the phone_otp step is showing: enter the phone, or enter the code. */
enum class OtpPhase { EnterContact, EnterCode }

data class PhoneOtpUiState(
    val phase: OtpPhase,
    val headerTitle: String,
    val description: String,
    val label: String,
    val hint: String,
    val continueText: String,
    val resendText: String,
    val input: String = "",
    val error: String? = null,
    val submitting: Boolean = false,
    val resendEnabled: Boolean = false,
    val secondsRemaining: Int = 0
) {
    val continueEnabled: Boolean
        get() = !submitting && when (phase) {
            OtpPhase.EnterContact -> input.isValidPhone()
            OtpPhase.EnterCode -> input.isNotBlank()
        }
}

/**
 * V2 phone-OTP view model — the Compose port of v1's PhoneVerify + PhoneCheck as one two-phase
 * flow: enter phone → `setInfo`+`upload`+`sendPhoneOTP` → enter code → `submitPhoneOTP`.
 * Approved code emits [completed]. Sets its own AmaniEvent listener for CUSTOMER errors (v1);
 * the host re-attaches Home's listener when the pre-KYC chain returns.
 */
class PhoneOtpViewModel : ViewModel() {

    private val step = CachingHomeKYC.appConfig?.stepConfigs
        ?.firstOrNull { it.identifier == AppConstant.IDENTIFIER_PHONE_OTP }
    private val version = step?.mDocuments?.firstOrNull()?.versions?.firstOrNull()
    private val general = CachingHomeKYC.appConfig?.generalConfigs

    private val _state = MutableStateFlow(contactState())
    val state: StateFlow<PhoneOtpUiState> = _state.asStateFlow()

    private val _completed = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val completed: SharedFlow<Unit> = _completed.asSharedFlow()

    private var countdownJob: Job? = null

    init {
        setAmaniEventListener()
    }

    private fun contactState() = PhoneOtpUiState(
        phase = OtpPhase.EnterContact,
        headerTitle = version?.steps?.firstOrNull()?.captureTitle.orFallback("Phone"),
        description = version?.steps?.firstOrNull()?.captureDescription.orEmpty(),
        label = version?.phoneTitle.orEmpty(),
        hint = version?.phoneHint.orEmpty(),
        continueText = general?.continueText.orFallback("Continue"),
        resendText = version?.resendOTP.orEmpty()
    )

    private fun codeState() = PhoneOtpUiState(
        phase = OtpPhase.EnterCode,
        headerTitle = version?.steps?.firstOrNull()?.confirmationTitle.orFallback("Verify"),
        description = version?.steps?.firstOrNull()?.confirmationDescription.orEmpty(),
        label = version?.otpTitle.orEmpty(),
        hint = "",
        continueText = general?.continueText.orFallback("Continue"),
        resendText = version?.resendOTP.orFallback("Resend code")
    )

    fun onInputChange(value: String) = _state.update { it.copy(input = value, error = null) }

    fun onContinue() {
        val s = _state.value
        if (!s.continueEnabled) return
        when (s.phase) {
            OtpPhase.EnterContact -> submitPhone(s.input)
            OtpPhase.EnterCode -> submitOtp(s.input)
        }
    }

    private fun submitPhone(phone: String) {
        if (!phone.isValidPhone()) {
            _state.update { it.copy(error = version?.invalidPhoneNumberError ?: "Phone number is invalid") }
            return
        }
        _state.update { it.copy(submitting = true, error = null) }
        Amani.sharedInstance().CustomerInfo().setInfo(phoneNumber = phone)
        Amani.sharedInstance().CustomerInfo().upload { uploaded ->
            if (!uploaded) {
                _state.update { it.copy(submitting = false, error = "Something went wrong. Please try again.") }
                return@upload
            }
            Amani.sharedInstance().CustomerInfo().sendPhoneOTP { sent ->
                if (sent) {
                    _state.value = codeState()
                    startCountdown()
                } else {
                    _state.update { it.copy(submitting = false, error = "Something went wrong. Please try again.") }
                }
            }
        }
    }

    private fun submitOtp(code: String) {
        _state.update { it.copy(submitting = true, error = null) }
        Amani.sharedInstance().CustomerInfo().submitPhoneOTP(otp = code) { valid ->
            if (valid) _completed.tryEmit(Unit)
            else _state.update { it.copy(submitting = false, error = "The code is invalid. Please try again.") }
        }
    }

    /** Back to the phone screen to re-send (v1 PhoneCheck resend → PhoneVerify). */
    fun backToContact() {
        countdownJob?.cancel()
        _state.value = contactState()
    }

    private fun startCountdown(seconds: Int = 180) {
        countdownJob?.cancel()
        countdownJob = viewModelScope.launch {
            for (remaining in seconds downTo 1) {
                _state.update { it.copy(resendEnabled = false, secondsRemaining = remaining) }
                delay(1000)
            }
            _state.update { it.copy(resendEnabled = true, secondsRemaining = 0) }
        }
    }

    private fun setAmaniEventListener() {
        Amani.sharedInstance().AmaniEvent().setListener(object : AmaniEventCallBack {
            override fun onError(type: String?, error: ArrayList<AmaniError?>?) {
                if (type == AmaniErrorTypes.CUSTOMER.name) {
                    _state.update {
                        it.copy(submitting = false, error = error?.firstOrNull()?.errorMessage?.toString())
                    }
                }
            }

            override fun profileStatus(profileStatus: ProfileStatus) {}
            override fun stepsResult(stepsResult: StepsResult?) {}
        })
    }

    private fun String?.orFallback(fallback: String): String =
        this?.takeIf { it.isNotBlank() } ?: fallback
}
