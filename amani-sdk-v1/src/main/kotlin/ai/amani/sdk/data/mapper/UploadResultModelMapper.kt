package ai.amani.sdk.data.mapper

import ai.amani.sdk.model.UploadResultModel
import ai.amani.sdk.interfaces.IUploadCallBack
import datamanager.model.customer.Errors
import java.lang.Error

/**
 * @Author: zekiamani
 * @Date: 1.09.2022
 */

object UploadResultModelMapper {

    fun map(
        isSuccess: Boolean
    ): UploadResultModel {
        return UploadResultModel(
            isSuccess = isSuccess
        )
    }
}