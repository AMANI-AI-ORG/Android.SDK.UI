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
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.parcelize.Parcelize
import timber.log.Timber

class NFCScanningBottomDialog : BottomSheetDialogFragment() {
    private var binding : DialogNfcScanBinding? = null
    private var nfcMessages = NFCDialogMessages()

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
        playScanningAnimations()
    }

    private fun playScanningAnimations(){
        if (binding != null) {
            binding?.nfcScanningDescription?.text = nfcMessages.nfcScanningDescription
            binding?.nfcScanningText?.text = nfcMessages.nfcScanningTitle
            binding?.nfcScanningText?.show()
            binding?.nfcScanningDescription?.show()
            binding?.nfcErrorMessage?.hide()
            binding?.loadingView?.startAnimation()
            binding?.nfcScanningAnimation?.playAnimation()
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
        }
    }

    fun dismissSafely() {
        try {
            if (isAdded && !isStateSaved && activity?.isFinishing == false
                && activity?.isDestroyed == false) {
                dismissAllowingStateLoss()
            } else {
                Timber.e("NFC Scanning modal dismiss fail because it was not visible")
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun nfcScanningDoneAnimations() {
        stopScanningAnimation()
        playNFCDoneAnimation()
    }

    private fun stopScanningAnimation() {
        binding?.let {
            it.loadingView.stopAnimation()
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
        val nfcScanningTitle: String? = null
    ): Parcelable
}