package org.saudigitus.emis.data.model.app_config

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class TransferEvent(
    val program: String?,
    val academicYear: String?,
    val enrollment: String?,
    val trackerId: String?
)
