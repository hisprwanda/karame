package org.saudigitus.emis.data.model.app_config


import com.google.gson.annotations.SerializedName
import kotlinx.serialization.Serializable

@Serializable
data class Attendance(
    @SerializedName("absenceReason")
    val absenceReason: String?,
    @SerializedName("enabled")
    val enabled: Boolean?,
    @SerializedName("attendanceStatus")
    val attendanceStatus: AttendanceStatus?,
    @SerializedName("lastUpdate")
    val lastUpdate: String?,
    @SerializedName("programStage")
    val programStage: String?,
    @SerializedName("status")
    val status: String?,
    @SerializedName("statusOptions")
    val statusOptions: List<StatusOption>?,
)