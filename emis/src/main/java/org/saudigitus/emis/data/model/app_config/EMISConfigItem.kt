package org.saudigitus.emis.data.model.app_config

import com.google.gson.annotations.SerializedName
import kotlinx.serialization.Serializable

@Serializable
data class EMISConfigItem(
    @SerializedName("absenteeism")
    val absenteeism: Absenteeism?,
    @SerializedName("attendance")
    val attendance: Attendance?,
    @SerializedName("defaults")
    val defaults: Defaults?,
    @SerializedName("filters")
    val filters: Filters?,
    @SerializedName("final-result")
    val finalResult: FinalResult?,
    @SerializedName("key")
    val key: String?,
    @SerializedName("lastUpdate")
    val lastUpdate: String?,
    @SerializedName("performance")
    val performance: Performance?,
    @SerializedName("program")
    val program: String?,
    @SerializedName("reenroll")
    val reenroll: Reenroll?,
    @SerializedName("registration")
    val registration: Registration?,
    @SerializedName("socio-economics")
    val socioEconomics: SocioEconomics?,
    @SerializedName("trackedEntityType")
    val trackedEntityType: String?,
    @SerializedName("transfer")
    val transfer: Transfer?
)
