package ai.amani.sdk.presentation.otp.profile_info

import ai.amani.amani_sdk.R
import ai.amani.amani_sdk.databinding.FragmentProfileInfoBinding
import ai.amani.sdk.extentions.customStroke
import ai.amani.sdk.extentions.customizeToolBar
import ai.amani.sdk.extentions.hide
import ai.amani.sdk.extentions.hideKeyboard
import ai.amani.sdk.extentions.navigateSafely
import ai.amani.sdk.extentions.setKeyboardEventListener
import ai.amani.sdk.extentions.show
import ai.amani.sdk.extentions.showSnackbar
import ai.amani.sdk.model.KYCResult
import ai.amani.sdk.presentation.common.NavigationCommands
import ai.amani.sdk.utils.AppConstant
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import kotlinx.coroutines.launch
import net.yslibrary.android.keyboardvisibilityevent.KeyboardVisibilityEventListener

class ProfileInfoFragment : Fragment() {

    private var _binding: FragmentProfileInfoBinding? = null
    private val binding get() = _binding!!
    private val args by navArgs<ProfileInfoFragmentArgs>()
    private val viewModel by viewModels<ProfileInfoViewModel>()
    private lateinit var datePickerHandler: DatePickerHandler

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        _binding = FragmentProfileInfoBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.setArgs(args.data)

        setCustomUI()

        datePickerHandler = DatePickerHandler(
            context = requireContext(),
            listener = {
                binding.birthDateInput.setText(it)
                viewModel.data(birthDate = it)
                binding.birthDateInput.customStroke(2, args.data.config.appFontColor,
                    args.data.config.buttonRadiusAndroid)
            }
        )

        binding.nameInput.setOnFocusChangeListener { v, hasFocus ->
            if (hasFocus) {
                binding.nameInput.customStroke(2, args.data.config.appFontColor,
                    args.data.config.buttonRadiusAndroid)
            } else binding.nameInput.customStroke(1, args.data.config.appFontColor,
                args.data.config.buttonRadiusAndroid)
        }

        binding.surnameInput.setOnFocusChangeListener { v, hasFocus ->
            if (hasFocus) {
                binding.surnameInput.customStroke(2, args.data.config.appFontColor,
                    args.data.config.buttonRadiusAndroid)
            } else binding.surnameInput.customStroke(1, args.data.config.appFontColor,
                args.data.config.buttonRadiusAndroid)
        }

        binding.datePicker.setOnClickListener{
            binding.nameInput.clearFocus()
            binding.surnameInput.clearFocus()
            datePickerHandler.showDatePickerDialog()
        }

        binding.birthDateInput.setOnClickListener{
            datePickerHandler.showDatePickerDialog()
        }

        binding.continueBtn.setOnClickListener {
            hideKeyboard()

            viewModel.data(
                name = binding.nameInput.text.toString(),
                surname = binding.surnameInput.text.toString()
            )

            viewModel.buttonClick()
        }

        requireActivity().onBackPressedDispatcher
            .addCallback(viewLifecycleOwner, object : OnBackPressedCallback(true){
                override fun handleOnBackPressed() {
                    finishActivity()
                }
            })

        setKeyboardEventListener {
            if (it) { // Value should be less than keyboard's height
                binding.bottomLogo.hide()
            } else {
                binding.bottomLogo.show()
            }
        }

        observeLiveEvent()
    }

    private fun observeLiveEvent() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.RESUMED) {
                viewModel.uiState.collect{
                    when(it) {
                        is ProfileInfoState.NameIsEmpty -> {

                        }

                        is ProfileInfoState.SurnameIsEmpty -> {

                        }

                        is ProfileInfoState.BirthdateIsEmpty -> {

                        }

                        is ProfileInfoState.Error -> {
                            showSnackbar(it.message)
                            binding.progressBar.hide()
                        }

                        is ProfileInfoState.Loading -> {
                            binding.progressBar.show()

                        }

                        is ProfileInfoState.Empty -> {
                            binding.progressBar.hide()
                        }
                    }
                }
            }
        }

        lifecycleScope.launch {
            viewModel.navigateTo.collect{
                when(it) {
                    is NavigationCommands.NavigateDirections -> {
                        findNavController().navigateSafely(it.direction)
                    }
                    
                    is NavigationCommands.NavigateToHomeScreen -> {
                        findNavController().clearBackStack(R.id.homeKYCFragment)
                        findNavController().popBackStack(R.id.homeKYCFragment, false)
                    }
                }
            }
        }
    }

    private fun setCustomUI () {
        binding.nameTitle.text = viewModel.currentStep().mDocuments?.first()?.versions?.first()?.nameTitle?: ""
        binding.nameInput.hint = viewModel.currentStep().mDocuments?.first()?.versions?.first()?.nameHint?: ""
        binding.surnameTitle.text = viewModel.currentStep().mDocuments?.first()?.versions?.first()?.surnameTitle?: ""
        binding.surnameInput.hint = viewModel.currentStep().mDocuments?.first()?.versions?.first()?.surnameHint?: ""
        binding.birthDateTitle.text = viewModel.currentStep().mDocuments?.first()?.versions?.first()?.birthDateTitle?: ""
        binding.birthDateInput.hint = viewModel.currentStep().mDocuments?.first()?.versions?.first()?.birthDateHint?: ""
        binding.descriptionText.text =
            viewModel.currentStep().mDocuments?.first()?.versions?.first()?.steps?.first()?.captureDescription
        binding.continueBtn.text = args.data.config.continueText

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

        binding.nameInput.customStroke(
            stroke = 1,
            color =  args.data.config.appFontColor,
            cornerRadius =  args.data.config.buttonRadiusAndroid)
        binding.surnameInput.customStroke(
            stroke = 1,
            color =  args.data.config.appFontColor,
            cornerRadius = args.data.config.buttonRadiusAndroid)
        binding.birthDateInput.customStroke(
            stroke = 1,
            color = args.data.config.appFontColor,
            cornerRadius =  args.data.config.buttonRadiusAndroid)
    }

    private fun finishActivity() {
        //Finishing activity with KYCResult as INCOMPLETE
        val intent = Intent()
        intent.putExtra(AppConstant.KYC_RESULT, KYCResult())
        requireActivity().setResult(Activity.RESULT_OK, intent)
        requireActivity().finish()
    }

    override fun onPause() {
        super.onPause()
        hideKeyboard()
    }
}