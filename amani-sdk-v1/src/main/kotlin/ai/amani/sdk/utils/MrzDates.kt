package ai.amani.sdk.utils

import java.util.Calendar

/**
 * MRZ dates are `yyMMdd`: that is what the chip reader is handed, and it never changes here.
 * The screen shows them as `dd/MM/yyyy`, the way they are printed on the document.
 */
internal object MrzDates {

    private const val MRZ_DATE_LENGTH = 6

    /**
     * `yyMMdd` → `dd/MM/yyyy`; anything else is returned as it came. A birth year in the future
     * belongs to the previous century, an expiry never does.
     */
    fun toDisplay(mrzDate: String?, isBirthDate: Boolean, currentYear: Int = currentYear()): String {
        if (mrzDate == null) return ""
        val digits = mrzDate.filter { it.isDigit() }
        if (digits.length != MRZ_DATE_LENGTH) return mrzDate
        val yy = digits.substring(0, 2).toInt()
        val year = when {
            !isBirthDate -> 2000 + yy
            yy > currentYear % 100 -> 1900 + yy
            else -> 2000 + yy
        }
        return "${digits.substring(4, 6)}/${digits.substring(2, 4)}/$year"
    }

    private fun currentYear(): Int = Calendar.getInstance().get(Calendar.YEAR)
}
