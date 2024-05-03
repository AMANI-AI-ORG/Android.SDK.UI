package ai.amani.sdk.extentions

import androidx.navigation.NavController
import androidx.navigation.NavDirections

/**
 * @Author: @zekiamani
 * @Date: 22.12.2023
 */
fun NavController.navigateSafely(directions: NavDirections){
    currentDestination?.getAction(directions.actionId)?.destinationId?:return
    navigate(directions.actionId,directions.arguments,null)
}