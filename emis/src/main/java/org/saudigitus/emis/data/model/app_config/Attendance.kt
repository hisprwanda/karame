package org.saudigitus.emis.data.model.app_config


import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

@JsonIgnoreProperties(ignoreUnknown = true)
data class Attendance(
    @JsonProperty("absenceReason")
    val absenceReason: String?,
    @JsonProperty("enabled")
    val enabled: Boolean?,
    @JsonProperty("attendanceStatus")
    val attendanceStatus: AttendanceStatus?,
    @JsonProperty("lastUpdate")
    val lastUpdate: String?,
    @JsonProperty("programStage")
    val programStage: String?,
    @JsonProperty("status")
    val status: String?,
    @JsonProperty("statusOptions")
    val statusOptions: List<StatusOption>?,
)