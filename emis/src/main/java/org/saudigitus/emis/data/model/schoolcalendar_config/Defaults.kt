package org.saudigitus.emis.data.model.schoolcalendar_config


import com.google.gson.annotations.SerializedName
import kotlinx.serialization.Serializable

@Serializable
data class Defaults(
    @SerializedName("academicYear")
    val academicYear: String?
)