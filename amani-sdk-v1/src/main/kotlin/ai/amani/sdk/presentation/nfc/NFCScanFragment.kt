package ai.amani.sdk.presentation.nfc

import ai.amani.amani_sdk.R
import ai.amani.amani_sdk.databinding.FragmentNfcScanBinding
import ai.amani.sdk.data.manager.VoiceAssistantSDKManager
import ai.amani.sdk.extentions.alertDialog
import ai.amani.sdk.extentions.hide
import ai.amani.sdk.extentions.setToolBarTitle
import ai.amani.sdk.extentions.show
import ai.amani.sdk.model.HomeKYCResultModel
import ai.amani.sdk.presentation.otp.profile_info.DatePickerHandler
import ai.amani.sdk.presentation.selfie.SelfieType
import ai.amani.sdk.utils.AmaniDocumentTypes
import ai.amani.sdk.utils.ColorConstant
import ai.amani.voice_assistant.AmaniVoiceAssistant
import ai.amani.voice_assistant.callback.AmaniVAPlayerCallBack
import ai.amani.voice_assistant.model.AmaniVAVoiceKeys
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.provider.Settings
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber


/**
 * @Author: zekiamani
 * @Date: 26.09.2022
 */
class NFCScanFragment : Fragment() {

    private lateinit var binding: FragmentNfcScanBinding
    private val args: NFCScanFragmentArgs by navArgs()
    private val viewModel: NFCSharedViewModel by activityViewModels{NFCSharedViewModel.Factory}
    private val nfcScanningModal by lazy { NFCScanningBottomDialog() }
    private var nfcDialogMessages: NFCScanningBottomDialog.NFCDialogMessages = NFCScanningBottomDialog.NFCDialogMessages()
    private lateinit var expiryDatePicker: DatePickerHandler
    private lateinit var birthDatePicker: DatePickerHandler

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_nfc_scan, container, false)
        binding = FragmentNfcScanBinding.bind(view)
        binding.dataModel = args.dataModel.configModel
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setCustomUI()
        observeLiveEvent()

        VoiceAssistantSDKManager.play(
            context = requireContext(),
            key = AmaniVAVoiceKeys.VOICE_NFC,
            callBack = object : AmaniVAPlayerCallBack {
                override fun onPlay() {

                }

                override fun onStop() {

                }

                override fun onFailure(exception: Exception) {

                }
            }
        )
    }

    override fun onResume() {
        super.onResume()
        viewModel.clearNFCState()
        viewModel.checkNFCState(
            requireContext(),
            disable = {
                Timber.i("NFC is disabled by user, redirecting message will be popped for enable NFC")
                alertDialog(
                    args.dataModel.configModel.version!!.enableNfcHeader,
                    args.dataModel.configModel.version!!.enableNfcDescription,
                    args.dataModel.configModel.generalConfigs!!.tryAgainText,
                    args.dataModel.configModel.generalConfigs!!.appFontColor,
                    args.dataModel.configModel.generalConfigs!!.appBackground,
                    onButtonClick = {
                        // Opening NFCSetting Screen for user to enabling NFC
                        startNfcSettingsActivity()
                    }
                )
            },
            notSupported = {
                Timber.e("NFC is not supported")
            }
        )
    }


    private fun setCustomUI() {
        val nfcTitle =
            args.dataModel.configModel.version!!.nfcTitle
        val nfcDescription1 =
            args.dataModel.configModel.version!!.nfcDescription1
        val nfcDescription2 =
            args.dataModel.configModel.version!!.nfcDescription2
        val nfcDescription3 =
            args.dataModel.configModel.version!!.nfcDescription3
        args.dataModel.configModel.version!!.nfcPleaseHold
        val appFontColor =
            args.dataModel.configModel.generalConfigs!!.appFontColor
        val appBackGroundColor: Int =
            Color.parseColor(args.dataModel.configModel.generalConfigs!!.appBackground)
        args.dataModel.configModel.version!!.nfcAnimationColor

        binding.nfcDesc1.setTextProperty(nfcDescription1, appFontColor)
        binding.nfcDesc2.setTextProperty(nfcDescription2, appFontColor)
        binding.nfcDesc3.setTextProperty(nfcDescription3, appFontColor)

        binding.parentLayout.setBackgroundColor(appBackGroundColor)

        nfcDialogMessages = NFCScanningBottomDialog.NFCDialogMessages(
            nfcFail = args.dataModel.configModel.version!!.nfcFailed,
            nfcScanningDescription = args.dataModel.configModel.version!!.nfcDialogDescription,
            nfcScanningTitle = args.dataModel.configModel.version!!.nfcDialogTitle
        )

        setToolBarTitle(
            nfcTitle,
            appFontColor
        )

        binding.birthDateInput.setText(args.dataModel.mrzModel.birthDate)
        binding.expiryDateInput.setText(args.dataModel.mrzModel.expireDate)
        binding.docNumberInput.setText(args.dataModel.mrzModel.docNumber)

        viewModel.setMRZ(
            args.dataModel.mrzModel
        )

        binding.docNumberInput.addTextChangedListener(object : TextWatcher{
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
            }

            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
            }

            override fun afterTextChanged(p0: Editable?) {
                viewModel.mrzData.docNumber = p0.toString()
            }
        })

        binding.continueBtn.setOnClickListener {
            viewModel.continueBtnClick()
        }

        binding.birthDateInput.setOnClickListener {
            birthDatePicker.showDatePickerDialog(dateFormat = "yy-MM-dd")
        }

        binding.expiryDateInput.setOnClickListener {
            expiryDatePicker.showDatePickerDialog(dateFormat = "yy-MM-dd")
        }

        binding.continueBtn.setBackgroundDrawable(
            ResourcesCompat.getDrawable(resources, R.drawable.custom_btn, null),
            args.dataModel.configModel.generalConfigs!!.primaryButtonBackgroundColor,
            4, args.dataModel.configModel.generalConfigs!!.primaryButtonBackgroundColor,
            0f, null,
            true, args.dataModel.configModel.generalConfigs!!.buttonRadiusAndroid)

        expiryDatePicker = DatePickerHandler(
            context = requireContext(),
            listener = {
                binding.expiryDateInput.setText(it.replace("-", ""))
                viewModel.mrzData.expireDate = it.replace("-", "")
            }
        )

        birthDatePicker = DatePickerHandler(
            context = requireContext(),
            listener = {
                binding.birthDateInput.setText(it.replace("-", ""))
                viewModel.mrzData.birthDate = it.replace("-", "")
            }
        )
    }

    private fun observeLiveEvent() {
        viewModel.get.observe(viewLifecycleOwner) {
            if (it != null) {
                CoroutineScope(Dispatchers.Main).launch {
                    requireActivity().supportFragmentManager.let {
                        val args = Bundle()
                        args.putParcelable(NFCScanningBottomDialog.ARG_KEY, nfcDialogMessages)
                        nfcScanningModal.arguments = args
                       if (!nfcScanningModal.isVisible) nfcScanningModal.show(it, NFCScanningBottomDialog.TAG)
                    }
                }

                viewModel.scanNFC(
                    it,
                    requireContext()
                )
            }
        }

        viewModel.nfcScanState.observe(viewLifecycleOwner) {
            when(it) {
                is NFCScanState.Success -> {
                    //Navigate HomeKYCFragment
                    Timber.i("NFC is scanned properly")

                    lifecycleScope.launch(Dispatchers.Main) {
                        nfcScanningModal.nfcScanningDoneAnimations()
                        delay(1000)

                        viewModel.clearNFCState()
                        findNavController().getBackStackEntry(R.id.homeKYCFragment)
                            .savedStateHandle[AmaniDocumentTypes.type] = HomeKYCResultModel(
                                AmaniDocumentTypes.NFC,
                                args.dataModel.configModel.version!!.type,
                                SelfieType.Unknown,
                                args.dataModel.nfcOnly // Represents us the process with ID or Nfc
                                // only as true/false
                            )

                        nfcScanningModal.dismiss()
                        findNavController().clearBackStack(R.id.homeKYCFragment)
                        findNavController().popBackStack(R.id.homeKYCFragment, false)
                    }

                }

                is NFCScanState.Failure ->{
                    //Show message to user to re-scan NFC
                    Timber.i("NFC scanning failed")
                    lifecycleScope.launch(Dispatchers.Main) {
                        nfcScanningModal.showError()
                        delay(1000)
                        nfcScanningModal.dismiss()
                        viewModel.setState(NFCScanState.ShowMRZCheck)
                    }
                }

                is NFCScanState.OutOfMaxAttempt ->{
                    //Navigate HomeKYCFragment
                    Timber.i("NFC scanning failed, OutOfMaxAttempt")
                    lifecycleScope.launch(Dispatchers.Main){

                        //Closing the NFC scanning dialog
                        nfcScanningModal.dismiss()

                        viewModel.clearNFCState()

                        if (args.dataModel.nfcOnly) {
                            findNavController().clearBackStack(R.id.homeKYCFragment)
                            findNavController().popBackStack()
                            return@launch
                        }

                        findNavController().getBackStackEntry(R.id.homeKYCFragment)
                            .savedStateHandle[AmaniDocumentTypes.type] =
                            HomeKYCResultModel(
                                AmaniDocumentTypes.IDENTIFICATION,
                                // Due to NFC failed, we are sending type as ID to only upload ID,
                                // otherwise it will crash while trying
                                // to upload ID and NFC together although NFC could not scanned
                                args.dataModel.configModel.version!!.type
                            )

                        findNavController().clearBackStack(R.id.homeKYCFragment)
                        findNavController().popBackStack(R.id.homeKYCFragment, false)
                    }

                }

                is NFCScanState.ReadyToScan -> {
                    lifecycleScope.launch(Dispatchers.Main) {
                        viewModel.setNfcEnable(true)
                        binding.infoLayout.show()
                        binding.mrzCheckLayout.hide()
                    }
                }

                is NFCScanState.ShowMRZCheck -> {
                    lifecycleScope.launch(Dispatchers.Main) {
                        viewModel.setNfcEnable(false)
                        viewModel.set(null)
                        binding.infoLayout.hide()
                        binding.mrzCheckLayout.show()
                    }
                }
            }
        }
    }

    override fun onPause() {
        super.onPause()
        viewModel.setNfcEnable(false)
        viewModel.set(null)
        VoiceAssistantSDKManager.stop()
    }

    private fun startNfcSettingsActivity() {
        startActivity(Intent(Settings.ACTION_NFC_SETTINGS))
    }
}