package ai.amani.sdk.presentation.signature_screen

import ai.amani.amani_sdk.R
import ai.amani.amani_sdk.databinding.FragmentSignatureBinding
import ai.amani.sdk.Amani
import ai.amani.sdk.extentions.replaceChildFragmentWithoutBackStack
import ai.amani.sdk.extentions.setToolBarTitle
import ai.amani.sdk.model.HomeKYCResultModel
import ai.amani.sdk.modules.signature.interfaces.ISignatureStartCallBack
import ai.amani.sdk.utils.AmaniDocumentTypes
import ai.amani.sdk.utils.AppConstant.SIGNATURE_NUMBER
import android.graphics.Bitmap
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import timber.log.Timber

/**
 * @Author: zekiamani
 * @Date: 5.01.2023
 */
class SignatureFragment: Fragment() {

    private val args: SignatureFragmentArgs by navArgs()
    private lateinit var binding: FragmentSignatureBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_signature, container, false)
        binding = FragmentSignatureBinding.bind(view)
        binding.dataModel = args.configModel
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        toolBar()

        initDigitalSignatureFragment()

        clickEvents()
    }

    private fun initDigitalSignatureFragment() {
        val fragment =
            Amani.sharedInstance().Signature().start(requireContext(), SIGNATURE_NUMBER,
                object : ISignatureStartCallBack {
                    override fun cb(bitmap: Bitmap?, countOfSignature: Int) {
                        if (bitmap != null) {
                            if (countOfSignature == SIGNATURE_NUMBER){
                                //Required signature count is achieved
                                //Time to navigate HomeKYCScreen to upload relevant data
                                Timber.i("Required all digital signatures are taken from user")

                                //Navigating to home screen for upload
                                navigateHomeScreen()
                            }
                        }
                    }
                })

        replaceChildFragmentWithoutBackStack(R.id.child_of_digital_signature_screen, fragment)

    }

    private fun clickEvents() {

        binding.confirmButton.setOnClickListener{
            //Confirming current data for next Signature
            Amani.sharedInstance().Signature().confirm(requireContext())
        }

        binding.tryAgainButton.setOnClickListener {
            //Cleaning current signature for new signature
            Amani.sharedInstance().Signature().clean()
        }
    }

    private fun toolBar() {
        setToolBarTitle(
            args.configModel.version!!.steps[0].captureTitle,
            args.configModel.generalConfigs!!.appFontColor
        )
    }

    /** Navigating the HomeScreen when required [SIGNATURE_NUMBER] is reached.
     * HomeKYCScreen will be handle to upload taken data from SignatureModule due to [AmaniDocumentTypes]
     */
    private fun navigateHomeScreen() {
        requireActivity().runOnUiThread {
            //Navigating to HomeKYCFragment for calling upload the taken data
            findNavController().getBackStackEntry(R.id.homeKYCFragment).savedStateHandle[AmaniDocumentTypes.type] =
                HomeKYCResultModel(
                    args.configModel.version!!.documentId,
                    args.configModel.version!!.type
                )

            findNavController().clearBackStack(R.id.homeKYCFragment)
            findNavController().popBackStack(R.id.homeKYCFragment, false)
        }
    }
}