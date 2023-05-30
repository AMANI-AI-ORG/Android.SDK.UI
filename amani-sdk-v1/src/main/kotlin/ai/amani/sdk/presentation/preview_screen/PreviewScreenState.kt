package ai.amani.sdk.presentation.preview_screen

import androidx.annotation.StringRes

/**
 * @Author: zekiamani
 * @Date: 26.09.2022
 */
sealed interface PreviewScreenState {

    object Loading : PreviewScreenState

    object Loaded: PreviewScreenState

    object OutOfMaxAttempt : PreviewScreenState

    class Error(@StringRes val errorMessageId: Int = 0) : PreviewScreenState

}