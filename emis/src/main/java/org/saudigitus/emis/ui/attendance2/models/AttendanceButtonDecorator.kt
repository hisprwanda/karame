package org.saudigitus.emis.ui.attendance2.models

import androidx.compose.ui.graphics.Color

data class AttendanceButtonDecorator(
    val buttonType: String? = null,
    val containerColor: Color? = null,
    val contentColor: Long = 0xFF888888,
) {
    override fun toString(): String {
        return "AttendanceButtonDecorator(buttonType=$buttonType, containerColor=$containerColor, contentColor=$contentColor)"
    }
}