package ai.amani.sdk.extentions

import ai.amani.sdk.model.amani_events.error.AmaniError
import datamanager.model.config.ResGetConfig
import datamanager.model.config.StepConfig

/**
 * @Author: zekiamani
 * @Date: 20.09.2022
 */

internal fun ResGetConfig.getStepConfig(id: Int): StepConfig {
    val configHashMap = HashMap<Int, StepConfig>()
    var i = 0
    for (stepConfig: StepConfig in this.stepConfigs) {
        i += 1
        configHashMap[i] = stepConfig
    }
    return configHashMap[id]!!
}

internal fun ArrayList<AmaniError?>?.getFirstErrorCode(): Int {
    return try {
        this!!.first()!!.errorCode.toString().toInt()
    } catch (e: Exception) {
        0
    }
}



