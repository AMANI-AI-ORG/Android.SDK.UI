package ai.amani.sdk.presentation.otp.phone_verify

import ai.amani.amani_sdk.R
import ai.amani.amani_sdk.databinding.FragmentPhoneVerifyBinding
import ai.amani.sdk.extentions.customStroke
import ai.amani.sdk.extentions.customizeToolBar
import ai.amani.sdk.extentions.hide
import ai.amani.sdk.extentions.hideKeyboard
import ai.amani.sdk.extentions.navigateSafely
import ai.amani.sdk.extentions.runOnUiThread
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
class PhoneVerifyFragment: Fragment() {
    private val viewModel by viewModels<PhoneVerifyViewModel>()
    private var _binding: FragmentPhoneVerifyBinding? = null
    private val binding get() = _binding!!
    private val args by navArgs<PhoneVerifyFragmentArgs>()


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        _binding = FragmentPhoneVerifyBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.setArgs(args = args)

        setCustomUI()

        observeLiveEvent()

        listenViews()
    }

    private fun observeLiveEvent() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.RESUMED) {
                viewModel.uiState.collect{
                    when(it) {
                        is PhoneVerifyState.Empty -> {
                            binding.progressBar.hide()
                            binding.phoneInput.hideTextViewAlert()
                        }

                        is PhoneVerifyState.Loading -> {
                            binding.progressBar.show()
                        }

                        is PhoneVerifyState.Success -> {
                            binding.progressBar.hide()
                        }

                        is PhoneVerifyState.InvalidPhoneNumber -> {
                            binding.progressBar.hide()
                            binding.phoneInput.showTextViewAlert(it.message)
                        }

                        is PhoneVerifyState.SnakeMessage -> {
                            showSnackbar(it.message)
                            binding.progressBar.hide()
                        }
                    }
                }
            }
        }

        lifecycleScope.launch {
            viewModel.navigateTo.collect{
                if (it is NavigationCommands.NavigateDirections) {
                    findNavController().navigateSafely(it.direction)
                }
            }
        }

        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            object: OnBackPressedCallback(true){
                override fun handleOnBackPressed() {
                    finishActivity()
                }
            })
    }

    private fun listenViews() {
        binding.phoneInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            }

            override fun afterTextChanged(s: Editable?) {
                viewModel.enteredPhoneInput(binding.ccp.selectedCountryCode + s.toString())
            }
        })

        binding.phoneInput.setOnFocusChangeListener { v, hasFocus ->
            if (hasFocus) {
                binding.otpLayout.customStroke(2, args.data.config.appFontColor,
                    args.data.config.buttonRadiusAndroid)
            } else binding.otpLayout.customStroke(1, args.data.config.appFontColor,
                args.data.config.buttonRadiusAndroid)
        }

        binding.continueBtn.setOnClickListener {
            viewModel.enteredPhoneInput(binding.ccp.selectedCountryCode +
                    binding.phoneInput.text.toString())
            viewModel.onClickContinue()
        }

        setKeyboardEventListener {
            if (it) { // Value should be less than keyboard's height
                binding.bottomLogo.hide()
            } else {
                binding.bottomLogo.show()
            }
        }
    }

    private fun EditText.showTextViewAlert(alert: String?) {
        binding.otpLayout.customStroke(2, "#FF0000",
            args.data.config.buttonRadiusAndroid)

        val alertColor = ContextCompat.getColor(requireContext(), R.color.error_red)
        this.setHintTextColor(alertColor)
        this.setTextColor(alertColor)
        val alertDefault = requireContext().resources.getString(R.string.invalid_phone_number)
        binding.alertText.text = alert ?: alertDefault
        binding.alertText.show()
    }

    private fun EditText.hideTextViewAlert() {
        binding.otpLayout.customStroke(2, args.data.config.appFontColor,
            args.data.config.buttonRadiusAndroid)

        binding.alertText.hide()
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

    private fun setCustomUI() {
        runOnUiThread {
            customizeToolBar(
                args.data.config.topBarBackground,
                args.data.config.topBarFontColor,
                args.data.config.topBarFontColor,
                title= args.data.steps.first().mDocuments?.first()?.versions
                    ?.first()?.steps?.first()?.captureTitle?: ""
            )

            binding.continueBtn.setBackgroundDrawable(
                ResourcesCompat.getDrawable(resources, R.drawable.custom_btn, null),
                args.data.config.primaryButtonBackgroundColor,
                4, args.data.config.primaryButtonBackgroundColor,
                0f, null,
                true, args.data.config.buttonRadiusAndroid)

            binding.otpLayout.customStroke(1, args.data.config.appFontColor,
                args.data.config.buttonRadiusAndroid)

            binding.resetPasswordDesc.text = viewModel.version?.steps?.first()?.captureDescription
            binding.otpText.text = viewModel.version?.phoneTitle
            binding.phoneInput.hint = viewModel.version?.phoneHint
            binding.continueBtn.text = args.data.config.continueText

            binding.ccp.customStroke(1,"#D9D9D9",
            args.data.config.buttonRadiusAndroid)
            val customTypeface = ResourcesCompat.getFont(requireActivity(), R.font.rubik_400)
            binding.ccp.setTypeFace(customTypeface)
        }
    }

    override fun onPause() {
        super.onPause()
        hideKeyboard()
    }
}