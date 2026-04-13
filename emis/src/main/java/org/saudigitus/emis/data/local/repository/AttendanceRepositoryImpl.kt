package org.saudigitus.emis.data.local.repository

import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.dhis2.commons.bindings.dataElement
import org.dhis2.commons.date.DateUtils
import org.hisp.dhis.android.core.D2
import org.hisp.dhis.android.core.event.EventCreateProjection
import org.hisp.dhis.android.core.event.EventStatus
import org.saudigitus.emis.data.local.AttendanceRepository
import org.saudigitus.emis.data.local.DataManager
import org.saudigitus.emis.data.local.util.AttendanceTransformation
import org.saudigitus.emis.data.model.SearchTeiModel
import org.saudigitus.emis.data.model.Summary
import org.saudigitus.emis.ui.attendance2.models.AttendanceButtonDecorator
import org.saudigitus.emis.ui.attendance2.models.AttendanceButtonModel
import org.saudigitus.emis.ui.attendance2.models.AttendanceButtonState
import org.saudigitus.emis.ui.attendance2.models.AttendanceEvent
import org.saudigitus.emis.ui.attendance2.models.AttendanceEventWithDecorator
import org.saudigitus.emis.ui.attendance2.models.AttendanceStatus
import org.saudigitus.emis.utils.Constants
import org.saudigitus.emis.utils.Constants.KEY
import org.saudigitus.emis.utils.DateHelper
import org.saudigitus.emis.utils.Utils.WHITE
import org.saudigitus.emis.utils.Utils.dynamicIcons
import org.saudigitus.emis.utils.Utils.getAttendanceStatusColor
import org.saudigitus.emis.utils.eventsWithTrackedDataValuesByDate
import org.saudigitus.emis.utils.optionsByOptionSetAndCode
import timber.log.Timber
import java.sql.Date
import javax.inject.Inject

class AttendanceRepositoryImpl @Inject constructor(
    private val d2: D2,
    private val repository: DataManager,
    private val transformations: AttendanceTransformation,
) : AttendanceRepository {

    private fun getAttributeOptionCombo() =
        d2.categoryModule().categoryOptionCombos()
            .byDisplayName().eq(Constants.DEFAULT).one().blockingGet()?.uid()

    private fun createEventProjection(
        enrollment: String? = null,
        ou: String,
        program: String,
        programStage: String,
    ): String {
        val projection = EventCreateProjection.builder()
            .organisationUnit(ou)
            .program(program).programStage(programStage)
            .attributeOptionCombo(getAttributeOptionCombo())

        return d2.eventModule().events()
            .blockingAdd(
                if (!enrollment.isNullOrEmpty()) {
                    projection.enrollment(enrollment).build()
                } else {
                    projection.build()
                }
            )
    }

    private fun eventUid(
        tei: String?,
        program: String,
        programStage: String,
        date: String?,
    ): String? {
        val eventsRepo = d2.eventModule().events()

        return if (!tei.isNullOrEmpty()) {
            eventsRepo
                .byTrackedEntityInstanceUids(listOf(tei))
                .byProgramUid().eq(program)
                .byProgramStageUid().eq(programStage)
                .byDeleted().isFalse
                .byEventDate().eq(Date.valueOf(date))
                .one().blockingGet()?.uid()
        } else {
            eventsRepo
                .byProgramUid().eq(program)
                .byProgramStageUid().eq(programStage)
                .byDeleted().isFalse
                .byEventDate().eq(Date.valueOf(date))
                .one().blockingGet()?.uid()
        }
    }

    private suspend fun saveEvent(
        event: String?,
        orgUnit: String,
        program: String,
        programStage: String,
        tei: SearchTeiModel?,
        data: Map<String, Pair<String, String?>>,
        eventDate: String?
    ) {
        withContext(Dispatchers.IO) {
            val date = eventDate ?: DateHelper.formatDate(System.currentTimeMillis())!!

            try {
                val uid = event ?: eventUid(
                    tei?.uid(),
                    program,
                    programStage,
                    date
                ) ?: createEventProjection(
                    tei?.selectedEnrollment?.uid(),
                    orgUnit,
                    program,
                    programStage,
                )

                val primaryDataValue = data["dataElement"]!!
                val secondaryDataValue = data.getOrElse("reasonDataElement") { null }

                d2.trackedEntityModule().trackedEntityDataValues()
                    .value(uid, primaryDataValue.first)
                    .blockingSet(primaryDataValue.second)

                if (secondaryDataValue != null) {
                    d2.trackedEntityModule().trackedEntityDataValues()
                        .value(uid, secondaryDataValue.first)
                        .blockingSet(secondaryDataValue.second)
                }

                val repository = d2.eventModule().events().uid(uid)
                repository.setEventDate(Date.valueOf(date))
                repository.setStatus(EventStatus.COMPLETED)
            } catch (e: Exception) {
                Timber.tag("SAVE_EVENT").e(e)
            }
        }
    }

    override suspend fun saveAttendance(
        program: String,
        programStage: String,
        attendanceEvents: List<AttendanceEventWithDecorator>
    ) {
        attendanceEvents.forEach { attendanceEvent ->
            saveEvent(
                event = attendanceEvent.event?.event,
                orgUnit = attendanceEvent.tei!!.tei.organisationUnit().orEmpty(),
                tei = attendanceEvent.tei,
                program = program,
                programStage = programStage,
                data = mapOf(
                    "dataElement" to Pair(
                        attendanceEvent.event?.dataElement.orEmpty(),
                        attendanceEvent.event?.value.orEmpty()
                    ),
                    "reasonDataElement" to Pair(
                        attendanceEvent.event?.reasonDataElement.orEmpty(),
                        attendanceEvent.event?.reasonOfAbsence
                    ),
                ),
                eventDate = attendanceEvent.event?.date
            )
        }
    }

    override suspend fun getAttendanceStatus(program: String, date: String): AttendanceStatus? =
        withContext(Dispatchers.IO) {
            val attendanceStatus = repository.getConfig(KEY)?.find { it.program == program }
                ?.attendance
                ?.attendanceStatus

            if (attendanceStatus == null) return@withContext null

            val dataElement = d2.dataElement(attendanceStatus.status.orEmpty())

            val event = d2.eventsWithTrackedDataValuesByDate(
                program = attendanceStatus.program.orEmpty(),
                stage = attendanceStatus.programStage.orEmpty(),
                date = date
            )

            val dataValue = event?.trackedEntityDataValues()
                ?.find { dv -> dv.dataElement() == dataElement?.uid() }

            AttendanceStatus(
                event = event?.uid(),
                program = attendanceStatus.program,
                programStage = attendanceStatus.programStage,
                dataElement = dataElement?.uid(),
                displayName = dataElement?.displayFormName(),
                value = dataValue?.value()
            )
        }

    override suspend fun loadAttendanceEvents(
        teiUids: List<String>,
        program: String,
        programStage: String,
        dataElement: String,
        reasonDataElement: String,
        eventDate: String?,
        attendanceButtonState: AttendanceButtonState
    ): AttendanceButtonState = withContext(Dispatchers.IO) {
        val config = repository.getConfig(KEY)?.find { it.program == program }
        val attendanceConfig = config?.attendance

        val attendanceEvents = getAttendanceEvent(
            teiUids = teiUids,
            program = program,
            programStage = programStage,
            dataElement = attendanceConfig?.status.orEmpty(),
            reasonDataElement = attendanceConfig?.absenceReason.orEmpty(),
            eventDate = eventDate ?: DateHelper.formatDate(System.currentTimeMillis())
                .orEmpty()
        )

        buttonState(program, attendanceButtonState, attendanceEvents)
    }

    override suspend fun updateAttendanceEvent(
        eventDate: String?,
        tei: SearchTeiModel?,
        attendanceButtonState: AttendanceButtonState,
        buttonModel: AttendanceButtonModel
    ): AttendanceButtonState {
        val attendanceEvents = attendanceButtonState.attendanceEvents.toMutableList()

        val event = attendanceEvents.find {
            it.event?.tei == tei?.uid()
        }

        if (event != null) {
            val reasonOfAbsence = if (buttonModel.key == Constants.ABSENT) {
                event.event?.reasonOfAbsence
            } else null

            val updatedEvent = event.event?.copy(
                value = buttonModel.code.orEmpty(),
                reasonOfAbsence = reasonOfAbsence
            )
            val eventWithDecorator = event.copy(
                event = updatedEvent,
                decorator = AttendanceButtonDecorator(
                    buttonType = buttonModel.code.orEmpty(),
                    containerColor = buttonModel.color ?: getAttendanceStatusColor(
                        buttonModel.code.orEmpty()
                    ),
                    contentColor = WHITE
                ),
            )

            val hasBeenRemoved = attendanceEvents.removeIf { it.event?.tei == tei?.uid() }

            if (hasBeenRemoved) {
                attendanceEvents.add(eventWithDecorator)
            }
        } else {
            attendanceEvents.add(
                AttendanceEventWithDecorator(
                    tei = tei,
                    event = AttendanceEvent(
                        tei = tei?.uid().orEmpty(),
                        enrollment = tei?.selectedEnrollment?.uid().orEmpty(),
                        dataElement = buttonModel.dataElement.orEmpty(),
                        value = buttonModel.code.orEmpty(),
                        date = eventDate ?: DateHelper.formatDate(System.currentTimeMillis())
                            .orEmpty()
                    ),
                    decorator = AttendanceButtonDecorator(
                        buttonType = buttonModel.code.orEmpty(),
                        containerColor = buttonModel.color ?: getAttendanceStatusColor(
                            buttonModel.code.orEmpty()
                        ),
                        contentColor = WHITE
                    ),
                    icon = buttonModel.icon
                        ?: dynamicIcons(buttonModel.iconName.orEmpty()),
                    iconName = buttonModel.iconName.orEmpty(),
                    iconColor = Color.White,
                )
            )
        }

        return attendanceButtonState.copy(attendanceEvents = attendanceEvents)
    }

    override fun attendanceSummary(
        attendanceButtonState: AttendanceButtonState,
    ): List<Summary> {
        val options = attendanceButtonState.buttons

        val summaries = options.map { option ->
            val count =
                attendanceButtonState.attendanceEvents.count { option.code == it.event?.value }

            Summary(
                icon = option.icon,
                iconName = option.iconName,
                count = count,
                color = option.color
            )
        }

        return summaries
    }

    override fun hasInvalidData(
        attendanceButtonState: AttendanceButtonState,
    ): Boolean {
        val events = attendanceButtonState.attendanceEvents.mapNotNull { it.event }
        val option =
            attendanceButtonState.buttons.find { it.key == Constants.ABSENT } ?: return false

        return events.filter { it.value == option.code }
            .any { it.reasonOfAbsence.isNullOrEmpty() }
    }

    override fun isAttendanceCompleted(
        students: List<SearchTeiModel>,
        attendanceButtonState: AttendanceButtonState
    ): Boolean {
        val events = attendanceButtonState.attendanceEvents.mapNotNull { it.event }

        return students.size == events.size && !hasInvalidData(attendanceButtonState)
    }

    override fun clearAll(attendanceButtonState: AttendanceButtonState): AttendanceButtonState {
        return attendanceButtonState.copy(attendanceEvents = emptyList())
    }

    private suspend fun buttonState(
        program: String,
        attendanceButtonState: AttendanceButtonState,
        attendanceEvents: List<AttendanceEventWithDecorator>
    ): AttendanceButtonState {
        val options = getAttendanceStatusOptions(program)

        return attendanceButtonState.copy(
            isLoading = false,
            buttons = options,
            attendanceEvents = attendanceEvents
        )
    }

    private suspend fun getOptionsByCode(
        dataElement: String,
        codes: List<String>
    ) = withContext(Dispatchers.IO) {
        val optionSet = d2.dataElement(dataElement)?.optionSetUid()

        d2.optionsByOptionSetAndCode(optionSet, codes)
    }

    private suspend fun getAttendanceStatusOptions(program: String) =
        withContext(Dispatchers.IO) {
            val config = repository.getConfig(KEY)?.find { it.program == program }
            val optionsCode = config?.attendance?.statusOptions
                ?.mapNotNull { it.code?.trim() }
                ?.filterNot { it.isEmpty() } ?: emptyList()

            val options = getOptionsByCode(
                config?.attendance?.status.orEmpty(),
                optionsCode
            )

            options.mapNotNull {
                val status = config?.attendance?.statusOptions?.find { status ->
                    status.code == it.code()
                }

                if (status == null) return@mapNotNull null

                AttendanceButtonModel(
                    key = status.key.orEmpty(),
                    code = it.code(),
                    name = it.displayName(),
                    dataElement = config.attendance.status.orEmpty(),
                    icon = dynamicIcons(status.icon.orEmpty()),
                    iconName = status.icon.orEmpty(),
                    color = getAttendanceStatusColor(
                        status.key.orEmpty(),
                        status.color.orEmpty()
                    ),
                    order = it.sortOrder() ?: -1,
                )
            }
        }

    private suspend fun getAttendanceEvent(
        teiUids: List<String>,
        program: String,
        programStage: String,
        dataElement: String,
        reasonDataElement: String,
        eventDate: String?
    ) = withContext(Dispatchers.IO) {
        val config = repository.getConfig(KEY)?.find { it.program == program }

        val date = if (eventDate != null) {
            Date.valueOf(eventDate)
        } else {
            DateUtils.getInstance().today
        }

        d2.eventModule().events()
            .byTrackedEntityInstanceUids(teiUids)
            .byProgramUid().eq(program)
            .byProgramStageUid().eq(programStage)
            .byDeleted().isFalse
            .byEventDate().eq(date)
            .withTrackedEntityDataValues()
            .blockingGet()
            .map {
                transformations.teiEventTransform(
                    eventUid = it.uid(),
                    program = program,
                    attendanceDataElement = dataElement,
                    reasonDataElement = reasonDataElement,
                    config = config?.attendance,
                )
            }
    }

}