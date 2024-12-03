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

        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
    }
}