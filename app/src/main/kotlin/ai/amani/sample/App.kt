package ai.amani.sample

import ai.amani.base.utility.AmaniVersion
import ai.amani.sdk.DynamicFeature
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
            applicationContext = this.applicationContext,
            serverURL = "TestCredentials.SERVER_URL",
            amaniVersion = AmaniVersion.V2,
            sharedSecret = null,
            enabledFeatures = DynamicFeature.allFeatures
        )

        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
    }
}