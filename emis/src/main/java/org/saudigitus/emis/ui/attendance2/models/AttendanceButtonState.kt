package org.saudigitus.emis.ui.attendance2.models

import androidx.compose.runtime.Immutable
import org.saudigitus.emis.utils.Utils

@Immutable
data class AttendanceButtonState(
    val key: String = Utils.generateRandomId(),
    val isLoading: Boolean = false,
    val isEditing: Boolean = false,
    val buttons: List<AttendanceButtonModel> = emptyList(),
    val attendanceEvents: List<AttendanceEventWithDecorator> = emptyList(),
) {
    fun hasEvent() = attendanceEvents.any { it.event != null }
}