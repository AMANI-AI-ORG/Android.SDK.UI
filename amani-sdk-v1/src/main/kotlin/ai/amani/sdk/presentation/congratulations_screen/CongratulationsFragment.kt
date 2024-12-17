package ai.amani.sdk.presentation.congratulations_screen

import ai.amani.amani_sdk.R
import ai.amani.amani_sdk.databinding.FragmentCongratulationsBinding
import ai.amani.sdk.extentions.customizeToolBar
import ai.amani.sdk.model.KYCResult
import ai.amani.sdk.presentation.binding.setText
import ai.amani.sdk.utils.AppConstant
import ai.amani.sdk.utils.ProfileStatus
import ai.amani.voice_assistant.AmaniVoiceAssistant
import ai.amani.voice_assistant.callback.AmaniVAPlayerCallBack
import ai.amani.voice_assistant.model.AmaniVAVoiceKeys
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.navArgs

/**
 * @Author: zekiamani
 * @Date: 9.12.2022
 */
class CongratulationsFragment : Fragment() {

    private val args: CongratulationsFragmentArgs by navArgs()
    private lateinit var binding: FragmentCongratulationsBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_congratulations, container, false)
        binding = FragmentCongratulationsBinding.bind(view)
        binding.dataModel = args.configModel
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        AmaniVoiceAssistant.play(
            context = requireContext(),
            key = AmaniVAVoiceKeys.VOICE_SUCCESS,
            callBack = object : AmaniVAPlayerCallBack {
                override fun onPlay() {

                }

                override fun onStop() {

                }

                override fun onFailure(exception: Exception) {

                }
            }
        )

        setCustomUI()

        onClickEvents()

        handleBackPressed()
    }

    private fun onClickEvents() {
        binding.continueBtn.setOnClickListener {
            finishActivity()
        }
    }

    private fun setCustomUI() {
        customizeToolBar(
            backgroundColor = args.configModel.generalConfigs?.topBarBackground?: "",
            backImgColor = args.configModel.generalConfigs?.appFontColor?: "",
            titleTextColor = args.configModel.generalConfigs?.appFontColor?: "",
            title= args.configModel.generalConfigs?.successTitle?: ""
        )

        binding.congratulationTv.text = args.configModel.generalConfigs?.successHeaderText
    }

    private fun handleBackPressed() {
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, object : OnBackPressedCallback(true){
            override fun handleOnBackPressed() {
                finishActivity()
            }
        })
    }

    private fun finishActivity() {
        val returnIntent = Intent()
        returnIntent.putExtra(AppConstant.KYC_RESULT,
            KYCResult(
                profileStatus = ProfileStatus.APPROVED
            )
        )
        requireActivity().setResult(Activity.RESULT_OK, returnIntent)
        requireActivity().finish()
    }

    override fun onStop() {
        super.onStop()
        AmaniVoiceAssistant.stop()
    }
}