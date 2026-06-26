package org.saudigitus.emis.data.model.app_config


import com.google.gson.annotations.SerializedName
import kotlinx.serialization.Serializable

@Serializable
data class Filters(
    @SerializedName("dataElements")
    val dataElements: List<DataElement?>?
)