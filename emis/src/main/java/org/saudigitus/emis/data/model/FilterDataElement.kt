package org.saudigitus.emis.data.model

import com.google.gson.annotations.SerializedName
import kotlinx.serialization.Serializable

@Serializable
data class FilterDataElement(
    @SerializedName("code")
    val code: String?,
    @SerializedName("dataElement")
    val dataElement: String?,
    @SerializedName("label")
    val label: String?,
    @SerializedName("order")
    val order: Int?
)
