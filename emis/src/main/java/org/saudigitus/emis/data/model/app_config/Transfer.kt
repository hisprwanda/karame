package org.saudigitus.emis.data.model.app_config


import com.google.gson.annotations.SerializedName
import kotlinx.serialization.Serializable

@Serializable
data class Transfer(
    @SerializedName("approvedCode")
    val approvedCode: String?,
    @SerializedName("destinySchool")
    val destinySchool: String?,
    @SerializedName("enabled")
    val enabled: Boolean?,
    @SerializedName("lastUpdate")
    val lastUpdate: String?,
    @SerializedName("originSchool")
    val originSchool: String?,
    @SerializedName("penddingCode")
    val penddingCode: String?,
    @SerializedName("programStage")
    val programStage: String?,
    @SerializedName("reprovedCode")
    val reprovedCode: String?,
    @SerializedName("status")
    val status: String?,
    @SerializedName("statusOptions")
    val statusOptions: List<StatusOption>?
)