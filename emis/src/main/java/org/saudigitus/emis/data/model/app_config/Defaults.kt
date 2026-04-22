package org.saudigitus.emis.data.model.app_config


import com.google.gson.annotations.SerializedName
import kotlinx.serialization.Serializable

@Serializable
data class Defaults(
    @SerializedName("allowSearching")
    val allowSearching: Boolean?,
    @SerializedName("defaultOrder")
    val defaultOrder: String?
)