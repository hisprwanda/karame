package org.saudigitus.emis.data.model.app_config


import kotlinx.serialization.Serializable

@Serializable
data class Absenteeism(
    val enabled: Boolean?
)