package org.saudigitus.emis.data.model.schoolcalendar_config


import com.google.gson.annotations.SerializedName
import kotlinx.serialization.Serializable

@Serializable
data class Holiday(
    @SerializedName("date")
    val date: String?,
    @SerializedName("event")
    val event: String?,
    @SerializedName("type")
    val type: String?
)