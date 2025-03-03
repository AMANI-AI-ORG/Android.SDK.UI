package ai.amani.sample

import ai.amani.base.utility.AmaniVersion
import android.app.Application
import timber.log.Timber

/**
 * @Author: zekiamani
 * @Date: 1.09.2022
 */


class App :Application(){

    override fun onCreate() {
        super.onCreate()

        AmaniSDKUI.init(
            applicationContext = this,
            serverURL = "TestCredentials.SERVER_URL",
            amaniVersion = AmaniVersion.V2,
            sharedSecret = null
        )

        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
    }
}