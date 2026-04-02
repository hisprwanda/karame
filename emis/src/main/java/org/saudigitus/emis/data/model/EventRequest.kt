package org.saudigitus.emis.data.model

import kotlinx.serialization.Serializable
import org.hisp.dhis.android.core.event.EventStatus

@Serializable
data class EventRequest(
    val orgUnit: String,
    val program: String,
    val programStage: String,
    val status: String = EventStatus.COMPLETED.name,
    val trackedEntity: String,
    val enrollment: String,
    val occurredAt: String,
    val dataValues: List<DataValue>
)

@Serializable
data class DataValue(
    val dataElement: String,
    val value: String?
)

@Serializable
data class EventBulkRequest(
    val events: List<EventRequest>
)

