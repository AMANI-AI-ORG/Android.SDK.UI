package ai.amani.sdk.model

/**
 * @Author: zekiamani
 * @Date: 1.09.2022
 */
data class UploadResultModel(
    var isSuccess: Boolean = false,
    var result: String? = null,
    var onError: Int? = null,
    var throwable: Throwable? = null
)
