package ai.amani.sdk.presentation.selfie

import ai.amani.amani_sdk.R
import ai.amani.amani_sdk.databinding.FragmentSelfieCaptureBinding
import ai.amani.sdk.Amani
import ai.amani.sdk.extentions.*
import ai.amani.sdk.interfaces.IFragmentCallBack
import ai.amani.sdk.model.PreviewScreenModel
import ai.amani.sdk.modules.document_capture.camera.BitmapUtils
import ai.amani.sdk.modules.selfie.pose_estimation.observable.OnFailurePoseEstimation
import ai.amani.sdk.modules.selfie.pose_estimation.observable.PoseEstimationObserver
import ai.amani.sdk.presentation.MainActivity
import ai.amani.voice_assistant.AmaniVoiceAssistant
import ai.amani.voice_assistant.callback.AmaniVAPlayerCallBack
import ai.amani.voice_assistant.model.AmaniVAVoiceKeys
import android.graphics.Bitmap
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

/**
 * @Author: zekiamani
 * @Date: 6.09.2022
 */
class SelfieCaptureFragment: Fragment() {

    private val args: SelfieCaptureFragmentArgs by navArgs()
    private val viewModel: SelfieCaptureViewModel by viewModels()
    private lateinit var binding: FragmentSelfieCaptureBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_selfie_capture, container, false)
        binding = FragmentSelfieCaptureBinding.bind(view)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        toolBar()

        binding.confirmButton.setOnClickListener{
            viewModel.confirmButtonClick()
        }

        viewModel.initialViewState(args.dataModel.version!!)

        binding.dataModel = args.dataModel

        observeUiState()

        AmaniVoiceAssistant.play(requireContext(), "VOICE_SE0", callBack = object : AmaniVAPlayerCallBack{
            override fun onFailure(exception: Exception) {

            }

            override fun onPlay() {
            }

            override fun onStop() {
            }
        })
    }

    /** Navigates the Manual Selfie Fragment*/
    private fun initManualSelfie() {
        val selfieFragment = Amani.sharedInstance().Selfie().start("XXX_SE_0", object : IFragmentCallBack{
            override fun cb(bitmap: Bitmap?, isDestroyed: Boolean?, file: File?) {
                if (file != null) {
                   val action = SelfieCaptureFragmentDirections.actionSelfieCaptureFragmentToPreviewScreenFragment(
                        PreviewScreenModel(
                            file.absolutePath,
                            args.dataModel,
                            null,
                            viewModel.currentSelfieType()
                        )
                    )

                    findNavController().navigateSafely(action)
                }
            }
        })

        replaceChildFragmentWithoutBackStack(R.id.child_of_selfie, selfieFragment)
    }

    /** Navigates the Auto Selfie Fragment*/
    private fun initAutoSelfie() {
        Amani.sharedInstance().AutoSelfieCapture().setCustomUI(
            ai.amani.R.color.white,
            20,
            ai.amani.R.color.white,
            true,
            100,
            10,
            "Close enough",
            "Face not found",
            "Hold stable",
            "Restart in 3 w234234",
            ai.amani.R.color.white,
            ai.amani.R.color.approve_green)

        val selfieFragment = Amani.sharedInstance().AutoSelfieCapture().start("XXX_SE_0",
            null, MainActivity.binding.fragmentContainerView,object: IFragmentCallBack{
                override fun cb(
                    bitmap: Bitmap?,
                    manualButtonActivated: Boolean?,
                    file: File?
                ) {
                    if (bitmap != null) {
                        val saved = BitmapUtils.saveBitmapAsFile(bitmap, "selfie-a",requireContext())
                        val action = SelfieCaptureFragmentDirections.actionSelfieCaptureFragmentToPreviewScreenFragment(
                            PreviewScreenModel(
                                saved!!.absolutePath,
                                args.dataModel,
                                null,
                                viewModel.currentSelfieType()
                            )
                        )

                        findNavController().navigateSafely(action)
                    }
                }
            })
        replaceChildFragmentWithoutBackStack(R.id.child_of_selfie, selfieFragment!!)
    }

    /** Navigates the Selfie Pose Estimation Fragment*/
    private fun initSelfiePoseEstimation(
        requestedOrderNumber: Int,
        videoRecord: Boolean?
    ) {
        val selfieFragment = Amani.sharedInstance().SelfiePoseEstimation()
            .Builder()
            .requestedPoseNumber(requestedOrderNumber)
            .ovalViewAnimationDurationMilSec(500)
            .videoRecord(videoRecord = videoRecord)
            .observe(observable)
            .userInterfaceColors(
                ai.amani.R.color.white,
                ai.amani.R.color.approve_green,
                ai.amani.R.color.error_red,
                ai.amani.R.color.color_black,
                ai.amani.R.color.color_black,
                ai.amani.R.color.color_black,
                ai.amani.R.color.white,
                ai.amani.R.color.white)
            .userInterfaceTexts(
                "Your face is not inside the area",
                "Your face is not straight",
                "Your face is too far from camera",
                "Please keep straight the phone",
                "Verification Failed",
                "Failed",
                "Try Again"
            )
            .build(requireContext())
        selfieFragment?.let {
            replaceChildFragmentWithoutBackStack(R.id.child_of_selfie, selfieFragment)
        }
    }

    private val observable: PoseEstimationObserver = object : PoseEstimationObserver {
        override fun onSuccess(bitmap: Bitmap?) {
            bitmap?.let {

                val file = BitmapUtils.saveBitmapAsFile(it,"selfie-pe",requireContext())

                val action = SelfieCaptureFragmentDirections.actionSelfieCaptureFragmentToPreviewScreenFragment(
                    PreviewScreenModel(
                        file!!.absolutePath,
                        args.dataModel,
                        null,
                        viewModel.currentSelfieType()
                    )
                )

                findNavController().navigateSafely(action)
            }?: run {
            }
        }

        override fun onFailure(reason: OnFailurePoseEstimation, currentAttempt: Int) {
        }

        override fun onError(error: Error) {
        }
    }

    private fun observeUiState() {
        CoroutineScope(Dispatchers.Main).launch {
            // repeatOnLifecycle launches the block in a new coroutine every time the
            // lifecycle is in the STARTED state (or above) and cancels it when it's STOPPED.
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                // Trigger the flow and start listening for values.
                // Note that this happens when lifecycle is STARTED and stops
                // collecting when the lifecycle is STOPPED
                viewModel.uiState.collect{
                    when(it) {

                        is SelfieCaptureUIState.FirstAnimation -> {
                            binding.selfieAnimationFirst.let { animation ->
                                animation.setAnimation(R.raw.animation_first_selfie_instruction)
                                animation.playAnimation()
                            }
                        }

                        is SelfieCaptureUIState.SecondAnimation -> {
                            binding.selfieAnimationFirst.let { animation ->
                                animation.slideLeft{
                                    binding.selfieAnimationSecond.let { animation2 ->
                                        animation2.setAnimation(R.raw.animation_second_selfie_instruction)
                                        animation2.playAnimation()
                                    }
                                }
                            }
                        }

                        is SelfieCaptureUIState.Navigate -> {

                            when(it.navigateTo) {
                                is SelfieType.Manual -> {
                                    binding.confirmButton.gone()
                                    binding.selfieAnimationFirst.slideLeft()
                                    initManualSelfie()
                                }

                                is SelfieType.Auto -> {
                                    binding.confirmButton.gone()
                                    binding.selfieAnimationFirst.slideLeft()
                                    initAutoSelfie()
                                }

                                is SelfieType.PoseEstimation -> {
                                    binding.confirmButton.gone()
                                    binding.selfieAnimationSecond.slideLeft{
                                        initSelfiePoseEstimation(
                                            requestedOrderNumber = args.dataModel.version!!.selfieType!!,
                                            videoRecord  = args.dataModel.version!!.videoRecord
                                        )
                                    }
                                }

                                else -> {}
                            }

                        }
                        else -> {
                            //Ignore else state
                        }
                    }
                }
            }
        }
    }

    private fun setToolBarTitle() {
        MainActivity.setToolBar(
            args.dataModel.version!!.title,
            args.dataModel.generalConfigs!!.appFontColor
        )
    }

    private fun toolBar() {
        setToolBarTitle(
            args.dataModel.version!!.steps[0].captureTitle,
            args.dataModel.generalConfigs!!.appFontColor
        )
    }

    override fun onResume() {
        super.onResume()
        setToolBarTitle()
    }

    override fun onPause() {
        super.onPause()
        AmaniVoiceAssistant.stop()
    }
}