package ai.amani.sample

import android.app.Application
import timber.log.Timber

/**
 * @Author: zekiamani
 * @Date: 1.09.2022
 */


class App :Application(){

    override fun onCreate() {
        super.onCreate()

        // No AmaniSDKUI.init() here on purpose: the sample starts from the QR entry screen and
        // the scanned QR carries the server URL for that customer, so the SDK is initialised in
        // QrEntryActivity.startKyc() right before the KYC flow is launched.

        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
    }
}
