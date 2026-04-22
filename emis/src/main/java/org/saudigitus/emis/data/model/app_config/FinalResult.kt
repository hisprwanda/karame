package org.saudigitus.emis.data.model.app_config


import com.google.gson.annotations.SerializedName
import kotlinx.serialization.Serializable

@Serializable
data class FinalResult(
    @SerializedName("enabled")
    val enabled: Boolean?,
    @SerializedName("lastUpdate")
    val lastUpdate: String?,
    @SerializedName("programStage")
    val programStage: String?,
    @SerializedName("status")
    val status: String?
)