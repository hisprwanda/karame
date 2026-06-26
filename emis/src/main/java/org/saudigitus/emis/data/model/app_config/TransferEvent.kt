package org.saudigitus.emis.data.model.app_config

import kotlinx.serialization.Serializable

@Serializable
data class TransferEvent(
    val program: String?,
    val academicYear: String?,
    val enrollment: String?,
    val trackerId: String?
)
