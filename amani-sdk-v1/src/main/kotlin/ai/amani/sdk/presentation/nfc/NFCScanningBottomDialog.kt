package ai.amani.sdk.presentation.nfc


import ai.amani.amani_sdk.R
import ai.amani.amani_sdk.databinding.DialogNfcScanBinding
import ai.amani.sdk.extentions.gone
import ai.amani.sdk.extentions.hide
import ai.amani.sdk.extentions.parcelable
import ai.amani.sdk.extentions.show
import android.os.Bundle
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.res.ResourcesCompat
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.parcelize.Parcelize

class NFCScanningBottomDialog : BottomSheetDialogFragment() {
    private var binding : DialogNfcScanBinding? = null
    private var nfcMessages = NFCDialogMessages()
    private var onCancelClick: (() -> Unit)? = null

    init {
        this.isCancelable = false
        setStyle(STYLE_NO_FRAME, R.style.BottomSheetTheme)
    }
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = DialogNfcScanBinding.inflate(inflater, container, false)
        return binding!!.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        nfcMessages = arguments?.parcelable<NFCDialogMessages>(ARG_KEY)!!
        showWaitingState()
        binding?.nfcCancelBtn?.setOnClickListener {
            onCancelClick?.invoke()
        }
    }

    fun setOnCancelListener(listener: () -> Unit) {
        onCancelClick = listener
    }

    /** Called immediately when the modal is shown — displays the ready-state
     *  texts from the remote config so the user sees a clear prompt before
     *  any tag interaction. */
    private fun showWaitingState() {
        if (binding != null) {
            binding?.nfcScanningText?.text = nfcMessages.nfcReadyTitle
            binding?.nfcScanningDescription?.text = nfcMessages.nfcReadyDescription
            binding?.nfcScanningText?.show()
            binding?.nfcScanningDescription?.show()
            binding?.nfcErrorMessage?.hide()
            styleAndShowCancelButton()
        }
    }

    /** Called when an NFC tag is detected and scanning begins.
     *  Switches to the dynamic config texts and starts the animations. */
    fun startScanAnimation() {
        binding?.nfcScanningText?.text = nfcMessages.nfcScanningTitle
        binding?.nfcScanningDescription?.text = nfcMessages.nfcScanningDescription
        binding?.loadingView?.startAnimation()
        binding?.nfcScanningAnimation?.playAnimation()
    }

    private fun styleAndShowCancelButton() {
        binding?.nfcCancelBtn?.let { btn ->
            val cancelText = nfcMessages.cancelButtonText
            val textColor = nfcMessages.cancelButtonTextColor
            val bgColor = nfcMessages.cancelButtonBackgroundColor
            val borderColor = nfcMessages.cancelButtonBorderColor
            val radius = nfcMessages.cancelButtonRadius ?: 20

            if (cancelText != null && textColor != null) {
                btn.setTextProperty(cancelText, textColor)
            } else {
                btn.text = cancelText ?: getString(R.string.nfc_cancel)
            }

            if (bgColor != null && borderColor != null) {
                btn.setBackgroundDrawable(
                    ResourcesCompat.getDrawable(resources, R.drawable.custom_btn, null),
                    bgColor,
                    4,
                    borderColor,
                    0f,
                    null,
                    true,
                    radius
                )
            }
            btn.show()
        }
    }

    fun showError() {
        stopScanningAnimation()
        binding?.let {
            it.nfcErrorMessage.text = nfcMessages.nfcFail?: ""
            it.nfcErrorMessage.show()
            it.nfcErrorView.show()
            binding?.nfcScanningText?.gone()
            binding?.nfcScanningDescription?.gone()
            binding?.nfcCancelBtn?.gone()
        }
    }

    fun dismissSafely() {
        try {
            stopScanningAnimation()
            if (isAdded && !isStateSaved && activity?.isFinishing == false
                && activity?.isDestroyed == false) {
                dismissAllowingStateLoss()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun nfcScanningDoneAnimations() {
        stopScanningAnimation()
        playNFCDoneAnimation()
        binding?.nfcCancelBtn?.gone()
    }

    private fun stopScanningAnimation() {
        binding?.let {
            it.loadingView.stopAnimation()
            it.nfcScanningAnimation.cancelAnimation()
        }
    }

    private fun playNFCDoneAnimation() {
        binding?.let {
            it.nfcScanningAnimation.gone()
            it.nfcDoneAnimation.playAnimation()
        }
    }

    companion object {
        const val TAG = "NFCScanningBottomDialog"
        const val ARG_KEY = "nfc_dialog_messages"
    }

    @Parcelize
    data class NFCDialogMessages(
        val nfcFail: String? = null,
        val nfcScanningDescription: String? = null,
        val nfcScanningTitle: String? = null,
        val nfcReadyTitle: String? = null,
        val nfcReadyDescription: String? = null,
        val cancelButtonText: String? = null,
        val cancelButtonTextColor: String? = null,
        val cancelButtonBackgroundColor: String? = null,
        val cancelButtonBorderColor: String? = null,
        val cancelButtonRadius: Int? = null
    ): Parcelable
}
