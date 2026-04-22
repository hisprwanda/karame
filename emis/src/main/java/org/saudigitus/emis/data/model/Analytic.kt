package org.saudigitus.emis.data.model

import com.google.gson.annotations.SerializedName
import kotlinx.serialization.Serializable

@Serializable
data class Analytic(
    @SerializedName("program")
    val program: String,
    @SerializedName("widgets")
    val widgets: List<Widget>
)
