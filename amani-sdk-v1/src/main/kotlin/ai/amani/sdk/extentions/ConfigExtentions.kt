package ai.amani.sdk.extentions

import datamanager.model.config.ResGetConfig
import datamanager.model.config.StepConfig

/**
 * @Author: zekiamani
 * @Date: 20.09.2022
 */

fun ResGetConfig.getStepConfig(id: Int): StepConfig {
    val configHashMap = HashMap<Int, StepConfig>()
    var i = 0
    for (stepConfig: StepConfig in this.stepConfigs) {
        i += 1
        configHashMap[i] = stepConfig
    }
    return configHashMap[id]!!
}



