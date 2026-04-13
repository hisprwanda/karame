package org.saudigitus.emis.data.local

import org.saudigitus.emis.data.model.SearchTeiModel
import org.saudigitus.emis.data.model.Summary
import org.saudigitus.emis.ui.attendance2.models.AttendanceButtonModel
import org.saudigitus.emis.ui.attendance2.models.AttendanceButtonState
import org.saudigitus.emis.ui.attendance2.models.AttendanceEventWithDecorator
import org.saudigitus.emis.ui.attendance2.models.AttendanceStatus
import org.saudigitus.emis.ui.form.attendance.models.FormFieldData


interface AttendanceRepository {
    suspend fun saveAttendance(
        program: String,
        programStage: String,
        attendanceEvents: List<AttendanceEventWithDecorator>
    )

    suspend fun getAttendanceStatus(program: String, date: String): AttendanceStatus?

    suspend fun loadAttendanceEvents(
        teiUids: List<String>,
        program: String,
        programStage: String,
        dataElement: String,
        reasonDataElement: String,
        eventDate: String?,
        attendanceButtonState: AttendanceButtonState,
    ): AttendanceButtonState

    suspend fun updateAttendanceEvent(
        eventDate: String?,
        tei: SearchTeiModel?,
        attendanceButtonState: AttendanceButtonState,
        buttonModel: AttendanceButtonModel
    ): AttendanceButtonState

    fun attendanceSummary(
        attendanceButtonState: AttendanceButtonState,
    ): List<Summary>

    fun hasInvalidData(
        attendanceButtonState: AttendanceButtonState,
    ): Boolean

    fun isAttendanceCompleted(
        students: List<SearchTeiModel>,
        attendanceButtonState: AttendanceButtonState
    ): Boolean

    fun clearAll(attendanceButtonState: AttendanceButtonState): AttendanceButtonState
}