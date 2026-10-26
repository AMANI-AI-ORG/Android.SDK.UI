package ai.amani.sdk.presentation.common.document_picker

import ai.amani.amani_sdk.databinding.DialogDocumentSourceBinding
import ai.amani.sdk.extentions.parcelable
import ai.amani.sdk.model.DocumentSource
import ai.amani.sdk.model.DocumentSourceDialogConfig
import ai.amani.sdk.presentation.home_kyc.ScreenRoutes
import ai.amani.sdk.utils.ColorConstant
import android.content.res.Resources
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.InsetDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.graphics.ColorUtils
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import datamanager.model.config.GeneralConfigs
import datamanager.model.config.Version
import timber.log.Timber

/**
 * Source dialog of a document whose `documentSource` is `optional`: the user picks the gallery,
 * a PDF or the camera, and the host takes the route of that choice.
 *
 * The options are stacked SDK buttons: the PDF sits in the middle as the primary button and its
 * label carries the recommendation, while the gallery and the camera are secondary buttons — a
 * PDF is the original document, so it is the upload the flow steers the user to. Every color and
 * the corner radius come from the remote config.
 *
 * The choice is delivered through the fragment result API, so it reaches a host recreated while
 * the dialog was open. Leaving the dialog without a choice keeps the user where they were.
 */
class DocumentSourceDialog : DialogFragment() {

    private var binding: DialogDocumentSourceBinding? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DialogDocumentSourceBinding.inflate(inflater, container, false)
        return binding!!.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val config = arguments?.parcelable<DocumentSourceDialogConfig>(ARG_KEY) ?: run {
            Timber.e("Document source dialog is opened without a config")
            dismissSafely()
            return
        }
        applyConfig(config)
    }

    override fun onStart() {
        super.onStart()
        // A card with the screen margins around it, the scrim stays the platform's.
        dialog?.window?.setBackgroundDrawable(
            InsetDrawable(ColorDrawable(Color.TRANSPARENT), WINDOW_INSET_DP.dp)
        )
        dialog?.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }

    private fun applyConfig(config: DocumentSourceDialogConfig) {
        val b = binding ?: return
        val radius = config.buttonRadius.toFloat()
        val fontColor = Color.parseColor(config.fontColor)

        b.rootContainer.background = roundedDrawable(
            fillColor = Color.parseColor(config.backgroundColor),
            radius = radius
        )

        b.tvTitle.text = config.texts.title
        b.tvTitle.setTextColor(fontColor)
        b.tvDescription.text = config.texts.description
        b.tvDescription.setTextColor(fontColor)

        // The SDK's button styling, shared with the terms dialog: the PDF is the primary
        // button, the gallery and the camera the secondary ones.
        b.btnGallery.setConfigStyle(
            text = config.texts.galleryText,
            textColor = config.secondaryButtonTextColor,
            backgroundColor = config.secondaryButtonBackgroundColor,
            borderColor = config.secondaryButtonBorderColor,
            radius = config.buttonRadius
        )
        b.btnPdf.setConfigStyle(
            text = config.texts.pdfText,
            textColor = config.primaryButtonTextColor,
            backgroundColor = config.primaryButtonBackgroundColor,
            borderColor = config.primaryButtonBorderColor,
            radius = config.buttonRadius
        )
        b.btnCamera.setConfigStyle(
            text = config.texts.cameraText,
            textColor = config.secondaryButtonTextColor,
            backgroundColor = config.secondaryButtonBackgroundColor,
            borderColor = config.secondaryButtonBorderColor,
            radius = config.buttonRadius
        )

        b.btnGallery.setOnClickListener { deliver(DocumentSource.Gallery, config) }
        b.btnPdf.setOnClickListener { deliver(DocumentSource.PdfFile, config) }
        b.btnCamera.setOnClickListener { deliver(DocumentSource.Camera, config) }
    }

    private fun deliver(source: DocumentSource, config: DocumentSourceDialogConfig) {
        Timber.i("Document source $source is chosen for ${config.versionType}")
        parentFragmentManager.setFragmentResult(
            REQUEST_KEY,
            bundleOf(RESULT_SOURCE to source.name, RESULT_VERSION_TYPE to config.versionType)
        )
        dismissSafely()
    }

    private fun roundedDrawable(fillColor: Int, radius: Float) = GradientDrawable().apply {
        cornerRadius = radius
        setColor(fillColor)
    }

    private fun dismissSafely() {
        try {
            if (isAdded && !isStateSaved && activity?.isFinishing == false
                && activity?.isDestroyed == false
            ) {
                dismissAllowingStateLoss()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private val Int.dp: Int
        get() = (this * Resources.getSystem().displayMetrics.density).toInt()

    companion object {
        const val TAG = "DocumentSourceDialog"
        private const val ARG_KEY = "document_source_dialog_config"
        private const val REQUEST_KEY = "document_source_dialog_request"
        private const val RESULT_SOURCE = "document_source"
        private const val RESULT_VERSION_TYPE = "document_source_version_type"

        private const val WINDOW_INSET_DP = 24

        /**
         * Shows the dialog of [version] over [host], styled from [generalConfigs]; the choice
         * reaches the listener of [setOnSourceChosen].
         */
        internal fun show(host: Fragment, version: Version?, generalConfigs: GeneralConfigs?) {
            if (host.childFragmentManager.findFragmentByTag(TAG) != null) return
            val config = configOf(version, generalConfigs, host.resources)
            DocumentSourceDialog().apply {
                arguments = Bundle().apply { putParcelable(ARG_KEY, config) }
            }.show(host.childFragmentManager, TAG)
        }

        /**
         * Listens for the choice of a dialog shown over [host]. Call it from `onViewCreated` so a
         * recreated host still receives a choice made while it was gone.
         *
         * @param onChosen route of the chosen source (see [chosenSourceRouteOf]) and the type of
         * the document it was chosen for
         */
        internal fun setOnSourceChosen(
            host: Fragment,
            onChosen: (route: ScreenRoutes, versionType: String?) -> Unit
        ) {
            host.childFragmentManager.setFragmentResultListener(
                REQUEST_KEY,
                host.viewLifecycleOwner
            ) { _, result ->
                val source = result.getString(RESULT_SOURCE)
                    ?.let { name -> DocumentSource.values().firstOrNull { it.name == name } }
                    ?: return@setFragmentResultListener
                val route = chosenSourceRouteOf(source) ?: return@setFragmentResultListener
                onChosen.invoke(route, result.getString(RESULT_VERSION_TYPE))
            }
        }

        /**
         * Builds the dialog config of [version] from the general config, falling back to a
         * visible color wherever the config leaves one transparent (the general config
         * defaults its button colors to transparent).
         */
        private fun configOf(
            version: Version?,
            generalConfigs: GeneralConfigs?,
            resources: Resources
        ): DocumentSourceDialogConfig {
            val background = generalConfigs?.appBackground.visibleOr(DEFAULT_BACKGROUND)
            val font = generalConfigs?.appFontColor.visibleOr(ColorConstant.COLOR_BLACK)
            val primaryBackground =
                generalConfigs?.primaryButtonBackgroundColor.visibleOr(ColorConstant.COLOR_BLACK)
            return DocumentSourceDialogConfig(
                versionType = version?.type,
                texts = documentSourceChoiceTextsOf(version, resources),
                backgroundColor = background,
                fontColor = font,
                primaryButtonBackgroundColor = primaryBackground,
                primaryButtonTextColor =
                    generalConfigs?.primaryButtonTextColor.visibleOr(DEFAULT_BACKGROUND),
                primaryButtonBorderColor = generalConfigs?.primaryButtonBorderColor
                    .visibleOr(primaryBackground),
                secondaryButtonBackgroundColor = generalConfigs?.secondaryButtonBackgroundColor
                    .visibleOr(background),
                secondaryButtonTextColor =
                    generalConfigs?.secondaryButtonTextColor.visibleOr(font),
                secondaryButtonBorderColor = generalConfigs?.secondaryButtonBorderColor
                    .visibleOr(faint(font)),
                buttonRadius = generalConfigs?.buttonRadiusAndroid ?: DEFAULT_RADIUS
            )
        }

        private const val DEFAULT_BACKGROUND = "#FFFFFF"
        private const val DEFAULT_RADIUS = 20
        /** ~20% of the font color: a hairline that reads on a light and a dark background. */
        private const val FAINT_ALPHA = 51

        /** The color itself when it parses and is not fully transparent, else [fallback]. */
        private fun String?.visibleOr(fallback: String): String {
            val color = try {
                this?.let(Color::parseColor)
            } catch (e: IllegalArgumentException) {
                null
            }
            return if (color != null && Color.alpha(color) != 0) this!! else fallback
        }

        private fun faint(color: String): String = String.format(
            "#%08X",
            ColorUtils.setAlphaComponent(Color.parseColor(color), FAINT_ALPHA)
        )
    }
}
