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

    @Volatile
    private var isLoginDone = false

    @Volatile
    private var loginResultModel = LoginResultModel()

    override fun login(
        activity: Activity,
        tcNumber: String,
        token: String,
        lang: String,
        location: Boolean,
        onStart: () -> Unit,
        onCompleted: (loginResultModel: LoginResultModel) -> Unit
    ) {
        if(isLoginDone) {
            onCompleted.invoke(loginResultModel)
            return
        }

        onStart.invoke()

        runCatching{
            Amani.sharedInstance().initAmani(
                activity= activity,
                id =tcNumber,
                token = token,
                geoLocation = location,
                lang = lang,
                callback = { isSucess ->
                    loginResultModel = LoginResultModelMapper.map(
                        isSucess
                    )
                    onCompleted.invoke(loginResultModel)
                    isLoginDone = true
                }
            )
        }.onFailure {
            loginResultModel.throwable = it
            onCompleted.invoke(loginResultModel)
            isLoginDone = false
        }
    }
}