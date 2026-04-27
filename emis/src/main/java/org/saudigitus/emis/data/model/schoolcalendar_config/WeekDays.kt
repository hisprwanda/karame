package org.saudigitus.emis.data.model.schoolcalendar_config


import com.google.gson.annotations.SerializedName
import kotlinx.serialization.Serializable

@Serializable
data class WeekDays(
    @SerializedName("friday")
    val friday: Boolean?,
    @SerializedName("monday")
    val monday: Boolean?,
    @SerializedName("saturday")
    val saturday: Boolean?,
    @SerializedName("sunday")
    val sunday: Boolean?,
    @SerializedName("thursday")
    val thursday: Boolean?,
    @SerializedName("tuesday")
    val tuesday: Boolean?,
    @SerializedName("wednesday")
    val wednesday: Boolean?
)