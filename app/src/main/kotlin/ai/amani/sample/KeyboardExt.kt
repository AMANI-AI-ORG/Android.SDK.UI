package ai.amani.sample

import android.content.Context
import android.content.Intent
import android.os.Build.VERSION.SDK_INT
import android.os.Bundle
import android.os.Parcelable
import android.view.inputmethod.InputMethodManager
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat


/**
 * @Author: zekiamani
 * @Date: 14.03.2023
 */

fun AppCompatActivity.hideKeyboard() {
    this.currentFocus?.let {
        val inputMethodManager = ContextCompat.getSystemService(this, InputMethodManager::class.java)!!
        inputMethodManager.hideSoftInputFromWindow(it.windowToken, 0)
    }
}

inline fun <reified T : Parcelable> Intent.parcelable(key: String): T? = when {
    SDK_INT >= 33 -> getParcelableExtra(key, T::class.java)
    else -> @Suppress("DEPRECATION") getParcelableExtra(key) as? T
}

inline fun <reified T : Parcelable> Bundle.parcelable(key: String): T? = when {
    SDK_INT >= 33 -> getParcelable(key, T::class.java)
    else -> @Suppress("DEPRECATION") getParcelable(key) as? T
}