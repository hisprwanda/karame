package org.saudigitus.emis.data.model.schoolcalendar_config


import com.google.gson.annotations.SerializedName
import kotlinx.serialization.Serializable

@Serializable
data class ClassPeriod(
    @SerializedName("description")
    val description: String?,
    @SerializedName("endDate")
    val endDate: String?,
    @SerializedName("key")
    val key: String?,
    @SerializedName("startDate")
    val startDate: String?
)