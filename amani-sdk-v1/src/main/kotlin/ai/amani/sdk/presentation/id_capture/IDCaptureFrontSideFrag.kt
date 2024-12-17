package ai.amani.sdk.presentation.id_capture

import ai.amani.amani_sdk.R
import ai.amani.amani_sdk.databinding.FragmentIdCaptureFrontBinding
import ai.amani.sdk.Amani
import ai.amani.sdk.extentions.*
import ai.amani.sdk.model.PreviewScreenModel
import ai.amani.sdk.modules.document_capture.camera.BitmapUtils
import ai.amani.sdk.presentation.MainActivity
import ai.amani.sdk.utils.AppConstant
import ai.amani.voice_assistant.AmaniVoiceAssistant
import ai.amani.voice_assistant.callback.AmaniVAPlayerCallBack
import ai.amani.voice_assistant.model.AmaniVAVoiceKeys
import android.animation.Animator
import android.graphics.Bitmap
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import timber.log.Timber
import java.io.File

/**
 * @Author: zekiamani
 * @Date: 5.09.2022
 */
class IDCaptureFrontSideFrag : Fragment() {

    private val args: IDCaptureFrontSideFragArgs by navArgs()
    private lateinit var binding: FragmentIdCaptureFrontBinding
    private var idCaptureFragmentFrontSide: Fragment? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_id_capture_front, container, false)
        binding = FragmentIdCaptureFrontBinding.bind(view)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        toolBar()

        playAnimation()

        listenAnimation()

        AmaniVoiceAssistant.play(
            context = requireContext(),
            key = AmaniVAVoiceKeys.VOICE_ID0,
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

    private fun playAnimation() {
        try {
            binding.idFrontAnimation.let {
                it.setAnimation(R.raw.xx_id_front)
                it.playAnimation()
                it.show()
            }
        } catch (e: Exception){
            Timber.e("ID Capture animation loading error ${e.message} ")
            initIDCaptureFragment()
        }
    }

    private fun listenAnimation() {
        binding.idFrontAnimation.addAnimatorListener(
            object : Animator.AnimatorListener{
                override fun onAnimationStart(animation: Animator) {
                    Timber.d("ID Capture animation is started")
                }

                override fun onAnimationEnd(animation: Animator) {
                    Timber.d("ID Capture animation is end up")
                    binding.idFrontAnimation.let {
                        it.removeAllAnimatorListeners()
                        it.gone()
                    }
                    initIDCaptureFragment()
                }

                override fun onAnimationCancel(animation: Animator) {
                    Timber.d("ID Capture animation is canceled")
                }

                override fun onAnimationRepeat(animation: Animator) {
                }

            }
        )
    }

    private fun initIDCaptureFragment() {

        if (!isResumed) {
            Timber.d("ID Capture cannot be loaded cause of fragment state. It is not on resume")
            return
        }

        // Setting IDCapture timeOut as default 30 sec
        Amani.sharedInstance().IDCapture().setManualCropTimeOut(AppConstant.ID_CAPTURE_TIME_OUT)

        Amani.sharedInstance().IDCapture().videoRecord(args.dataModel.version!!.videoRecord)

        Timber.d("VideoRecord ${args.dataModel.version!!.videoRecord}")

       idCaptureFragmentFrontSide = Amani.sharedInstance().IDCapture().start(
            requireActivity(),
            MainActivity.binding.fragmentContainerView,
            args.dataModel.version!!.type,
            true
        )
        { bitmap: Bitmap?, isManualButtonActivated: Boolean?, file: File? ->

            if (bitmap != null) {

                //We are removing the child fragment from the paren fragment for two main reason.
                //First for blocking possible memory leaks when child is still on back stack
                //Second for ui, when back pressed from preview view child fragment is opening before
                //the animation is end that we do not prefer
                removeChildFragment(idCaptureFragmentFrontSide)

                try {
                    val file: File? =
                        BitmapUtils.saveBitmapAsFile(bitmap, "frontSide", requireContext())
                    val action =
                        IDCaptureFrontSideFragDirections.actionIDCaptureFrontSideFragToPreviewScreenFragment(
                            PreviewScreenModel(
                                file!!.absolutePath,
                                args.dataModel,
                                true
                            )
                        )

                    findNavController().navigateSafely(action)

                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
        idCaptureFragmentFrontSide?.let {
            replaceChildFragmentWithoutBackStack(R.id.child_of_id_front, it)
        }
    }

    private fun toolBar() {
        setToolBarTitle(
            args.dataModel.version!!.steps[0].captureTitle,
            args.dataModel.generalConfigs!!.appFontColor
        )
    }

    override fun onStop() {
        super.onStop()
        AmaniVoiceAssistant.stop()
    }
}