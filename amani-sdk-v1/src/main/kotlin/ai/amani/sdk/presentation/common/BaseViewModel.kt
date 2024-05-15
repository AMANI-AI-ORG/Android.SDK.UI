package ai.amani.sdk.presentation.common

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavDirections
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

/**
 * @Author: @zekiamani
 * @Date: 21.12.2023
 */
open class BaseViewModel: ViewModel() {

    private var _navigateTo = MutableSharedFlow<NavigationCommands>(replay = 0, extraBufferCapacity = 1, BufferOverflow.DROP_OLDEST)
    val navigateTo: MutableSharedFlow<NavigationCommands> = _navigateTo

    fun navigateTo(action: NavDirections) {
        viewModelScope.launch {
            _navigateTo.emit(NavigationCommands.NavigateDirections(action))
        }
    }

    fun navigateToHomeScreen() {
        viewModelScope.launch {
            _navigateTo.emit(NavigationCommands.NavigateToHomeScreen)
        }
    }


}