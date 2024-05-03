package ai.amani.sdk.presentation.select_document_type

import ai.amani.amani_sdk.R
import ai.amani.amani_sdk.databinding.FragmentSelectDocumentTypeBinding
import ai.amani.sdk.extentions.navigateSafely
import ai.amani.sdk.model.ConfigModel
import ai.amani.sdk.presentation.home_kyc.ScreenRoutes
import ai.amani.sdk.presentation.select_document_type.adapter.DocumentAdapter
import ai.amani.sdk.utils.ColorConstant
import ai.amani.sdk.extentions.setToolBarTitle
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import datamanager.model.config.ResGetConfig
import datamanager.model.config.Version
import timber.log.Timber

/**
 * @Author: zekiamani
 * @Date: 12.09.2022
 */
class SelectDocumentTypeFragment: Fragment(), DocumentAdapter.IDocumentListener {

    private lateinit var binding: FragmentSelectDocumentTypeBinding
    private val args: SelectDocumentTypeFragmentArgs by navArgs()
    private val viewModel: SelectDocumentTypeViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_select_document_type, container, false)
        binding = FragmentSelectDocumentTypeBinding.bind(view)
        setCustomUI(
            args.dataModel.versionList,
            args.dataModel.generalConfigs
        )
        return view
    }

    override fun onOnItemSelected(version: Version?) {
        viewModel.navigateScreen(
            version!!
        ) {
            when (it) {
                ScreenRoutes.IDFrontSideScreen -> {
                    Timber.d("ID Capture is selected by user")

                    val action =
                        SelectDocumentTypeFragmentDirections.actionSelectDocumentTypeFragmentToIDCaptureFrontSideFrag(
                            ConfigModel(
                                version = version,
                                generalConfigs =  args.dataModel.generalConfigs!!.generalConfigs,
                                featureConfig = args.dataModel.featureConfig
                            )
                        )
                    findNavController().navigateSafely(action)
                }

                else -> {
                    Timber.d("Un-known")
                }
            }
        }
    }

    private fun setCustomUI(
        versionList: List<Version?>?,
        appConfig: ResGetConfig?
    ) {
        if (appConfig == null || versionList.isNullOrEmpty()) return
        val color: String = if (appConfig.generalConfigs!!.appFontColor != null
        ) appConfig.generalConfigs?.appFontColor!! else ColorConstant.COLOR_BLACK

        val matchingStepConfig = appConfig.stepConfigs.find { it.id == args.dataModel.currentVersionID }

        setToolBarTitle(
            matchingStepConfig?.documentSelectionTitle,
            appConfig.generalConfigs!!.appFontColor
        )

        binding.text.setTextProperty(matchingStepConfig?.documentSelectionDescription, color)
        binding.parentLayout.setBackgroundColor(
            Color.parseColor(
                appConfig.generalConfigs?.appBackground
            )
        )

        binding.recyclerView.layoutManager = LinearLayoutManager(
            activity,
            RecyclerView.VERTICAL,
            false
        )
        binding.recyclerView.itemAnimator = DefaultItemAnimator()
        binding.recyclerView.isNestedScrollingEnabled = false
        binding.recyclerView.adapter = DocumentAdapter(versionList, this, args.dataModel.generalConfigs!!.generalConfigs)
    }

}