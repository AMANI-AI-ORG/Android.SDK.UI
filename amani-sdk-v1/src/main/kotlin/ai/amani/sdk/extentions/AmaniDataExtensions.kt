package ai.amani.sdk.extentions

import ai.amani.sdk.model.amani_events.error.AmaniError
import ai.amani.sdk.model.customer.Rule
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

/**
 * The [StepConfig] of [rule], matched by step id. [getStepConfig] takes the n-th config by the
 * rule's sortOrder, which picks the wrong step when the config also lists steps outside the KYC
 * list (profile_info, OTP, questionnaire) or the sort orders have gaps. Falls back to the
 * sortOrder lookup only when no config carries the rule's id.
 */
internal fun ResGetConfig.getStepConfig(rule: Rule): StepConfig =
    stepConfigs.firstOrNull { it.id != null && it.id == rule.id }
        ?: getStepConfig(rule.sortOrder!!)

internal fun ArrayList<AmaniError?>?.getFirstErrorCode(): Int {
    return try {
        this!!.first()!!.errorCode.toString().toInt()
    } catch (e: Exception) {
        0
    }
}



