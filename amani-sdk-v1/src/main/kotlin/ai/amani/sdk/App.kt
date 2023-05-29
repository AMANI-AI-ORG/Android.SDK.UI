package ai.amani.sdk

import ai.amani.amani_sdk.BuildConfig
import ai.amani.base.utility.AmaniVersion
import ai.amani.sdk.di.AppContainer
import android.app.Application
import timber.log.Timber

/**
 * @Author: zekiamani
 * @Date: 1.09.2022
 */


class App :Application(){

    val appContainer = AppContainer()

    override fun onCreate() {
        super.onCreate()

        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
    }
}