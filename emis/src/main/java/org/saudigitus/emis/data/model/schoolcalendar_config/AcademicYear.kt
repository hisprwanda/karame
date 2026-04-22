package org.saudigitus.emis.data.model.schoolcalendar_config


import com.google.gson.annotations.SerializedName
import kotlinx.serialization.Serializable

@Serializable
data class AcademicYear(
    @SerializedName("code")
    val code: String?,
    @SerializedName("description")
    val description: String?,
    @SerializedName("endDate")
    val endDate: String?,
    @SerializedName("label")
    val label: String?,
    @SerializedName("startDate")
    val startDate: String?
)