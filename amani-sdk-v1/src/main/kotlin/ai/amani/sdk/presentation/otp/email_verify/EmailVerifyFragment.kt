package ai.amani.sdk.presentation.otp.email_verify

import ai.amani.amani_sdk.R
import ai.amani.amani_sdk.databinding.FragmentEmailVerifyBinding
import ai.amani.sdk.extentions.customStroke
import ai.amani.sdk.extentions.customizeToolBar
import ai.amani.sdk.extentions.hide
import ai.amani.sdk.extentions.hideKeyboard
import ai.amani.sdk.extentions.navigateSafely
import ai.amani.sdk.extentions.setCustomBackground
import ai.amani.sdk.extentions.setKeyboardEventListener
import ai.amani.sdk.extentions.show
import ai.amani.sdk.extentions.showSnackbar
import ai.amani.sdk.model.KYCResult
import ai.amani.sdk.presentation.common.NavigationCommands
import ai.amani.sdk.utils.AppConstant
import android.app.Activity
import android.content.Intent
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
import kotlinx.coroutines.launch

/**
 * @Author: @zekiamani
 * @Date: 21.12.2023
 */
class EmailVerifyFragment: Fragment() {


    private val viewModel by viewModels<EmailVerifyViewModel>()
    private var _binding: FragmentEmailVerifyBinding? = null
    private val binding get() = _binding!!
    private val args by navArgs<EmailVerifyFragmentArgs>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        _binding = FragmentEmailVerifyBinding.inflate(inflater, container, false)
        return binding.root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.setArgs(args)

        setCustomUI()

        binding.emailInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            }

            override fun afterTextChanged(s: Editable?) {
                viewModel.enteredEmailInput(s.toString())
            }
        })

        binding.emailInput.setOnFocusChangeListener { v, hasFocus ->
            if (hasFocus) {
                binding.emailInput.customStroke(2, args.data.config.appFontColor,
                    args.data.config.buttonRadiusAndroid)
            } else binding.emailInput.customStroke(1, args.data.config.appFontColor,
                args.data.config.buttonRadiusAndroid)
        }

        binding.continueBtn.setOnClickListener {
            viewModel.onClickContinue()
        }

        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            object: OnBackPressedCallback(true){
                override fun handleOnBackPressed() {
                    finishActivity()
                }
            })

        lifecycleScope.launch {
            viewModel.navigateTo.collect{
                if (it is NavigationCommands.NavigateDirections) {
                    hideKeyboard()
                    findNavController().navigateSafely(it.direction)
                }
            }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.RESUMED) {
                viewModel.uiState.collect{
                    when(it) {
                        is EmailVerifyStates.Empty -> {
                            binding.progressBar.hide()
                            binding.emailInput.hideTextViewAlert()
                        }

                        is EmailVerifyStates.Success -> {
                            binding.progressBar.hide()

                        }

                        is EmailVerifyStates.InvalidEmail -> {
                            binding.emailInput.showTextViewAlert(it.message)
                            binding.progressBar.hide()
                        }

                        is EmailVerifyStates.Loading -> {
                            binding.progressBar.show()
                        }

                        is EmailVerifyStates.SnakeMessage -> {
                            showSnackbar(it.message)
                            binding.progressBar.hide()
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

    private fun EditText.showTextViewAlert(message : String?) {
        this.customStroke(2, "#FF0000",
            args.data.config.buttonRadiusAndroid)

        val alertColor = ContextCompat.getColor(requireContext(), R.color.error_red)
        this.setHintTextColor(alertColor)
        this.setTextColor(alertColor)
        val defaultMessage  = requireContext().resources.getString(R.string.invalid_phone_number)
        binding.alertTextOtp.text = message ?: defaultMessage
        binding.alertTextOtp.show()
    }

    private fun EditText.hideTextViewAlert() {

        this.customStroke(2, args.data.config.appFontColor,
            args.data.config.buttonRadiusAndroid)

        binding.alertTextOtp.hide()
        this.setHintTextColor(
            ContextCompat.getColor(requireContext(),
                R.color.light_gray)
        )

        this.setTextColor(
            ContextCompat.getColor(requireContext(),
                R.color.black_20)
        )
    }

    private fun finishActivity() {
        //Finishing activity with KYCResult as INCOMPLETE
        val intent = Intent()
        intent.putExtra(AppConstant.KYC_RESULT, KYCResult())
        requireActivity().setResult(Activity.RESULT_OK, intent)
        requireActivity().finish()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun setCustomUI() {
        customizeToolBar(
            args.data.config.topBarBackground,
            args.data.config.topBarFontColor,
            args.data.config.topBarFontColor,
            title= args.data.steps.first().mDocuments?.first()?.versions
                ?.first()?.steps?.first()?.captureTitle?: ""
        )

        binding.resetPasswordDesc.text = viewModel.version?.steps?.first()?.captureDescription
        binding.otpText.text = viewModel.version?.emailTitle
        binding.emailInput.hint = viewModel.version?.emailHint
        //TODO change the button text from version
        binding.continueBtn.text = args.data.config.continueText

        binding.continueBtn.setBackgroundDrawable(
            ResourcesCompat.getDrawable(resources, R.drawable.custom_btn, null),
            args.data.config.primaryButtonBackgroundColor,
            4, args.data.config.primaryButtonBackgroundColor,
            0f, null,
            true, args.data.config.buttonRadiusAndroid)

        binding.emailInput.customStroke(1, args.data.config.appFontColor,
            args.data.config.buttonRadiusAndroid)
    }

    override fun onPause() {
        super.onPause()
        hideKeyboard()
    }
}