package ai.amani.sdk.presentation.nfc

import ai.amani.amani_sdk.R
import ai.amani.amani_sdk.databinding.FragmentNfcScanBinding
import ai.amani.sdk.extentions.*
import ai.amani.sdk.model.HomeKYCResultModel
import ai.amani.sdk.presentation.selfie.SelfieType
import ai.amani.sdk.utils.AmaniDocumentTypes
import ai.amani.sdk.utils.ColorConstant
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.constraintlayout.widget.Constraints.LayoutParams
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
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
    }

    override fun onResume() {
        super.onResume()
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
            if (args.dataModel.configModel.version!!.nfcTitle != null)
                args.dataModel.configModel.version!!.nfcTitle else resources.getString(
                R.string.nfc_title
            )
        val nfcDescription1 =
            if (args.dataModel.configModel.version!!.nfcDescription1 != null)
                args.dataModel.configModel.version!!.nfcDescription1 else resources.getString(
                R.string.nfc_hold_your_card
            )
        val nfcDescription2 =
            if (args.dataModel.configModel.version!!.nfcDescription2 != null)
                args.dataModel.configModel.version!!.nfcDescription2 else resources.getString(
                R.string.nfc_keep_still
            )
        val nfcDescription3 =
            if (args.dataModel.configModel.version!!.nfcDescription3 != null)
                args.dataModel.configModel.version!!.nfcDescription3 else resources.getString(
                R.string.nfc_reading_completed
            )
        val nfcPleaseHold =
            if (args.dataModel.configModel.version!!.nfcPleaseHold != null)
                args.dataModel.configModel.version!!.nfcPleaseHold else resources.getString(
                R.string.nfc_hold_your_card
            )
        val appFontColor =
            if (args.dataModel.configModel.generalConfigs!!.appFontColor != null)
                args.dataModel.configModel.generalConfigs!!.appFontColor
            else ColorConstant.COLOR_BLACK
        val appBackGroundColor: Int =
            if (args.dataModel.configModel.generalConfigs!!.appBackground != null)
                Color.parseColor(args.dataModel.configModel.generalConfigs!!.appBackground)
            else Color.BLACK
        val nfcAnimationColor =
            if (args.dataModel.configModel.version!!.nfcAnimationColor != null)
                args.dataModel.configModel.version!!.nfcAnimationColor else appFontColor

        binding.textNfc.setTextProperty(nfcPleaseHold, appFontColor)
        binding.nfcDesc1.setTextProperty(nfcDescription1, appFontColor)
        binding.nfcDesc2.setTextProperty(nfcDescription2, appFontColor)
        binding.nfcDesc3.setTextProperty(nfcDescription3, appFontColor)

        binding.parentLayout.setBackgroundColor(appBackGroundColor)
        binding.animationView.setBackgroundColor(appBackGroundColor)
        binding.animationDone.setBackgroundColor(appBackGroundColor)

        try {
            binding.animationView.setColor(nfcAnimationColor)

            binding.animationDone.setColor(nfcAnimationColor)

        } catch (e: Exception) {
            Timber.e("NFCFragment setUI exception$e")
        }

        setToolBarTitle(
            nfcTitle,
            appFontColor
        )
    }

    private fun observeLiveEvent() {
        viewModel.get.observe(viewLifecycleOwner) {
            if (it != null) {
                CoroutineScope(Dispatchers.Main).launch {
                    binding.animationView.makeItCenter()
                }

                binding.animationView.playAnimation()
                showTexts(false)
                viewModel.scanNFC(
                    it,
                    requireContext(),
                    args.dataModel.mrzModel.birthDate,
                    args.dataModel.mrzModel.expireDate,
                    args.dataModel.mrzModel.expirationDate
                )
            }
        }

        viewModel.nfcScanState.observe(viewLifecycleOwner) {
            when(it) {
                is NFCScanState.Success -> {
                    //Navigate HomeKYCFragment
                    Timber.i("NFC is scanned properly")

                    CoroutineScope(Dispatchers.Main).launch {
                        binding.animationView.hide()
                        binding.animationDone.show()
                        binding.animationDone.playAnimation()
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

                        findNavController().clearBackStack(R.id.homeKYCFragment)
                        findNavController().popBackStack(R.id.homeKYCFragment, false)
                    }

                }

                is NFCScanState.Failure ->{
                    //Show message to user to re-scan NFC
                    Timber.i("NFC scanning failed")
                    CoroutineScope(Dispatchers.Main).launch {
                        binding.animationView.putTopOfIt(binding.linearLayout)
                        showTexts(true)
                        binding.animationView.pauseAnimation()
                    }
                }

                is NFCScanState.OutOfMaxAttempt ->{
                    //Navigate HomeKYCFragment
                    Timber.i("NFC scanning failed, OutOfMaxAttempt")
                    CoroutineScope(Dispatchers.Main).launch {
                        viewModel.clearNFCState()
                        binding.animationView.hide()

                        binding.linearLayout.makeItCenter()

                        binding.textNfc.text =
                            args.dataModel.configModel.version!!.nfcFailedScreenText1
                        binding.textNfc.show()
                        delay(1000)
                        binding.nfcDesc1.text =
                            args.dataModel.configModel.version!!.nfcFailedScreenText2
                        binding.nfcDesc1.show()
                        delay(1000)
                        binding.nfcDesc2.text =
                            args.dataModel.configModel.version!!.nfcFailedScreenText3
                        binding.nfcDesc2.show()
                        delay(1000)

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

                else -> {
                    //Ignore when state Empty.
                }
            }
        }
    }

    private fun View.makeItCenter() {
        val constraintSet = ConstraintSet()
        constraintSet.clone(binding.parentLayout)
        constraintSet.centerHorizontally(this.id, ConstraintSet.PARENT_ID)
        constraintSet.centerVertically(this.id, ConstraintSet.PARENT_ID)
        constraintSet.applyTo(binding.parentLayout)
    }

    private fun View.putTopOfIt(view: View){
        val constraintSet = ConstraintSet()
        constraintSet.clone(binding.parentLayout)
        constraintSet.clear(view.id)
        constraintSet.connect(this.id, ConstraintSet.BOTTOM, view.id, ConstraintSet.TOP)
        constraintSet.applyTo(binding.parentLayout)
    }
    override fun onPause() {
        super.onPause()
        viewModel.setNfcEnable(false)
    }

    private fun startNfcSettingsActivity() {
        startActivity(Intent(Settings.ACTION_NFC_SETTINGS))
    }

    private fun showTexts(show: Boolean) {
        binding.textNfc.show(show)
        binding.nfcDesc1.show(show)
        binding.nfcDesc2.show(show)
        binding.nfcDesc3.show(show)
    }
}