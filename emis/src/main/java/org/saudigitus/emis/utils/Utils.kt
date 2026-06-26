package org.saudigitus.emis.utils

import androidx.compose.material.icons.Icons
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import org.dhis2.commons.resources.ColorUtils
import org.saudigitus.emis.R
import org.saudigitus.emis.data.model.TransferredTei
import org.saudigitus.emis.data.model.TransferredType
import org.saudigitus.emis.data.model.app_config.EMISConfig
import org.saudigitus.emis.data.model.app_config.TransferEvent
import kotlin.random.Random

object Utils {
    const val GREEN = 0xFF81C784
    const val RED = 0xFFE57373
    const val ORANGE = 0xFFFFB74D
    const val WHITE = 0xFFFFFFFF

    const val GRAY = 0xFF888888

    fun getIconByName(name: String) = when (name) {
        "correct_blue_fill" -> R.drawable.present
        "wrong_red_fill" -> R.drawable.absent
        "clock_orange_fill" -> R.drawable.late
        else -> R.drawable.ic_empty
    }

    fun dynamicIcons(name: String) = try {
        val cl = Class.forName("androidx.compose.material.icons.filled.${name}Kt")
        val method = cl.declaredMethods.first()
        method.invoke(null, Icons.Filled) as ImageVector
    } catch (_: Throwable) {
        null
    }

    fun isStringCastableToInt(str: String): Boolean {
        return try {
            str.toInt()
            true
        } catch (e: NumberFormatException) {
            false
        }
    }

    fun getAttendanceStatusColor(key: String, statusColor: String): Color {
        return try {
            val colorUtils = ColorUtils()
            Color(colorUtils.parseColor(statusColor))
        } catch (_: Exception) {
            getAttendanceStatusColor(key)
        }
    }

    fun getAttendanceStatusColor(key: String): Color {
        return when (key) {
            "present" -> Color(0xFF81C784)
            "absent" -> Color(0xFFE57373)
            "late" -> Color(0xFFFACC95)
            else -> Color.LightGray
        }
    }

    fun generateRandomId(length: Int = 11): String {
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"
        return (1..length)
            .map { chars[Random.nextInt(chars.length)] }
            .joinToString("")
    }


    fun mapToType(
        dataElement: String,
        transferred: TransferEvent
    ): TransferredType? {
        return when (dataElement) {
            transferred.trackerId -> TransferredType.TRACKER_ID
            transferred.academicYear -> TransferredType.ACADEMIC_YEAR
            transferred.enrollment -> TransferredType.ENROLLMENT
            else -> null
        }
    }
}
