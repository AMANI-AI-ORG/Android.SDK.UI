package ai.amani.sdk.model

/**
 * @Author: zekiamani
 * @Date: 1.09.2022
 */
data class LoginResultModel(
    var isSuccess: Boolean = false,
    var error: Int? = null,
    var throwable: Throwable? = null
)
