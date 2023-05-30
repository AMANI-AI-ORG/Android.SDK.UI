package ai.amani.sdk.presentation.binding

import ai.amani.amani_sdk.R
import ai.amani.sdk.presentation.common.CustomButton
import ai.amani.sdk.presentation.home_kyc.HomeKYCState
import ai.amani.sdk.utils.ColorConstant
import android.app.Activity
import android.content.res.ColorStateList
import android.graphics.Color
import android.net.Uri
import android.view.View
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.content.res.ResourcesCompat
import androidx.databinding.BindingAdapter
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import datamanager.model.config.GeneralConfigs
import datamanager.model.config.Version
import timber.log.Timber
import java.io.File


@BindingAdapter("bindingProgressLoader")
fun bindingProgressBar(progressLoader: ProgressBar, isLoading: Boolean) {
    if (isLoading) {
        progressLoader.visibility = View.VISIBLE
    } else progressLoader.visibility = View.INVISIBLE
}

@BindingAdapter("bindLoading")
fun bindingLoading(swipe: SwipeRefreshLayout, isLoading: Boolean) {
    Timber.tag("bindingLoading").e(" isLoading = ${isLoading}")
    swipe.isRefreshing = isLoading
    if (!isLoading) swipe.isEnabled = false
}


@BindingAdapter("bindFinish")
fun bindingFinish(view: View, finish: Boolean) {
    val ctx = view.context
    if (ctx is Activity && finish) {
        view.setOnClickListener { ctx.finish() }
    }
}

@BindingAdapter("homeKycUiState")
fun setUIForLoading(progressLoader: ProgressBar, homeKYCState: HomeKYCState?) {
    if (homeKYCState == null) return
    progressLoader.visibility = when(homeKYCState) {
        HomeKYCState.Loading -> View.VISIBLE
        else -> View.INVISIBLE
    }
}

@BindingAdapter("bitmapAsUri")
fun ImageView.setImageAsUri(imageUri: String) {
    Glide.with(this.context)
        .load(imageUri) // Uri of the picture
        .diskCacheStrategy(DiskCacheStrategy.NONE)
        .skipMemoryCache(true)
        .into(this);
}

@BindingAdapter("buttonTryAgain")
fun CustomButton.setPropertyTryAgainButton(
    generalConfigs: GeneralConfigs?
) {
    val tryAgain =
        if (generalConfigs!!.tryAgainText != null) generalConfigs.tryAgainText else resources.getString(
            R.string.try_again_text
        )
    val appBackgroundColor =
        if (generalConfigs.appBackground != null) generalConfigs.appBackground else ColorConstant.COLOR_BLACK
    val secondaryButtonText =
        if (generalConfigs.secorndaryButtonTextColor != null) generalConfigs.secorndaryButtonTextColor else ColorConstant.COLOR_BLACK
    val secondaryButtonBorder =
        if (generalConfigs.secorndaryButtonBorderColor != null) generalConfigs.secorndaryButtonBorderColor else ColorConstant.COLOR_BLACK
        val buttonRadius =
        if (generalConfigs.buttonRadiusAndroid != null) generalConfigs.buttonRadiusAndroid else 20

    this.setTextProperty(tryAgain, secondaryButtonText)
    this.setBackgroundDrawable(
        ResourcesCompat.getDrawable(resources, R.drawable.custom_btn, null), appBackgroundColor,
        4, secondaryButtonBorder, 0f, null, true, buttonRadius)
}

@BindingAdapter("buttonConfirm")
fun CustomButton.setPropertyConfirmButton(
    generalConfigs: GeneralConfigs?
) {
    val confirmText =
        if (generalConfigs!!.confirmText != null) generalConfigs.confirmText else resources.getString(
            R.string.confirm_text
        )
    val primaryButtonText =
        if (generalConfigs.primaryButtonTextColor != null) generalConfigs.primaryButtonTextColor else ColorConstant.COLOR_BLACK
    val primaryButtonBackground =
        if (generalConfigs.primaryButtonBackgroundColor != null) generalConfigs.primaryButtonBackgroundColor else ColorConstant.COLOR_BLACK
    val buttonRadius =
        if (generalConfigs.buttonRadiusAndroid != null) generalConfigs.buttonRadiusAndroid else 20

    this.setTextProperty(confirmText, primaryButtonText)
    this.setBackgroundDrawable(
        ResourcesCompat.getDrawable(resources, R.drawable.custom_btn, null), primaryButtonBackground,
        4, primaryButtonBackground, 0f, null, true, buttonRadius)
}

@BindingAdapter("setBackgroundColor")
fun View.setBackgroundColor(
    color: String?
) {
    this.setBackgroundColor(Color.parseColor(color))
}

@BindingAdapter("setText")
fun TextView.setText(
    text: String?
) {
    text?.let {
        this.text = text
    }
}

@BindingAdapter("setTextColor")
fun TextView.setTextColor(
    color: String?
) {
    color?.let {
        this.setTextColor(Color.parseColor(color))
    }
}

@BindingAdapter("setProgressLoaderColor")
fun ProgressBar.setProgressLoaderColor(
    color: String?
) {
    color?.let {
        this.progressTintList = ColorStateList.valueOf(Color.parseColor(color))
    }
}

@BindingAdapter("setColor")
fun ImageView.setColor(color : String?) {
    color?.let {
        this.setColorFilter(Color.parseColor(it))
    }
}


