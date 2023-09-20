package ai.amani.sdk.data.repository.config

import ai.amani.sdk.Amani
import ai.amani.sdk.modules.config.app.AppConfigObserver
import androidx.annotation.WorkerThread
import datamanager.model.config.ResGetConfig

/**
 * @Author: zekiamani
 * @Date: 6.09.2022
 */
class ConfigRepositoryImp: ConfigRepository {

    @WorkerThread
    override fun getAppConfig(
        onStart: () -> Unit,
        onComplete: (result: ResGetConfig) -> Unit,
        onError: (errorCode: Int) -> Unit
    ) {
        onStart.invoke()

        Amani.sharedInstance().AppConfig().getApplicationConfig(object : AppConfigObserver{
            override fun result(applicationConfig: ResGetConfig?, errorCode: Int?) {
                if (applicationConfig != null ) {
                    onComplete.invoke(applicationConfig)
                } else if (errorCode != null) {
                    onError.invoke(errorCode)
                }
            }
        })
    }

    @WorkerThread
    override fun fetchAppConfig(
        onStart: () -> Unit,
        onComplete: (result: ResGetConfig) -> Unit,
        onError: (errorCode: Int) -> Unit
    ) {
        onStart.invoke()

        Amani.sharedInstance().AppConfig().fetchApplicationConfig(object : AppConfigObserver{
            override fun result(applicationConfig: ResGetConfig?, errorCode: Int?) {
                if (applicationConfig != null ) {
                    onComplete.invoke(applicationConfig)
                } else if (errorCode != null) {
                    onError.invoke(errorCode)
                }
            }
        })
    }
}