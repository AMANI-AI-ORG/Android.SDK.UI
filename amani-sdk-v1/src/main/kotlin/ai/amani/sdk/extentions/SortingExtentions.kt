package ai.amani.sdk.extentions

import ai.amani.sdk.model.customer.Rule
import java.util.*

/**
 * @Author: zekiamani
 * @Date: 7.03.2023
 */

fun ArrayList<Rule>.sort(): ArrayList<Rule> {
    this.sortWith(Comparator { o1, o2 ->
        val p1 = o1 as Rule
        val p2 = o2 as Rule
        p2.sortOrder?.let { p1.sortOrder!!.compareTo(it) }!!
    })
    return this
}
