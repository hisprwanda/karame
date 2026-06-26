package org.saudigitus.emis.data.model

import com.google.gson.annotations.SerializedName
import kotlinx.serialization.Serializable

@Serializable
data class Visualization(
    @SerializedName("programIndicator")
    val programIndicator: String,
    @SerializedName("type")
    val type: String
)