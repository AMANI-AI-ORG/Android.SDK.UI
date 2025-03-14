package ai.amani.sdk.presentation.otp.email_check

import ai.amani.amani_sdk.R
import ai.amani.amani_sdk.databinding.FragmentEmailCheckBinding
import ai.amani.sdk.extentions.customStroke
import ai.amani.sdk.extentions.customizeToolBar
import ai.amani.sdk.extentions.hide
import ai.amani.sdk.extentions.hideKeyboard
import ai.amani.sdk.extentions.navigateSafely
import ai.amani.sdk.extentions.popBackStackSafely
import ai.amani.sdk.extentions.setKeyboardEventListener
import ai.amani.sdk.extentions.show
import ai.amani.sdk.extentions.showSnackbar
import ai.amani.sdk.presentation.AmaniMainActivity
import ai.amani.sdk.presentation.common.NavigationCommands
import android.os.Bundle
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * @Author: @zekiamani
 * @Date: 21.12.2023
 */
class EmailCheckFragment: Fragment() {

    private var _binding: FragmentEmailCheckBinding? = null
    private val binding get() = _binding!!
    private val viewModel: EmailCheckViewModel by viewModels()
    @Volatile
    private var isBackPressEnabled = true
    private val args by navArgs<EmailCheckFragmentArgs>()
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        _binding = FragmentEmailCheckBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.setArgs(args = args)

        setCustomUI()

        binding.countDownTimer
            .countDownTime(countDownTimeSeconds = 120)
            .startCountdown(countDownState = viewModel.timerState)

        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.RESUMED) {
                viewModel.uiState.collect{
                    when(it) {
                        is EmailCheckStates.Empty -> {
                            binding.progressBar.hide()
                        }

                        is EmailCheckStates.Loading -> {
                            binding.progressBar.show()
                        }

                        is EmailCheckStates.Success -> {
                            binding.progressBar.hide()
                            binding.otpInput.hideTextViewAlert()
                        }

                        is EmailCheckStates.SnackMessage -> {
                            showSnackbar(it.message)
                            binding.progressBar.hide()
                        }

                        is EmailCheckStates.InvalidOTP -> {
                            binding.otpInput.showTextViewAlert(it.message)
                            binding.progressBar.hide()
                        }

                        is EmailCheckStates.BackPressAvailability -> {
                            //If back press is not available, blocking back press physical and phone
                            //button all together. If its available enable them
                            isBackPressEnabled = it.available
                            AmaniMainActivity.isBackButtonEnabled(it.available)
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

        binding.resendEmailText.setOnClickListener{
            popBackStackSafely()
        }

        binding.otpInput.setOnFocusChangeListener { v, hasFocus ->
            if (hasFocus) {
                binding.otpInput.customStroke(2, args.data.config.appFontColor,
                    args.data.config.buttonRadiusAndroid)
            } else binding.otpInput.customStroke(1, args.data.config.appFontColor,
                args.data.config.buttonRadiusAndroid)
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

        binding.verifyEmailBtn.setOnClickListener {
            viewModel.onClickVerifyButton(binding.otpInput.text.toString())
        }

        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.Main) {
            viewModel.navigateTo.collect{
                when (it) {
                    is NavigationCommands.NavigateDirections -> {

                        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.Main) {
                            try {
                                hideKeyboard()
                                findNavController().navigateSafely(it.direction)
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                    }

                    is NavigationCommands.NavigateToHomeScreen -> {
                        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.Main) {
                            try {
                                findNavController().clearBackStack(R.id.homeKYCFragment)
                                findNavController().popBackStack(R.id.homeKYCFragment, false)
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                    }
                }
            }
        }

        setKeyboardEventListener {
            if (it) { // Value should be less than keyboard's height
                binding.bottomLogo.hide()
            } else {
                binding.bottomLogo.show()
            }
        }
    }

    private fun EditText.showTextViewAlert(alertMessage: String) {
        this.customStroke(2, "#FF0000",
            args.data.config.buttonRadiusAndroid)

        val alertColor = ContextCompat.getColor(requireContext(), R.color.error_red)
        this.setHintTextColor(alertColor)
        this.setTextColor(alertColor)
        binding.alertTextOtp.text = alertMessage
        binding.alertTextOtp.show()
    }

    private fun EditText.hideTextViewAlert() {
        this.customStroke(2, args.data.config.appFontColor,
            args.data.config.buttonRadiusAndroid)

        binding.alertTextOtp.hide()

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
        customizeToolBar(
            args.data.config.topBarBackground,
            args.data.config.topBarFontColor,
            args.data.config.topBarFontColor,
            title= args.data.steps.first().mDocuments?.first()?.versions
                ?.first()?.steps?.first()?.confirmationTitle?: ""
        )

        binding.resendEmailText.text = viewModel.version?.resendOTP
        binding.otpText.text = viewModel.version?.otpTitle
        binding.resetPasswordDesc.text = viewModel.version?.steps?.first()?.confirmationDescription
        binding.verifyEmailBtn.text = args.data.config.continueText

        binding.verifyEmailBtn.setBackgroundDrawable(
            ResourcesCompat.getDrawable(resources, R.drawable.custom_btn, null),
            args.data.config.primaryButtonBackgroundColor,
            4, args.data.config.primaryButtonBackgroundColor,
            0f, null,
            true, args.data.config.buttonRadiusAndroid)
        AmaniMainActivity.isBackButtonEnabled(false)

        binding.otpInput.customStroke(1, args.data.config.appFontColor,
            args.data.config.buttonRadiusAndroid)
    }

    override fun onPause() {
        super.onPause()
        hideKeyboard()
    }
}