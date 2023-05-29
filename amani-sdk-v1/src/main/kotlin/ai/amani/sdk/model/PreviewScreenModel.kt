package ai.amani.sdk.model

import ai.amani.sdk.presentation.home_kyc.ScreenRoutes
import ai.amani.sdk.presentation.selfie.SelfieType
import android.os.Parcelable
import datamanager.model.config.GeneralConfigs
import datamanager.model.config.Version
import kotlinx.parcelize.Parcelize

/**
 * @Author: zekiamani
 * @Date: 6.09.2022
 */
@Parcelize
data class PreviewScreenModel (
    val imageUri: String,
    val configModel: ConfigModel,
    val frontSide: Boolean? = null,
    val selfieType: SelfieType = SelfieType.Manual
    ): Parcelable