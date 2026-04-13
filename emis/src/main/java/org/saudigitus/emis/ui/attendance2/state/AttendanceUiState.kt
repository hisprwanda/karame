package org.saudigitus.emis.ui.attendance2.state

import org.saudigitus.emis.data.model.SearchTeiModel
import org.saudigitus.emis.data.model.Summary
import org.saudigitus.emis.data.model.schoolcalendar_config.SchoolCalendar
import org.saudigitus.emis.data.model.schoolcalendar_config.SchoolCalendarConfig
import org.saudigitus.emis.ui.attendance.ButtonStep
import org.saudigitus.emis.ui.attendance2.models.AttendanceButtonState
import org.saudigitus.emis.ui.attendance2.models.AttendanceStatus
import org.saudigitus.emis.ui.components.InfoCard
import org.saudigitus.emis.ui.components.ToolbarHeaders
import org.saudigitus.emis.ui.form.attendance.models.FormFieldData
import org.saudigitus.emis.ui.form.attendance.models.FormFieldState
import org.saudigitus.emis.utils.DateHelper

sealed class AttendanceUiState(
    open val toolbarHeaders: ToolbarHeaders,
    open val infoCard: InfoCard
) {
    object IDLE : AttendanceUiState(
        ToolbarHeaders(
            title = "Attendance",
            subtitle = DateHelper.formatDateWithWeekDay(
                DateHelper.formatDate(System.currentTimeMillis()).orEmpty()
            )
        ),
        InfoCard(displayBoolean = false)
    )
    object LOADING : AttendanceUiState(
        ToolbarHeaders(
            title = "Attendance",
            subtitle = DateHelper.formatDateWithWeekDay(
                DateHelper.formatDate(System.currentTimeMillis()).orEmpty()
            )
        ),
        InfoCard(displayBoolean = false)
    )

    data class HasAttendance(
        override val toolbarHeaders: ToolbarHeaders,
        override val infoCard: InfoCard,
        val program: String = "",
        val students: List<SearchTeiModel> = emptyList(),
        val attendanceButtonState: AttendanceButtonState = AttendanceButtonState(),
        val fields: List<FormFieldState> = emptyList(),
        val fieldsData: List<FormFieldData> = emptyList(),
        val attendanceStatus: AttendanceStatus? = null,
        val attendanceSummary: List<Summary> = emptyList(),
        val attendanceStep: ButtonStep = ButtonStep.EDITING,
        val canTakeAttendance: Boolean = true,
        val hasInvalidAbsence: Boolean = false,
        val isAttendanceCompleted: Boolean = false,
        val selectedDate: String = DateHelper.formatDate(System.currentTimeMillis()).orEmpty(),
        val hasCachedData: Boolean = false,
        val displayReasonField: Map<String, Boolean> = emptyMap(),
        val displayBulk: Boolean = false,
        val displaySummary: Boolean = false,
        val execSync: Boolean = false,
    ) : AttendanceUiState(toolbarHeaders, infoCard) {
    }
}