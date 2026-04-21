package org.saudigitus.emis.data.model.app_config


import com.google.gson.annotations.SerializedName
import kotlinx.serialization.Serializable

@Serializable
data class Registration(
    @SerializedName("enabled")
    val enabled: Boolean?,
    @SerializedName("grade")
    val grade: String?,
    @SerializedName("lastUpdate")
    val lastUpdate: String?,
    @SerializedName("programStage")
    val programStage: String?,
    @SerializedName("section")
    val section: String?
)