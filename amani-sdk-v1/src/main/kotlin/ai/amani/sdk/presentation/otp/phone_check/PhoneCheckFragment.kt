package ai.amani.sdk.presentation.otp.phone_check

import ai.amani.amani_sdk.R
import ai.amani.amani_sdk.databinding.FragmentPhoneCheckBinding
import ai.amani.sdk.extentions.customStroke
import ai.amani.sdk.extentions.customizeToolBar
import ai.amani.sdk.extentions.hide
import ai.amani.sdk.extentions.hideKeyboard
import ai.amani.sdk.extentions.navigateSafely
import ai.amani.sdk.extentions.popBackStackSafely
import ai.amani.sdk.extentions.runOnUiThread
import ai.amani.sdk.extentions.setKeyboardEventListener
import ai.amani.sdk.extentions.show
import ai.amani.sdk.extentions.showSnackbar
import ai.amani.sdk.presentation.AmaniMainActivity
import ai.amani.sdk.presentation.common.NavigationCommands
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.activity.OnBackPressedCallback
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * @Author: @zekiamani
 * @Date: 21.12.2023
 */
class PhoneCheckFragment: Fragment() {

    private val viewModel by viewModels<PhoneCheckViewModel>()
    private var _binding: FragmentPhoneCheckBinding? = null
    private val binding get() = _binding!!

    @Volatile
    private var isBackPressEnabled = true

    private val args by navArgs<PhoneCheckFragmentArgs>()
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        _binding = FragmentPhoneCheckBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.setArgs(args)

        setCustomUI()

        observeLiveEvent()

        listenViews()
    }

    private fun observeLiveEvent() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.RESUMED) {
                viewModel.uiState.collect{
                    when(it) {
                        is PhoneCheckStates.Empty -> {
                            binding.progressBar.hide()
                            binding.otpInput.hideTextViewAlert()
                        }

                        is PhoneCheckStates.Success -> {
                            CoroutineScope(Dispatchers.Main).launch {
                                binding.progressBar.hide()
                                hideKeyboard()
                            }
                        }

                        is PhoneCheckStates.InvalidOTP -> {
                            binding.otpInput.showTextViewAlert()
                            binding.progressBar.hide()
                        }

                        is PhoneCheckStates.Loading -> {
                            binding.progressBar.show()
                        }

                        is PhoneCheckStates.SnakeMessage -> {
                            showSnackbar(it.message)
                        }

                        is PhoneCheckStates.BackPressAvailability -> {
                            //If back press is not available, blocking back press physical and phone
                            //button all together. If its available enable them
                            isBackPressEnabled = it.available
                            AmaniMainActivity.isBackButtonEnabled(isEnabled =  it.available)
                            binding.resendEmailText.isClickable = it.available
                            if (it.available) {
                                binding.resendEmailText.setTextColor(
                                    ContextCompat.getColor(requireContext(),R.color.black_20)
                                )
                            }
                        }
                    }
                }
            }
        }

        requireActivity().onBackPressedDispatcher
            .addCallback(viewLifecycleOwner, object : OnBackPressedCallback(true){
                override fun handleOnBackPressed() {
                    if (isBackPressEnabled) {
                        isBackPressEnabled = false
                        requireActivity().onBackPressedDispatcher.onBackPressed()
                        popBackStackSafely()
                    }
                }
            })

        binding.resendEmailText.setOnClickListener{
            popBackStackSafely()
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.navigateTo.collect{
                when(it) {
                    is NavigationCommands.NavigateToHomeScreen -> {
                        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.Main) {
                            findNavController().clearBackStack(R.id.homeKYCFragment)
                            findNavController().popBackStack(R.id.homeKYCFragment, false)
                        }
                    }

                    is NavigationCommands.NavigateDirections -> {
                        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.Main) {
                            findNavController().navigateSafely(it.direction)
                        }
                    }
                }
            }
        }
    }

    private fun listenViews() {
        binding.continueBtn.setOnClickListener {
            viewModel.onClickContinue()
        }

        binding.otpInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            }

            override fun afterTextChanged(s: Editable?) {
                viewModel.enteredOTPInput(s.toString())
            }
        })

        binding.otpInput.setOnFocusChangeListener { v, hasFocus ->
            if (hasFocus) {
                binding.otpInput.customStroke(2, args.data.config.appFontColor,
                    args.data.config.buttonRadiusAndroid)
            } else binding.otpInput.customStroke(1, args.data.config.appFontColor,
                args.data.config.buttonRadiusAndroid)
        }

        binding.countDownTimer
            .countDownTime(countDownTimeSeconds = 180)
            .startCountdown(countDownState = viewModel.timerState)

        setKeyboardEventListener {
            if (it) { // Value should be less than keyboard's height
                binding.bottomLogo.hide()
            } else {
                binding.bottomLogo.show()
            }
        }
    }

    private fun EditText.showTextViewAlert() {
        this.customStroke(2, "#FF0000",
            args.data.config.buttonRadiusAndroid)

        val alertColor = ContextCompat.getColor(requireContext(), R.color.error_red)
        this.setHintTextColor(alertColor)
        this.setTextColor(alertColor)
        binding.alertText.text =
            requireContext().resources.getString(R.string.invalid_otp_code)
        binding.alertText.show()
    }

    private fun EditText.hideTextViewAlert() {

        this.customStroke(2, args.data.config.appFontColor,
            args.data.config.buttonRadiusAndroid)

        binding.alertText.hide()
        this.setHintTextColor(
            ContextCompat.getColor(requireContext(),
                R.color.black_20)
        )

        this.setTextColor(
            ContextCompat.getColor(requireContext(),
                R.color.black_20)
        )
    }

    private fun setCustomUI() {
        runOnUiThread {
            customizeToolBar(
                args.data.config.topBarBackground,
                args.data.config.topBarFontColor,
                args.data.config.topBarFontColor,
                title= args.data.steps.first().mDocuments?.first()?.versions
                    ?.first()?.steps?.first()?.confirmationTitle?: ""
            )

            binding.resetPasswordDesc.text =
                viewModel.version?.steps?.first()?.confirmationDescription?: ""

            binding.otpText.text = viewModel.version?.otpTitle
            binding.resendEmailText.text = viewModel.version?.resendOTP
            binding.continueBtn.text = args.data.config.continueText

            binding.continueBtn.setBackgroundDrawable(
                ResourcesCompat.getDrawable(resources, R.drawable.custom_btn, null),
                args.data.config.primaryButtonBackgroundColor,
                4, args.data.config.primaryButtonBackgroundColor,
                0f, null,
                true, args.data.config.buttonRadiusAndroid)

            binding.otpInput.customStroke(1, args.data.config.appFontColor,
                args.data.config.buttonRadiusAndroid)
        }
    }

    override fun onPause() {
        super.onPause()
        hideKeyboard()
    }
}