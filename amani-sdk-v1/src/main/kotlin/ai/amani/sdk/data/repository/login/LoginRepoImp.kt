package ai.amani.sdk.data.repository.login

import ai.amani.sdk.data.mapper.LoginResultModelMapper
import ai.amani.sdk.data.mapper.UploadResultModelMapper
import ai.amani.sdk.model.LoginResultModel
import ai.amani.sdk.Amani
import ai.amani.sdk.interfaces.ILoginCallBack
import android.app.Activity

/**
 * @Author: zekiamani
 * @Date: 1.09.2022
 */
class LoginRepoImp : LoginRepository {

    override fun login(
        activity: Activity,
        tcNumber: String,
        token: String,
        lang: String,
        location: Boolean,
        onStart: () -> Unit,
        onCompleted: (loginResultModel: LoginResultModel) -> Unit
    ) {
        onStart.invoke()

        var loginResultModel = LoginResultModel()

        runCatching{
            Amani.sharedInstance().initAmani(
                activity,
                tcNumber,
                token,
                location,
                lang
            ) { isSucess ->
                loginResultModel = LoginResultModelMapper.map(
                    isSucess
                )

                onCompleted.invoke(loginResultModel)
            }
        }.onFailure {
            loginResultModel.throwable = it
            onCompleted.invoke(loginResultModel)
        }
    }
}