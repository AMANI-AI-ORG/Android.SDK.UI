package ai.amani.sdk.data.mapper

import ai.amani.sdk.model.amani_events.steps_result.StepsResult
import ai.amani.sdk.model.customer.Error
import ai.amani.sdk.model.customer.Rule
import okhttp3.internal.toImmutableList
import timber.log.Timber

/**
 * @Author: zekiamani
 * @Date: 9.01.2023
 */
object StepsResultModelMapper {

    fun map(stepsResult: StepsResult?): List<Rule> {

        if (stepsResult == null) return listOf()

        val documentList = mutableListOf<Rule>()

        if(stepsResult.result.size == 1) {

            val rule = Rule(
                status = stepsResult.result[0].status,
                adapter = null,
                attempt = null,
                documentClasses = null,
                errors = if (!stepsResult.result[0].errors.isNullOrEmpty())
                    if (!stepsResult.result[0].errors.isNullOrEmpty())
                        listOf(
                            Error(
                                errorCode = stepsResult.result[0].errors!![0]!!.errorCode,
                                errorMessage = stepsResult.result[0].errors!![0]!!.errorMessage
                            )
                        ) else listOf() else listOf(),
                id = stepsResult.result[0].id,
                phase = null,
                sortOrder = stepsResult.result[0].sortOrder,
                title = null,
                isShowLoader = false
            )

            documentList.add(0, rule)

            return documentList
        }

        repeat(stepsResult.result.size) {

              val rule =  Rule(
                    status = stepsResult.result[it].status,
                    adapter = null,
                    attempt = null,
                    documentClasses = null,
                    errors = if (!stepsResult.result[it].errors.isNullOrEmpty())
                        if (!stepsResult.result[it].errors.isNullOrEmpty())
                            listOf(
                                Error(
                                    errorCode = stepsResult.result[it].errors!![0]!!.errorCode,
                                    errorMessage = stepsResult.result[it].errors!![0]!!.errorMessage
                                )
                            ) else listOf() else listOf(),
                    id = stepsResult.result[it].id,
                    phase = null,
                    sortOrder = stepsResult.result[it].sortOrder,
                    title = null,
                    isShowLoader = false
                )

            documentList.add(it, rule)
        }

        return documentList.toImmutableList()
    }
}