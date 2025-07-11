package ai.amani.sdk.presentation.selfie

import ai.amani.amani_sdk.R
import ai.amani.amani_sdk.databinding.FragmentSelfieCaptureBinding
import ai.amani.sdk.Amani
import ai.amani.sdk.data.manager.VoiceAssistantSDKManager
import ai.amani.sdk.extentions.*
import ai.amani.sdk.interfaces.IFragmentCallBack
import ai.amani.sdk.model.PreviewScreenModel
import ai.amani.sdk.modules.selfie.pose_estimation.observable.OnFailurePoseEstimation
import ai.amani.sdk.modules.selfie.pose_estimation.observable.PoseEstimationObserver
import ai.amani.sdk.presentation.AmaniMainActivity
import ai.amani.sdk.utils.BitmapUtils
import ai.amani.voice_assistant.AmaniVoiceAssistant
import ai.amani.voice_assistant.callback.AmaniVAPlayerCallBack
import android.graphics.Bitmap
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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

        selfieFragment?.let {
            replaceChildFragmentWithoutBackStack(R.id.child_of_selfie, it)
        }?:run {
            showSnackbar("Configuration error, Selfie Capture could not launch")
            popBackStackSafely()
        }
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
            args.dataModel.version?.faceIsTooFarText?: "Close enough",
            args.dataModel.version?.faceNotInsideText?: "Face not found",
            args.dataModel.version?.holdStableText?: "Please hold stable",
            args.dataModel.version?.selfieAlertDescription?: "Failed",
            ai.amani.R.color.white,
            ai.amani.R.color.approve_green)

        if (AmaniMainActivity.binding == null) return

        val selfieFragment = Amani.sharedInstance().AutoSelfieCapture().start("XXX_SE_0",
            null, AmaniMainActivity.binding!!.fragmentContainerView,object: IFragmentCallBack{
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
        selfieFragment?.let {
            replaceChildFragmentWithoutBackStack(R.id.child_of_selfie, it)
        }?:run {
            showSnackbar("Configuration error, Auto Selfie Capture could not launch")
            findNavController().popBackStack()
        }
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
                faceNotInside = args.dataModel.version?.faceNotInsideText?: "Your face is not inside the area",
                faceNotStraight = args.dataModel.version?.faceNotStraightText?: "Your face is not straight",
                faceIsTooFar = args.dataModel.version?.faceIsTooFarText?: "Your face is too far from camera",
                holdPhoneVertically = args.dataModel.version?.holdStableText?: "Please keep straight the phone",
                alertTitle = args.dataModel.version?.selfieAlertTitle?: "Verification Failed",
                alertDescription = args.dataModel.version?.selfieAlertDescription?: "Failed",
                alertTryAgain = args.dataModel.version?.selfieAlertTryAgain?: "Try Again",
                turnLeft = args.dataModel.version?.turnLeftText?: "Turn left",
                turnRight = args.dataModel.version?.turnRightText?: "Turn right",
                turnUp = args.dataModel.version?.turnUpText?: "Turn up",
                turnDown = args.dataModel.version?.turnDownText?: "Turn down",
                faceStraight = args.dataModel.version?.keepStraightText?: "Look straight",
            )
            .build(requireContext())
        selfieFragment?.let {
            replaceChildFragmentWithoutBackStack(R.id.child_of_selfie, it)
        }?:run {
            showSnackbar("Configuration error, Pose Estimation could not launch")
            findNavController().popBackStack()
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
                findNavController().popBackStack()
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
            repeatOnLifecycle(Lifecycle.State.RESUMED) {
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
                                    if (!isAdded) return@collect
                                    binding.confirmButton.gone()
                                    binding.selfieAnimationFirst.slideLeft()
                                    initManualSelfie()
                                }

                                is SelfieType.Auto -> {
                                    if (!isAdded) return@collect
                                    binding.confirmButton.gone()
                                    binding.selfieAnimationFirst.slideLeft()
                                    initAutoSelfie()
                                }

                                is SelfieType.PoseEstimation -> {
                                    if (!isAdded) return@collect
                                    binding.confirmButton.gone()
                                    binding.selfieAnimationSecond.slideLeft{
                                        val videoRecord = args.dataModel.featureConfig.selfieCaptureVideoRecord?:
                                        args.dataModel.version?.videoRecord?: false

                                        initSelfiePoseEstimation(
                                            requestedOrderNumber = args.dataModel.version!!.selfieType!!,
                                            videoRecord  = videoRecord
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
        AmaniMainActivity.setToolBar(
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
        VoiceAssistantSDKManager.stop()
    }
}