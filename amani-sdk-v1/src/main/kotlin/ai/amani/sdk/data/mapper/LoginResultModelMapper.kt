package ai.amani.sdk.data.mapper

import ai.amani.sdk.model.LoginResultModel

/**
 * @Author: zekiamani
 * @Date: 1.09.2022
 */
object LoginResultModelMapper {

    fun map(
        isSuccess: Boolean,
        error: Int?) : LoginResultModel {
        val loginResultModel = LoginResultModel()
        return if (isSuccess) {
            loginResultModel.isSuccess = true
            loginResultModel
        } else {
            if (error != null) {
                loginResultModel.error = error
                loginResultModel
            } else loginResultModel
        }
    }
}