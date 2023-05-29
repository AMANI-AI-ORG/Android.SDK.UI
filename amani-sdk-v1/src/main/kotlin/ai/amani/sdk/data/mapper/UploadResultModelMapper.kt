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
        isSuccess: Boolean,
        result: String?,
        errors: MutableList<Errors>?
    ): UploadResultModel {
        val idUploadResultModel = UploadResultModel()
        return if (isSuccess) {
            idUploadResultModel.isSuccess = true
            idUploadResultModel.result = result
            idUploadResultModel
        } else {
            if (!errors.isNullOrEmpty()) {
                idUploadResultModel.onError = errors[0].errorCode
                idUploadResultModel
            } else {
                idUploadResultModel
            }
        }
    }
}