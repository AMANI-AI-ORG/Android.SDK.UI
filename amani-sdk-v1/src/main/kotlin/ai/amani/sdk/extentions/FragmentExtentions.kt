package ai.amani.sdk.extentions

import ai.amani.BuildConfig
import ai.amani.amani_sdk.R
import ai.amani.sdk.model.UploadResultModel
import android.app.AlertDialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.view.LayoutInflater
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import net.yslibrary.android.keyboardvisibilityevent.KeyboardVisibilityEvent.setEventListener
import net.yslibrary.android.keyboardvisibilityevent.KeyboardVisibilityEventListener
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
): AlertDialog{

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
    return alertDialog
}


fun Fragment.debugToast(message: String?) {
    if (BuildConfig.DEBUG) {
        Toast.makeText(requireContext(), message.toString(), Toast.LENGTH_LONG).show()
    }
}


fun Fragment.showSnackbar(message: String) {
    val snackbar = Snackbar.make(
        requireActivity().findViewById(android.R.id.content),
        message,
        Snackbar.LENGTH_SHORT
    )

    snackbar.show()
}

fun Fragment.alertDialog(
    mainText: String,
    buttonText: String,
    onButtonClick: () -> Unit
) {
    val factory = LayoutInflater.from(this.requireContext())
    val alertDialogView: View = factory.inflate(R.layout.alert_dialog, null)
    val alertDialog: AlertDialog = AlertDialog.Builder(requireContext()).create()
    alertDialog.setView(
        alertDialogView,
        45,
        0,
        45,
        0)
    alertDialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

    alertDialogView.findViewById<TextView>(R.id.text_dialog).text = mainText
    alertDialogView.findViewById<Button>(R.id.back_to_login).text = buttonText
    alertDialogView.findViewById<Button>(R.id.back_to_login)
        .setOnClickListener {
            onButtonClick.invoke()
            alertDialog.dismiss()
        }

    alertDialog.show()
}

fun Fragment.hideKeyboard() {
    try {
        this.requireActivity().currentFocus?.let {
            val inputMethodManager = ContextCompat.getSystemService(
                this.requireContext(),
                InputMethodManager::class.java)!!
            inputMethodManager.hideSoftInputFromWindow(it.windowToken, 0)
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

fun Fragment.runOnUiThread(action: () -> Unit) {
    requireActivity().runOnUiThread(action)
}

fun Fragment.setKeyboardEventListener(listener: KeyboardVisibilityEventListener) {
    setEventListener(
        this.requireActivity(),
        this.viewLifecycleOwner,
        listener
        )
}

fun Fragment.popBackStackSafely() {
    try {
        if (view == null) return
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.Main) {
            try {
                findNavController().popBackStack()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
}
