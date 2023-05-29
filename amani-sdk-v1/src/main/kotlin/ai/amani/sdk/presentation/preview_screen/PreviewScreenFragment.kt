package ai.amani.sdk.presentation.preview_screen

import ai.amani.amani_sdk.R
import ai.amani.amani_sdk.databinding.FragmentPreviewScreenBinding
import ai.amani.sdk.extentions.alertDialog
import ai.amani.sdk.extentions.hide
import ai.amani.sdk.model.ConfigModel
import ai.amani.sdk.presentation.home_kyc.ScreenRoutes
import ai.amani.sdk.utils.AmaniDocumentTypes
import ai.amani.sdk.extentions.setToolBarTitle
import ai.amani.sdk.extentions.show
import ai.amani.sdk.model.HomeKYCResultModel
import ai.amani.sdk.model.NFCScanScreenModel
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import kotlinx.coroutines.launch

/**
 * @Author: zekiamani
 * @Date: 5.09.2022
 */
class PreviewScreenFragment : Fragment() {

    private lateinit var binding: FragmentPreviewScreenBinding
    private val args: PreviewScreenFragmentArgs by navArgs()
    private val viewModel: PreviewScreenViewModel by activityViewModels { PreviewScreenViewModel.Factory }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_preview_screen, container, false)
        binding = FragmentPreviewScreenBinding.bind(view)
        binding.dataModel = args.previewScreenModel
        viewModel.setMaxAttempt(args.previewScreenModel.configModel.version)
        setToolBarTitle()
        clickEvents()
        liveEvent()
        return view
    }

    private fun clickEvents() {
        binding.tryAgainButton.setOnClickListener {
            findNavController().popBackStack()
        }

        binding.confirmButton.setOnClickListener {
            viewModel.navigateScreen(
                requireContext(),
                args.previewScreenModel.configModel.version!!,
                args.previewScreenModel.frontSide
            ) {
                when (it) {
                    ScreenRoutes.IDBackSideScreen -> {
                        val action =
                            PreviewScreenFragmentDirections.actionPreviewScreenFragmentToIDCaptureBackSideFrag(
                                ConfigModel(
                                    args.previewScreenModel.configModel.version,
                                    args.previewScreenModel.configModel.generalConfigs
                                )
                            )
                        findNavController().navigate(action)
                    }

                    ScreenRoutes.HomeKYCScreen -> {
                        findNavController().getBackStackEntry(R.id.homeKYCFragment).savedStateHandle[AmaniDocumentTypes.type] =
                            HomeKYCResultModel(
                                args.previewScreenModel.configModel.version!!.documentId,
                                args.previewScreenModel.configModel.version!!.type,
                                args.previewScreenModel.selfieType
                            )
                        findNavController().clearBackStack(R.id.homeKYCFragment)
                        findNavController().popBackStack(R.id.homeKYCFragment, false)
                    }

                    ScreenRoutes.NFCScanScreen -> {
                        val action =
                            PreviewScreenFragmentDirections.actionPreviewScreenFragmentToNFCScanFragment(
                                NFCScanScreenModel(
                                    ConfigModel(
                                        args.previewScreenModel.configModel.version,
                                        args.previewScreenModel.configModel.generalConfigs
                                    ),

                                    viewModel.mrzModel!!,
                                    nfcOnly = false
                                )
                            )
                        findNavController().navigate(action)
                    }
                    else -> {

                    }
                }
            }
        }
    }

    private fun setToolBarTitle() {
        args.previewScreenModel.frontSide?.let {
            if (it) {
                // Set title of first step
                setToolBarTitle(args.previewScreenModel.configModel.version!!.steps[0].confirmationTitle)
            } else {
                // Set title of second step
                setToolBarTitle(args.previewScreenModel.configModel.version!!.steps[1].confirmationTitle)
            }
        } ?: run {
            setToolBarTitle(args.previewScreenModel.configModel.version!!.steps[0].confirmationTitle)
        }
    }

    private fun liveEvent() {
        lifecycleScope.launch {
            // repeatOnLifecycle launches the block in a new coroutine every time the
            // lifecycle is in the STARTED state (or above) and cancels it when it's STOPPED.
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                // Trigger the flow and start listening for values.
                // Note that this happens when lifecycle is STARTED and stops
                // collecting when the lifecycle is STOPPED
                viewModel.uiState.collect {
                    when (it) {
                        is PreviewScreenState.Loading -> {
                            binding.progressLoaderCentered.show()
                        }

                        is PreviewScreenState.Loaded -> {
                            binding.progressLoaderCentered.hide()
                        }

                        is PreviewScreenState.Error -> {
                            binding.progressLoaderCentered.hide()
                            alertDialog(
                                args.previewScreenModel.configModel.generalConfigs!!.tryAgainText,
                                args.previewScreenModel.configModel.version!!.mrzReadErrorText,
                                args.previewScreenModel.configModel.generalConfigs!!.okText,
                                args.previewScreenModel.configModel.generalConfigs!!.appFontColor,
                                args.previewScreenModel.configModel.generalConfigs!!.appBackground,
                                onButtonClick = {
                                    // Getting MRZ is could be failed in some reasons cause of OCR
                                    // failures At this time re-directing user IDCapture Back Side
                                    // to take photo
                                    viewModel.resetUIState()
                                    findNavController().popBackStack()
                                }
                            )
                        }


                        is PreviewScreenState.OutOfMaxAttempt -> {
                            //When current attempt is out of max attempt returns HomeKYCFragment to
                            //upload ID without NFC

                            findNavController().getBackStackEntry(R.id.homeKYCFragment).savedStateHandle[AmaniDocumentTypes.type] =
                                HomeKYCResultModel(
                                    AmaniDocumentTypes.IDENTIFICATION, // Due to NFC failed, we are sending type
                                    // as ID to only upload ID, otherwise it will crash while trying
                                    // to upload ID and NFC together although NFC could not scanned
                                    args.previewScreenModel.configModel.version!!.type
                                )

                            findNavController().clearBackStack(R.id.homeKYCFragment)
                            findNavController().popBackStack(R.id.homeKYCFragment, false)

                            viewModel.resetUIState()
                        }
                    }
                }
            }
        }
    }
}