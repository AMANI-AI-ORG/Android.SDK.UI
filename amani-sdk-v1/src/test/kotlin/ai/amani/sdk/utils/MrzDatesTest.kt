package ai.amani.sdk.utils

import org.junit.Assert.assertEquals
import org.junit.Test

class MrzDatesTest {

    @Test
    fun `a birth year past the current one belongs to the previous century`() {
        assertEquals("15/06/1985", MrzDates.toDisplay("850615", isBirthDate = true, currentYear = 2026))
    }

    @Test
    fun `a birth year up to the current one stays in this century`() {
        assertEquals("20/11/2010", MrzDates.toDisplay("101120", isBirthDate = true, currentYear = 2026))
    }

    @Test
    fun `an expiry is always in this century`() {
        assertEquals("31/12/2040", MrzDates.toDisplay("401231", isBirthDate = false, currentYear = 2026))
    }

    @Test
    fun `a value that is not an MRZ date is shown as it came`() {
        assertEquals("", MrzDates.toDisplay(null, isBirthDate = true))
        assertEquals("1985", MrzDates.toDisplay("1985", isBirthDate = true))
    }
}
