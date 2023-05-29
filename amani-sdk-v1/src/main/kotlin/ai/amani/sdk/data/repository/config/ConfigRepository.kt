package ai.amani.sdk.data.repository.config

import datamanager.model.config.ResGetConfig

/**
 * @Author: zekiamani
 * @Date: 6.09.2022
 */
interface ConfigRepository {

    /** ResGetConfig from local data while login process*/
    fun getAppConfig(
        onStart:() -> Unit,
        onComplete:(result: ResGetConfig) -> Unit,
        onError: (errorCode:Int) -> Unit
    )

    /** Most up-to date ResGetConfig from remote service*/
    fun fetchAppConfig(
        onStart:() -> Unit,
        onComplete:(result: ResGetConfig) -> Unit,
        onError: (errorCode:Int) -> Unit
    )
}