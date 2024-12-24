package ai.amani.sdk.presentation.otp.profile_info

import ai.amani.amani_sdk.R
import android.app.DatePickerDialog
import android.content.Context
import android.graphics.Color
import android.widget.DatePicker
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class DatePickerHandler(private val context: Context, listener: (String) -> Unit) {

    private var onDateSelectedListener: ((String) -> Unit)? = null

   init {
       onDateSelectedListener = listener
   }

    fun showDatePickerDialog(dateFormat: String = "yyyy-MM-dd") {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        val datePickerDialog = DatePickerDialog(
            context,
            R.style.DatePickerStyle,
            { _, selectedYear, selectedMonth, selectedDay ->
                val selectedDate = formatDate(
                    format = dateFormat,
                    year = selectedYear,
                    month =selectedMonth,
                    day =selectedDay
                )
                onDateSelectedListener?.invoke(selectedDate)
            },
            year,
            month,
            day
        )

        datePickerDialog.show()
        datePickerDialog.getButton(DatePickerDialog.BUTTON_POSITIVE).setTextColor(Color.BLACK)
        datePickerDialog.getButton(DatePickerDialog.BUTTON_NEGATIVE).setTextColor(Color.BLACK)
    }

    private fun formatDate(format: String, year: Int, month: Int, day: Int): String {
        val calendar = Calendar.getInstance()
        calendar.set(year, month, day)
        val dateFormat = SimpleDateFormat(format, Locale.getDefault())
        return dateFormat.format(calendar.time)
    }
}
