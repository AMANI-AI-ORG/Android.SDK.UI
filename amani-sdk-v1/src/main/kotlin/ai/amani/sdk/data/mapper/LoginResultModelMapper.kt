package ai.amani.sdk.data.mapper

import ai.amani.sdk.model.LoginResultModel

/**
 * @Author: zekiamani
 * @Date: 1.09.2022
 */
object LoginResultModelMapper {

    fun map(
        isSuccess: Boolean) : LoginResultModel {
        return LoginResultModel(
            isSuccess = isSuccess
        )
    }
}