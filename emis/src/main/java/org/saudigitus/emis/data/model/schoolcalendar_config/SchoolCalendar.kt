package org.saudigitus.emis.data.model.schoolcalendar_config


import com.google.gson.annotations.SerializedName
import kotlinx.serialization.Serializable

@Serializable
data class SchoolCalendar(
    @SerializedName("academicYear")
    val academicYear: AcademicYear?,
    @SerializedName("classPeriods")
    val classPeriods: List<ClassPeriod?>?,
    @SerializedName("holidays")
    val holidays: List<Holiday?>?,
    @SerializedName("id")
    val id: String?,
    @SerializedName("weekDays")
    val weekDays: WeekDays?
)