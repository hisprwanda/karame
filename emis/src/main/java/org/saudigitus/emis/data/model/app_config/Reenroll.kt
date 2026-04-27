package org.saudigitus.emis.data.model.app_config


import com.google.gson.annotations.SerializedName
import kotlinx.serialization.Serializable

@Serializable
data class Reenroll(
    @SerializedName("enabled")
    val enabled: Boolean?
)