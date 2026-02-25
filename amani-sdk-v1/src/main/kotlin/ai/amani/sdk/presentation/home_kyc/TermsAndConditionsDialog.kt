package ai.amani.sdk.presentation.home_kyc

import ai.amani.amani_sdk.R
import ai.amani.amani_sdk.databinding.DialogTermsAndConditionsBinding
import ai.amani.sdk.extentions.parcelable
import ai.amani.sdk.model.TermsAndConditionsConfig
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.toColorInt
import androidx.fragment.app.DialogFragment
import androidx.core.graphics.drawable.toDrawable

class TermsAndConditionsDialog : DialogFragment() {

    private var binding: DialogTermsAndConditionsBinding? = null
    private var onTermsResultListener: ((accepted: Boolean) -> Unit)? = null
    private var scrollChangedListener: ViewTreeObserver.OnScrollChangedListener? = null

    init {
        isCancelable = false
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DialogTermsAndConditionsBinding.inflate(inflater, container, false)
        return binding!!.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val config = arguments?.parcelable<TermsAndConditionsConfig>(ARG_KEY) ?: return
        dialog?.window?.setBackgroundDrawable(Color.TRANSPARENT.toDrawable())
        dialog?.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
        applyConfig(config)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding?.scrollDescription?.viewTreeObserver?.removeOnScrollChangedListener(scrollChangedListener)
        scrollChangedListener = null
        binding = null
    }

    fun setOnTermsAcceptedListener(listener: (accepted: Boolean) -> Unit) {
        onTermsResultListener = listener
    }

    private fun applyConfig(config: TermsAndConditionsConfig) {
        val b = binding ?: return

        b.rootContainer.setBackgroundColor(config.backgroundColor.toColorInt())

        b.tvTitle.text = config.titleText
        b.tvTitle.setTextColor(config.titleTextColor.toColorInt())

        b.tvDescription.text = config.descriptionText
        b.tvDescription.setTextColor(config.descriptionTextColor.toColorInt())

        b.btnPrimary.setTextProperty(config.acceptButtonText, config.primaryButtonTextColor)
        val primaryDrawable = ResourcesCompat.getDrawable(resources, R.drawable.custom_btn, null)
        b.btnPrimary.setBackgroundDrawable(
            primaryDrawable,
            config.primaryButtonBackgroundColor,
            4,
            config.primaryButtonBorderColor,
            0f,
            null,
            true,
            config.buttonRadius
        )

        b.btnPrimary.isEnabled = false
        b.btnPrimary.alpha = 0.5f

        b.btnSecondary.alpha = 0.5f
        b.btnSecondary.isEnabled = false

        b.btnPrimary.setOnClickListener {
            onTermsResultListener?.invoke(true)
            dismissSafely()
        }

        scrollChangedListener = ViewTreeObserver.OnScrollChangedListener {
            val child = b.scrollDescription.getChildAt(0) ?: return@OnScrollChangedListener
            if (b.scrollDescription.scrollY + b.scrollDescription.height >= child.height) {
                enableAcceptButton(b)
                enableDeclineButton(b)
            }
        }
        b.scrollDescription.viewTreeObserver.addOnScrollChangedListener(scrollChangedListener)
        // Enable immediately if content doesn't require scrolling
        b.scrollDescription.post {
            val child = b.scrollDescription.getChildAt(0)
            if (child == null || child.height <= b.scrollDescription.height) {
                enableAcceptButton(b)
                enableDeclineButton(b)
            }
        }

        b.btnSecondary.setTextProperty(config.declineButtonText ?: "", config.secondaryButtonTextColor)
        val secondaryDrawable = ResourcesCompat.getDrawable(resources, R.drawable.custom_btn, null)

        b.btnSecondary.setBackgroundDrawable(
            secondaryDrawable,
            config.secondaryButtonBackgroundColor,
            4,
            config.secondaryButtonBorderColor,
            0f,
            null,
            true,
            config.buttonRadius
        )
        b.btnSecondary.setOnClickListener {
            onTermsResultListener?.invoke(false)
            dismissSafely()
        }
    }

    private fun enableAcceptButton(b: DialogTermsAndConditionsBinding) {
        if (!b.btnPrimary.isEnabled) {
            b.btnPrimary.isEnabled = true
            b.btnPrimary.alpha = 1f
        }
    }

    private fun enableDeclineButton(b: DialogTermsAndConditionsBinding) {
        if (!b.btnSecondary.isEnabled) {
            b.btnSecondary.isEnabled = true
            b.btnSecondary.alpha = 1f
        }
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

    companion object {
        const val TAG = "TermsAndConditionsDialog"
        const val ARG_KEY = "terms_dialog_config"

        fun newInstance(config: TermsAndConditionsConfig): TermsAndConditionsDialog {
            return TermsAndConditionsDialog().apply {
                arguments = Bundle().apply {
                    putParcelable(ARG_KEY, config)
                }
            }
        }
    }
}
