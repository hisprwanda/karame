package org.saudigitus.emis.data.model

import com.google.gson.annotations.SerializedName
import kotlinx.serialization.Serializable

@Serializable
data class Widget(
    @SerializedName("displayName")
    val displayName: String,
    @SerializedName("visualizations")
    val visualizations: List<Visualization>
)
