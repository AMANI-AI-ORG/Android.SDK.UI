package ai.amani.sdk.model

import com.google.gson.annotations.SerializedName

/**
 * @Author: zekiamani
 * @Date: 2.12.2022
 */
data class StepRuleModel(
    @SerializedName("adapter")
    val adapter: String?,
    @SerializedName("attempt")
    val attempt: Int?,
    @SerializedName("document_classes")
    val documentClasses: List<String?>?,
    @SerializedName("errors")
    val errors: List<Error?>?,
    @SerializedName("id")
    val id: String?,
    @SerializedName("phase")
    val phase: Int?,
    @SerializedName("sort_order")
    val sortOrder: Int = 1,
    @SerializedName("status")
    val status: String?,
    @SerializedName("title")
    val title: String?,
    @SerializedName("isShowLoader")
    var isShowLoader: Boolean = false
)
