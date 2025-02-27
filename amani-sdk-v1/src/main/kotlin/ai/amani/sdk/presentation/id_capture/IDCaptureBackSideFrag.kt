package ai.amani.sdk.presentation.id_capture

import ai.amani.amani_sdk.R
import ai.amani.amani_sdk.databinding.FragmentIdCaptureBackBinding
import ai.amani.sdk.Amani
import ai.amani.sdk.data.manager.VoiceAssistantSDKManager
import ai.amani.sdk.extentions.gone
import ai.amani.sdk.extentions.navigateSafely
import ai.amani.sdk.extentions.popBackStackSafely
import ai.amani.sdk.extentions.removeChildFragment
import ai.amani.sdk.extentions.replaceChildFragmentWithoutBackStack
import ai.amani.sdk.extentions.setToolBarTitle
import ai.amani.sdk.extentions.show
import ai.amani.sdk.extentions.showSnackbar
import ai.amani.sdk.model.PreviewScreenModel
import ai.amani.sdk.presentation.MainActivity
import ai.amani.sdk.utils.BitmapUtils
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
class IDCaptureBackSideFrag : Fragment() {

    private val args: IDCaptureBackSideFragArgs by navArgs()
    private lateinit var binding: FragmentIdCaptureBackBinding
    private var idCaptureFragmentBackSide: Fragment? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_id_capture_back, container, false)
        binding = FragmentIdCaptureBackBinding.bind(view)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        toolBar()

        playAnimation()

        listenAnimation()

        VoiceAssistantSDKManager.play(
            context = requireContext(),
            key = AmaniVAVoiceKeys.VOICE_ID1,
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
            binding.idBackAnimation.let {
                it.setAnimation(R.raw.xx_id_back)
                it.playAnimation()
                it.show()
            }
        } catch (e: Exception) {
            Timber.e("ID Capture animation loading error ${e.message} ")
            initIDCaptureFragment()
        }
    }

    private fun listenAnimation() {
        binding.idBackAnimation.addAnimatorListener(
            object : Animator.AnimatorListener{
                override fun onAnimationStart(animation: Animator) {
                }

                override fun onAnimationEnd(animation: Animator) {
                    binding.idBackAnimation.let {
                        it.removeAllAnimatorListeners()
                        it.gone()
                    }
                    initIDCaptureFragment()
                }

                override fun onAnimationCancel(animation: Animator) {
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

        Amani.sharedInstance().IDCapture().videoRecord(args.dataModel.featureConfig.idCaptureVideoRecord)

        if (args.dataModel.version?.type.equals("TUR_ID_1")) {
            Amani.sharedInstance().IDCapture().hologramDetection(
                args.dataModel.featureConfig.idCaptureHologramDetection
            )
        }

        if (MainActivity.binding == null) return

        idCaptureFragmentBackSide = Amani.sharedInstance().IDCapture().start(
            requireActivity(),
            MainActivity.binding!!.fragmentContainerView, args.dataModel.version!!.type, false
        )
        { bitmap: Bitmap?, isManualButtonActivated: Boolean?, file: File? ->

            if (bitmap != null) {

                //We are removing the child fragment from the paren fragment for two main reason.
                //First for blocking possible memory leaks when child is still on back stack
                //Second for ui, when back pressed from preview view child fragment is opening before
                //the animation is end that we do not prefer
                removeChildFragment(idCaptureFragmentBackSide)

                try {
                    val file: File? =
                        BitmapUtils.saveBitmapAsFile(bitmap, "backSide", requireContext())
                    val action =
                        IDCaptureBackSideFragDirections.actionIDCaptureBackSideFragToPreviewScreenFragment(
                            PreviewScreenModel(
                                file!!.absolutePath,
                                args.dataModel,
                                false
                            )
                        )

                    findNavController().navigateSafely(action)

                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        idCaptureFragmentBackSide?.let {
            replaceChildFragmentWithoutBackStack(R.id.child_of_id_back, it)
        }?:run {
            showSnackbar("Configuration error, ID capture could not launch")
            popBackStackSafely()        }
    }

    private fun toolBar() {
        setToolBarTitle(
            args.dataModel.version!!.steps[1].captureTitle,
            args.dataModel.generalConfigs!!.appFontColor
        )
    }

    override fun onStop() {
        super.onStop()
        VoiceAssistantSDKManager.stop()
    }
}