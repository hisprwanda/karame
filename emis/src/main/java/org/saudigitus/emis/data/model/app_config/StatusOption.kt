package org.saudigitus.emis.data.model.app_config

import com.google.gson.annotations.SerializedName
import kotlinx.serialization.Serializable

@Serializable
data class StatusOption(
    @SerializedName("code")
    val code: String?,
    @SerializedName("color")
    val color: String?,
    @SerializedName("ConfigKey")
    val configKey: String?,
    @SerializedName("icon")
    val icon: String?,
    @SerializedName("key")
    val key: String?
)
