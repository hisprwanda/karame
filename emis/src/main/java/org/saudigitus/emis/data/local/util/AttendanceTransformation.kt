package org.saudigitus.emis.data.local.util

import androidx.compose.ui.graphics.Color
import org.dhis2.commons.bindings.enrollment
import org.dhis2.commons.date.DateUtils
import org.hisp.dhis.android.core.D2
import org.hisp.dhis.android.core.event.Event
import org.saudigitus.emis.data.model.app_config.Attendance
import org.saudigitus.emis.ui.attendance2.models.AttendanceButtonDecorator
import org.saudigitus.emis.ui.attendance2.models.AttendanceEvent
import org.saudigitus.emis.ui.attendance2.models.AttendanceEventWithDecorator
import org.saudigitus.emis.utils.DateHelper
import org.saudigitus.emis.utils.Transformations
import org.saudigitus.emis.utils.Utils.dynamicIcons
import org.saudigitus.emis.utils.Utils.getAttendanceStatusColor
import javax.inject.Inject

class AttendanceTransformation @Inject constructor(
    private val d2: D2,
    private val transformation: Transformations
) {

    fun teiEventTransform(
        eventUid: String,
        program: String,
        attendanceDataElement: String,
        reasonDataElement: String,
        config: Attendance?,
    ): AttendanceEventWithDecorator {
        val event = d2.eventModule().events()
            .byUid().eq(eventUid)
            .withTrackedEntityDataValues()
            .one().blockingGet()

        val enrollment = d2.enrollment(event!!.enrollment().orEmpty())

        val tei = d2.trackedEntityModule()
            .trackedEntityInstances()
            .byUid().eq(enrollment?.trackedEntityInstance())
            .withTrackedEntityAttributeValues()
            .one().blockingGet()

        val teiModel = transformation.transform(tei, program, enrollment)
        val transformedEvent =
            eventTransform(event, attendanceDataElement, reasonDataElement)

        val status = config?.statusOptions?.find { status ->
            status.code == transformedEvent?.value
        }

        val data = AttendanceEventWithDecorator(
            tei = teiModel,
            event = transformedEvent!!,
            decorator = AttendanceButtonDecorator(
                containerColor = getAttendanceStatusColor(
                    status?.key.orEmpty(),
                    status?.color.orEmpty()
                ),
                contentColor = 0xFFFFFFFF
            ),
            icon = dynamicIcons(status?.icon.orEmpty()),
            iconName = status?.icon.orEmpty(),
            iconColor = Color.White,
        )

        return data
    }

    fun eventTransform(
        event: Event,
        dataElement: String,
        reasonDataElement: String?,
    ): AttendanceEvent? {
        val dataValue = event.trackedEntityDataValues()?.find { it.dataElement() == dataElement }
        val reason = event.trackedEntityDataValues()?.find { it.dataElement() == reasonDataElement }

        return if (dataValue != null) {
            val tei = d2.enrollment(event.enrollment().toString())?.trackedEntityInstance().orEmpty()

            AttendanceEvent(
                tei = tei,
                event = event.uid(),
                enrollment = event.enrollment()!!,
                dataElement = dataElement,
                value = dataValue.value().orEmpty(),
                reasonDataElement = if (reason == null) {
                    null
                } else {
                    reasonDataElement
                },
                reasonOfAbsence = reason?.value(),
                date = DateHelper.formatDate(
                    event.eventDate()?.time ?: DateUtils.getInstance().today.time,
                ).orEmpty(),
            )
        } else {
            null
        }
    }
}