package org.saudigitus.emis.data.model.schoolcalendar_config


import com.google.gson.annotations.SerializedName
import kotlinx.serialization.Serializable

@Serializable
data class SchoolCalendarConfig(
    @SerializedName("academicYear")
    val academicYear: String?,
    @SerializedName("defaults")
    val defaults: Defaults?,
    @SerializedName("schoolCalendar")
    val schoolCalendar: List<SchoolCalendar?>?
)