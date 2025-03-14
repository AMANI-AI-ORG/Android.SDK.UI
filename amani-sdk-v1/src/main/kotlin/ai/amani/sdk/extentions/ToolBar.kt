package ai.amani.sdk.extentions

import ai.amani.sdk.presentation.AmaniMainActivity
import android.graphics.BlendMode
import android.graphics.BlendModeColorFilter
import android.graphics.PorterDuff
import android.graphics.drawable.Drawable
import android.os.Build
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController

/**
 * @Author: zekiamani
 * @Date: 15.09.2022
 */

fun Fragment.customizeToolBar(
    backgroundColor: String?,
    backImgColor: String?,
    titleTextColor: String?,
    title: String?
) {
    AmaniMainActivity.customizeToolBar(
        title,
        titleTextColor,
        backgroundColor,
        backImgColor
    )

    AmaniMainActivity.isBackButtonEnabled(isEnabled = true)
    findNavController().currentDestination!!.label = title
}

fun Fragment.setToolBarTitle(
    title : String?,
    navigationIconColor: String? = null
) {

    AmaniMainActivity.setToolBar(
        title,
        navigationIconColor
    )

    findNavController().currentDestination!!.label = title
}

fun Drawable.setColorFilter(color: Int) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        colorFilter = BlendModeColorFilter(color, BlendMode.SRC)
    } else {
        setColorFilter(color, PorterDuff.Mode.SRC)
    }
}