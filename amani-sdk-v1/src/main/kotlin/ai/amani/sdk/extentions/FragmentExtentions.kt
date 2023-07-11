package ai.amani.sdk.extentions

import ai.amani.BuildConfig
import ai.amani.amani_sdk.R
import ai.amani.sdk.model.UploadResultModel
import android.app.AlertDialog
import android.graphics.Color
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.widget.Toast
import androidx.fragment.app.Fragment
import timber.log.Timber


/**
 * @Author: zekiamani
 * @Date: 20.09.2022
 */

/** Add child fragment without back stack
 * @param container FragmentContainerView
 * @param fragment Fragment will be replaced
 * */
fun Fragment.replaceChildFragmentWithoutBackStack(container: Int, fragment: Fragment) {
    if (fragment.isAdded) return
    this.childFragmentManager
        .beginTransaction()
        .disallowAddToBackStack()
        .setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_right)
        .replace(container, fragment, fragment.javaClass.canonicalName)
        .commit()
}

fun Fragment.removeChildFragment(fragment: Fragment?) {
    if (fragment == null) return
    if (fragment.isRemoving) return
    this.childFragmentManager
        .beginTransaction()
        .remove(fragment)
        .commit()
}

fun Fragment.logUploadResult(
    it: UploadResultModel,
    docType: String
) {
    if (it.isSuccess) Timber.i("$docType: Uploading is succeed")

    if (it.onError != null) Timber.e("$docType: Error existed while uploading document, errorCode: ${it.onError}")

    if (it.throwable != null) Timber.e("$docType:An exception throws while uploading document, exception: ${it.throwable}")
}


fun Fragment.alertDialog(
    titleText: String?,
    descriptionText: String?,
    tryAgainText: String?,
    fontColor: String?,
    backgroundColor: String?,
    onButtonClick: () -> Unit
){

    val appFontColorSpan = ForegroundColorSpan(Color.parseColor(fontColor))
    val titleSsBuilder = SpannableStringBuilder(titleText)
    val descSsBuilder = SpannableStringBuilder(descriptionText)
    val tryAgainSsBuilder = SpannableStringBuilder(tryAgainText)

    titleSsBuilder.setSpan(appFontColorSpan, 0, titleText!!.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
    descSsBuilder.setSpan(appFontColorSpan, 0, descriptionText!!.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
    tryAgainSsBuilder.setSpan(appFontColorSpan, 0, tryAgainText!!.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)

    val alertDialogBuilder = AlertDialog.Builder(requireContext())
    alertDialogBuilder.setTitle(titleSsBuilder)
    alertDialogBuilder.setMessage(descSsBuilder)
    alertDialogBuilder.setPositiveButton(tryAgainSsBuilder) { _, _ ->
        onButtonClick.invoke()
    }

    alertDialogBuilder.setCancelable(false)
    val alertDialog = alertDialogBuilder.create()
    alertDialog.show()
    alertDialog.window!!.setDimAmount(0.6F)
    alertDialog.window!!.setBackgroundDrawableResource(R.drawable.dialog_background)
    alertDialog.window!!.decorView.background?.setTint(Color.parseColor(backgroundColor))
}


fun Fragment.debugToast(message: String?) {
    if (BuildConfig.DEBUG) {
        Toast.makeText(requireContext(), message.toString(), Toast.LENGTH_LONG).show()
    }
}
