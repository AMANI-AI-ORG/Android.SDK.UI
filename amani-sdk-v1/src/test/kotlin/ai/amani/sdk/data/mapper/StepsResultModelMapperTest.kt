package ai.amani.sdk.data.mapper

import ai.amani.sdk.model.amani_events.steps_result.Result
import ai.amani.sdk.model.amani_events.steps_result.StepsResult
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

/** The V1 adapter replaces its rows with these rules, so the optional flag must survive the mapping. */
@RunWith(JUnit4::class)
class StepsResultModelMapperTest {

    private fun result(id: String, optional: Boolean?) =
        Result(id = id, status = "APPROVED", sortOrder = 0, errors = null, title = null, optional = optional)

    @Test
    fun `a single step keeps its optional flag`() {
        val rules = StepsResultModelMapper.map(StepsResult(arrayListOf(result("a", true))))
        assertEquals(listOf(true), rules.map { it.optional })
    }

    @Test
    fun `several steps keep their optional flags`() {
        val rules = StepsResultModelMapper.map(
            StepsResult(arrayListOf(result("a", true), result("b", false), result("c", null)))
        )
        assertEquals(listOf(true, false, null), rules.map { it.optional })
    }
}
