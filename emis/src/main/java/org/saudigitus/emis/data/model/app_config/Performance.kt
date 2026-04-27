package org.saudigitus.emis.data.model.app_config


import com.google.gson.annotations.SerializedName
import kotlinx.serialization.Serializable

@Serializable
data class Performance(
    @SerializedName("enabled")
    val enabled: Boolean?,
    @SerializedName("lastUpdate")
    val lastUpdate: String?,
    @SerializedName("programStages")
    val programStages: List<ProgramStages?>?
)