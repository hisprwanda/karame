package org.saudigitus.emis.ui.attendance2.models

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

data class AttendanceButtonModel(
    val key: String = "",
    val owner: String? = null,
    val code: String? = null,
    val name: String? = null,
    val dataElement: String? = null,
    val icon: ImageVector? = null,
    val iconName: String? = null,
    val enabled: Boolean = true,
    val color: Color? = null,
    val order: Int? = null,
) {
    override fun toString(): String {
        return "AttendanceButtonModel(key='$key')"
    }
}