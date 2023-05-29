package ai.amani.sdk.data.repository.customer

import ai.amani.sdk.Amani
import ai.amani.sdk.model.customer.CustomerDetailResult
import ai.amani.sdk.modules.customer.detail.CustomerDetailObserver
import java.lang.NullPointerException

/**
 * @Author: zekiamani
 * @Date: 7.09.2022
 */

class CustomerDetailRepoImp: CustomerDetailRepository {

     override fun getCustomerDetail (
         onStart:() -> Unit,
         onComplete:(result: CustomerDetailResult) -> Unit,
         onError: (throwable: Throwable?) -> Unit
     ) {
         onStart.invoke()

         Amani.sharedInstance().CustomerDetail().getCustomerDetail(object : CustomerDetailObserver {
             override fun result(customerDetail: CustomerDetailResult?, throwable: Throwable?) {
                 if (throwable != null) onError.invoke(throwable)
                 if (customerDetail != null) onComplete.invoke(customerDetail)
                 else onError.invoke(Throwable(NullPointerException()))
             }
         })
    }
}