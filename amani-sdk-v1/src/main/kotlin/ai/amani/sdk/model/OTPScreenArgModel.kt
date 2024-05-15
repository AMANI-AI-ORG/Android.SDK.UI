package ai.amani.sdk.model

import android.os.Parcelable
import datamanager.model.config.GeneralConfigs
import datamanager.model.config.StepConfig
import kotlinx.parcelize.Parcelize

/**
 * @Author: @zekiamani
 * @Date: 9.01.2024
 */
@Parcelize
data class OTPScreenArgModel (
    val steps: ArrayList<StepConfig> = arrayListOf(),
    val config: GeneralConfigs = GeneralConfigs()
):Parcelable
