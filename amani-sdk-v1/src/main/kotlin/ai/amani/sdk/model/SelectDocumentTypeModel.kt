package ai.amani.sdk.model

import android.os.Parcelable
import datamanager.model.config.ResGetConfig
import datamanager.model.config.Version
import kotlinx.parcelize.Parcelize

/**
 * @Author: zekiamani
 * @Date: 12.09.2022
 */
@Parcelize
data class SelectDocumentTypeModel(
    val versionList: List<Version?>,
    val generalConfigs: ResGetConfig?
): Parcelable