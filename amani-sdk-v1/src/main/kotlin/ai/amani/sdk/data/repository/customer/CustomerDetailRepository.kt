package ai.amani.sdk.data.repository.customer

import ai.amani.sdk.model.customer.CustomerDetailResult


/**
 * @Author: zekiamani
 * @Date: 7.09.2022
 */
interface CustomerDetailRepository {

    fun getCustomerDetail(
        onStart:() -> Unit,
        onComplete:(result: CustomerDetailResult) -> Unit,
        onError: (throwable: Throwable?) -> Unit
    )
}