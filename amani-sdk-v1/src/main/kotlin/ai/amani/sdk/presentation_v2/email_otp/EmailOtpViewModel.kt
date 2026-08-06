package ai.amani.sdk.presentation_v2.email_otp

import ai.amani.sdk.Amani
import ai.amani.sdk.extentions.Validator.Companion.isValidEmail
import ai.amani.sdk.interfaces.AmaniEventCallBack
import ai.amani.sdk.model.amani_events.error.AmaniError
import ai.amani.sdk.model.amani_events.error.AmaniErrorTypes
import ai.amani.sdk.model.amani_events.profile_status.ProfileStatus
import ai.amani.sdk.model.amani_events.steps_result.StepsResult
import ai.amani.sdk.presentation.home_kyc.CachingHomeKYC
import ai.amani.sdk.presentation_v2.phone_otp.OtpPhase
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

data class EmailOtpUiState(
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
            OtpPhase.EnterContact -> input.isValidEmail()
            OtpPhase.EnterCode -> input.isNotBlank()
        }
}

/**
 * V2 email-OTP view model — the mirror of [PhoneOtpViewModel][ai.amani.sdk.presentation_v2.phone_otp.PhoneOtpViewModel]
 * for the email flow: enter email → `setInfo`+`upload`+`sendEmailOTP` → enter code →
 * `submitEmailOTP`. Approved code emits [completed].
 */
class EmailOtpViewModel : ViewModel() {

    private val step = CachingHomeKYC.appConfig?.stepConfigs
        ?.firstOrNull { it.identifier == AppConstant.IDENTIFIER_EMAIL_OTP }
    private val version = step?.mDocuments?.firstOrNull()?.versions?.firstOrNull()
    private val general = CachingHomeKYC.appConfig?.generalConfigs

    private val _state = MutableStateFlow(contactState())
    val state: StateFlow<EmailOtpUiState> = _state.asStateFlow()

    private val _completed = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val completed: SharedFlow<Unit> = _completed.asSharedFlow()

    private var countdownJob: Job? = null

    init {
        setAmaniEventListener()
    }

    private fun contactState() = EmailOtpUiState(
        phase = OtpPhase.EnterContact,
        headerTitle = version?.steps?.firstOrNull()?.captureTitle.orFallback("Email"),
        description = version?.steps?.firstOrNull()?.captureDescription.orEmpty(),
        label = version?.emailTitle.orEmpty(),
        hint = version?.emailHint.orEmpty(),
        continueText = general?.continueText.orFallback("Continue"),
        resendText = version?.resendOTP.orEmpty()
    )

    private fun codeState() = EmailOtpUiState(
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
            OtpPhase.EnterContact -> submitEmail(s.input)
            OtpPhase.EnterCode -> submitOtp(s.input)
        }
    }

    private fun submitEmail(email: String) {
        if (!email.isValidEmail()) {
            _state.update { it.copy(error = version?.invalidEmailError ?: "Email address is invalid") }
            return
        }
        _state.update { it.copy(submitting = true, error = null) }
        Amani.sharedInstance().CustomerInfo().setInfo(email = email)
        Amani.sharedInstance().CustomerInfo().upload { uploaded ->
            if (!uploaded) {
                _state.update { it.copy(submitting = false, error = "Something went wrong. Please try again.") }
                return@upload
            }
            Amani.sharedInstance().CustomerInfo().sendEmailOTP { sent ->
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
        Amani.sharedInstance().CustomerInfo().submitEmailOTP(otp = code) { valid ->
            if (valid) _completed.tryEmit(Unit)
            else _state.update {
                it.copy(submitting = false, error = version?.invalidEmailError ?: "The code is invalid. Please try again.")
            }
        }
    }

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
