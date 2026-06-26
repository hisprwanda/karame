package org.saudigitus.emis.ui.attendance2.state

import org.saudigitus.emis.data.model.SearchTeiModel
import org.saudigitus.emis.ui.attendance.ButtonStep
import org.saudigitus.emis.ui.attendance2.models.AttendanceButtonModel

sealed class AttendanceUiEvent {
    data object BackHandler: AttendanceUiEvent()
    data object SyncHandler: AttendanceUiEvent()
    data object LaunchBulk: AttendanceUiEvent()
    data object DismissBulk: AttendanceUiEvent()
    data object DismissSummary: AttendanceUiEvent()

    data object ClearAttendance: AttendanceUiEvent()

    data object SaveAttendance: AttendanceUiEvent()

    data class AddStep(val step: ButtonStep): AttendanceUiEvent()
    data class AddDate(val date: String): AttendanceUiEvent()
    data class AddAttendance(
        val tei: SearchTeiModel? = null,
        val attendance: AttendanceButtonModel
    ): AttendanceUiEvent()
    data class AddReasonOfAbsence(
        val tei: String,
        val dataElement: String,
        val reason: String
    ): AttendanceUiEvent()

}