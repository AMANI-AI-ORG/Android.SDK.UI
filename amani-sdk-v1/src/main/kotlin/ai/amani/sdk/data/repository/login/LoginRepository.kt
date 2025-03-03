package ai.amani.sdk.data.repository.login

import ai.amani.sdk.model.LoginResultModel
import ai.amani.sdk.utils.Annotations
import android.app.Activity
import androidx.annotation.WorkerThread

/**
 * @Author: zekiamani
 * @Date: 1.09.2022
 */

interface LoginRepository {

    @WorkerThread
    @Annotations.ProdEnvironmentCase("Should be used on prod environment")
    fun login(
        activity: Activity,
        tcNumber: String,
        token: String,
        lang: String,
        location: Boolean,
        onStart: () -> Unit,
        onCompleted: (loginResultModel : LoginResultModel) -> Unit
    )
}