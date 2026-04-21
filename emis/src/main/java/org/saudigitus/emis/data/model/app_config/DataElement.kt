package org.saudigitus.emis.data.model.app_config


import com.google.gson.annotations.SerializedName
import kotlinx.serialization.Serializable

@Serializable
data class DataElement(
    @SerializedName("code")
    val code: String?,
    @SerializedName("dataElement")
    val dataElement: String?,
    @SerializedName("label")
    val label: String?,
    @SerializedName("order")
    val order: Int?,
    @SerializedName("ulrParam")
    val ulrParam: String?
)