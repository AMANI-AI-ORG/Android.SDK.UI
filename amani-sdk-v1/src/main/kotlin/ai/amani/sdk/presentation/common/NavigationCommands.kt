package ai.amani.sdk.presentation.common

import androidx.navigation.NavDirections

sealed class NavigationCommands {
    data class NavigateDirections(val direction: NavDirections) : NavigationCommands()
    object NavigateToHomeScreen : NavigationCommands()
}