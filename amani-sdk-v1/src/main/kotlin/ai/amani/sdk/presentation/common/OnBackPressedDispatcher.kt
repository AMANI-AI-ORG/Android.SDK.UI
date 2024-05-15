package ai.amani.sdk.presentation.common

import ai.amani.sdk.presentation.MainActivity

/**
 * @Author: @zekiamani
 * @Date: 25.12.2023
 */
object OnBackPressedDispatcher {

    interface Listener {
        fun pressed()
    }

    private var listener: Listener? = null

    fun setListener(listener: Listener) {
        this.listener = listener
    }

    fun unRegisterListener() = apply {
        listener = null
    }

    fun MainActivity.backPressed() {
        listener?.pressed()
    }
}