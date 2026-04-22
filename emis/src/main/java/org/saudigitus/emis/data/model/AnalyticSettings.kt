package org.saudigitus.emis.data.model

import com.google.gson.annotations.SerializedName
import kotlinx.serialization.Serializable

@Serializable
data class AnalyticSettings(
    @SerializedName("tei")
    val tei: List<Analytic>
)
