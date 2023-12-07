package ai.amani.sdk.model

import android.os.Parcelable
import datamanager.model.config.GeneralConfigs
import datamanager.model.config.Version
import kotlinx.parcelize.Parcelize

/**
 * @Author: zekiamani
 * @Date: 12.09.2022
 */
@Parcelize
data class ConfigModel (
    val version: Version?,
    val generalConfigs: GeneralConfigs? = null,
    val featureConfig: FeatureConfig = FeatureConfig()
    ): Parcelable