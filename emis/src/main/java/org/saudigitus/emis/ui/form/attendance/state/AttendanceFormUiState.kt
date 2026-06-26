package org.saudigitus.emis.ui.form.attendance.state

import org.saudigitus.emis.ui.attendance2.models.AttendanceButtonState
import org.saudigitus.emis.ui.components.ToolbarHeaders
import org.saudigitus.emis.ui.form.attendance.models.FormFieldData
import org.saudigitus.emis.ui.form.attendance.models.FormFieldState

data class AttendanceFormUiState(
    val isLoading: Boolean = false,
    val isEditing: Boolean = false,
    val hasCachedData: Boolean = false,
    val attendanceButtonState: AttendanceButtonState = AttendanceButtonState(),
    val isEnabled: Boolean = false,
    val toolbarHeaders: ToolbarHeaders = ToolbarHeaders(""),
    val fields: List<FormFieldState> = emptyList(),
    val fieldsData: List<FormFieldData> = emptyList(),
    val error: String? = null,
    val isSaved: Boolean = false
)