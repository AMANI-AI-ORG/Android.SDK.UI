package ai.amani.sdk.extentions

import android.content.Context
import android.nfc.NfcManager
import androidx.lifecycle.ViewModel

/**
 * @Author: zekiamani
 * @Date: 5.10.2022
 */

fun ViewModel.deviceNFCState(
    context: Context,
    available: () -> Unit,
    disable: () -> Unit,
    notSupported: () -> Unit
) {
    val manager = context.getSystemService(Context.NFC_SERVICE) as NfcManager
    val adapter = manager.defaultAdapter
    if (adapter != null && adapter.isEnabled) {
        //Yes NFC available
        available.invoke()
    } else if (adapter != null && !adapter.isEnabled) {
        //NFC is not enabled.Need to enable by the user.
        disable.invoke()
    } else {
        //NFC is not supported
        notSupported.invoke()
    }
}

fun ViewModel.deviceHasNFC(
    context: Context,
) : Boolean {
    val manager = context.getSystemService(Context.NFC_SERVICE) as NfcManager
    val adapter = manager.defaultAdapter
    return if (adapter != null && adapter.isEnabled) {
        //Device has NFC
        true
    } else {
        // Device has NO NFC
        adapter != null && !adapter.isEnabled
    }
}