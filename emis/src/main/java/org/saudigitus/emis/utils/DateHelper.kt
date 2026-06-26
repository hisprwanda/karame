package org.saudigitus.emis.utils

import android.R.id.input
import org.dhis2.commons.date.DateUtils
import org.saudigitus.emis.data.model.schoolcalendar_config.Holiday
import timber.log.Timber
import java.text.SimpleDateFormat
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale

object DateHelper {
    fun formatDate(date: Long): String? {
        return try {
            val formatter = SimpleDateFormat(DateUtils.DATE_FORMAT_EXPRESSION, Locale.US)
            formatter.format(Date(date))
        } catch (e: Exception) {
            Timber.tag("DATE_FORMAT").e(e)
            null
        }
    }

    fun formatDate(date: String?): String? {
        return try {
            if (date == null) return null

            val inputFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            val outputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

            val date = inputFormat.parse(date)
            return outputFormat.format(date!!)
        } catch (_: Exception) {
            null
        }
    }

    fun stringToLocalDate(date: String): LocalDate {
        return LocalDate.parse(date)
    }

    fun isWeekend(date: LocalDate): Boolean {
        val dayOfWeek = date.dayOfWeek
        return dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY
    }

    fun isHoliday(holidays: List<Holiday>, date: Long): Boolean {
        return holidays.none { holiday ->
            holiday.date == formatDate(date)
        }
    }

    fun formatDateWithWeekDay(date: String): String? {
        return try {
            val inputFormat = SimpleDateFormat(DateUtils.DATE_FORMAT_EXPRESSION, Locale.US)
            val outputFormat = SimpleDateFormat("EEEE, MMM dd, yyyy", Locale.US)

            val inputDate: Date = inputFormat.parse(date)!!
            outputFormat.format(inputDate)
        } catch (e: Exception) {
            Timber.tag("DATE_FORMAT").e(e)
            null
        }
    }

    fun dateStringToSeconds(dateString: String): Long {
        val localDate = LocalDate.parse(dateString)
        return localDate
            .atStartOfDay(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli() / 1000
    }

    fun convertDateToMilliseconds(dateString: String): Long {
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

        val localDate = LocalDate.parse(dateString, formatter)

        val zonedDateTime = localDate.atStartOfDay(ZoneId.systemDefault())
        return zonedDateTime.toInstant().toEpochMilli()
    }
}
