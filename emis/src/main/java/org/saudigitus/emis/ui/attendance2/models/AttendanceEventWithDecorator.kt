package org.saudigitus.emis.ui.attendance2.models

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import org.saudigitus.emis.data.model.SearchTeiModel

data class AttendanceEventWithDecorator(
    val tei: SearchTeiModel? = null,
    val event: AttendanceEvent? = null,
    val decorator: AttendanceButtonDecorator? = null,
    val icon:  ImageVector? = null,
    val iconName: String? = null,
    val iconColor: Color? = null,
) {
    override fun toString(): String {
        return "AttendanceEventWithDecorator(tei=$tei, event=$event, decorator=$decorator, icon=$icon, iconName=$iconName, iconColor=$iconColor)"
    }
}