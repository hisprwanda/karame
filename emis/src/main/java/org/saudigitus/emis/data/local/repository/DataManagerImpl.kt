package org.saudigitus.emis.data.local.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import org.dhis2.commons.bindings.dataElement
import org.dhis2.commons.bindings.enrollment
import org.dhis2.commons.date.DateUtils
import org.dhis2.commons.network.NetworkUtils
import org.hisp.dhis.android.core.D2
import org.hisp.dhis.android.core.arch.repositories.scope.RepositoryScope
import org.hisp.dhis.android.core.dataelement.DataElement
import org.hisp.dhis.android.core.event.EventCreateProjection
import org.hisp.dhis.android.core.event.EventStatus
import org.saudigitus.emis.data.local.DataManager
import org.saudigitus.emis.data.local.util.SqlRaw
import org.saudigitus.emis.data.model.SearchTeiModel
import org.saudigitus.emis.data.model.Subject
import org.saudigitus.emis.data.model.TransferredTei
import org.saudigitus.emis.data.model.app_config.Defaults
import org.saudigitus.emis.data.model.app_config.EMISConfig
import org.saudigitus.emis.data.model.app_config.EMISConfigItem
import org.saudigitus.emis.data.model.app_config.ProgramStages
import org.saudigitus.emis.data.model.app_config.TransferEvent
import org.saudigitus.emis.data.model.dto.AttendanceEntity
import org.saudigitus.emis.data.model.dto.withBtnSettings
import org.saudigitus.emis.data.model.schoolcalendar_config.SchoolCalendarConfig
import org.saudigitus.emis.service.RuleEngineRepository
import org.saudigitus.emis.ui.attendance.AttendanceOption
import org.saudigitus.emis.ui.components.DropdownItem
import org.saudigitus.emis.utils.Constants
import org.saudigitus.emis.utils.Transformations
import org.saudigitus.emis.utils.Utils
import org.saudigitus.emis.utils.Utils.getAttendanceStatusColor
import org.saudigitus.emis.utils.Utils.mapToType
import org.saudigitus.emis.utils.decodeJson
import org.saudigitus.emis.utils.eventsWithTrackedDataValues
import org.saudigitus.emis.utils.optionByOptionSet
import org.saudigitus.emis.utils.optionsByOptionSetAndCode
import org.saudigitus.emis.utils.optionsNotInOptionGroup
import org.saudigitus.emis.utils.optionsNotInOptionsSets
import timber.log.Timber
import java.sql.Date
import javax.inject.Inject

class DataManagerImpl
@Inject constructor(
    val d2: D2,
    private val transformations: Transformations,
    val networkUtils: NetworkUtils,
    val ruleEngineRepository: RuleEngineRepository,
) : DataManager {

    private fun getAttributeOptionCombo() =
        d2.categoryModule().categoryOptionCombos()
            .byDisplayName().eq(Constants.DEFAULT).one().blockingGet()?.uid()

    private fun createEventProjection(
        enrollment: String,
        ou: String,
        program: String,
        programStage: String,
    ): String {
        return d2.eventModule().events()
            .blockingAdd(
                EventCreateProjection.builder()
                    .organisationUnit(ou)
                    .program(program).programStage(programStage)
                    .attributeOptionCombo(getAttributeOptionCombo())
                    .enrollment(enrollment).build(),
            )
    }

    private fun eventUid(
        tei: String,
        program: String,
        programStage: String,
        date: String?,
    ): String? {
        return d2.eventModule().events()
            .byTrackedEntityInstanceUids(listOf(tei))
            .byProgramUid().eq(program)
            .byProgramStageUid().eq(programStage)
            .byEventDate().eq(Date.valueOf(date.toString()))
            .one().blockingGet()?.uid()
    }

    override suspend fun save(
        ou: String,
        program: String,
        programStage: String,
        attendance: AttendanceEntity,
    ): Unit =
        withContext(Dispatchers.IO) {
            try {
                val oldEvent = eventUid(
                    attendance.tei,
                    program,
                    programStage,
                    attendance.date,
                )

                if (!oldEvent.isNullOrEmpty()) {
                    d2.eventModule().events().uid(oldEvent).blockingDeleteIfExist()
                }

                val uid = createEventProjection(
                    attendance.enrollment,
                    ou,
                    program,
                    programStage,
                )

                d2.trackedEntityModule().trackedEntityDataValues()
                    .value(uid, attendance.dataElement)
                    .blockingSet(attendance.value)

                if (attendance.reasonDataElement != null && attendance.reasonOfAbsence != null) {
                    attendance.reasonOfAbsence.let {
                        if (it.isNotEmpty() && it.isNotBlank()) {
                            d2.trackedEntityModule().trackedEntityDataValues()
                                .value(uid, attendance.reasonDataElement).blockingSet(it)
                        }
                    }
                }

                val repository = d2.eventModule().events().uid(uid)
                repository.setEventDate(Date.valueOf(attendance.date))
                repository.setStatus(EventStatus.COMPLETED)
            } catch (e: Exception) {
                Timber.tag("SAVE_EVENT").e(e)
            }
        }

    override suspend fun save(
        ou: String,
        program: String,
        programStage: String,
        attendances: List<AttendanceEntity>
    ) = withContext(Dispatchers.IO) {
        attendances.forEach { attendance ->
            save(
                ou,
                program,
                programStage,
                attendance
            )
        }
    }

    override suspend fun getConfig(id: String): List<EMISConfigItem>? =
        withContext(Dispatchers.IO) {
            val dataStore = d2.dataStoreModule()
                .dataStore()
                .byNamespace().eq("semis")
                .byKey().eq(id)
                .one().blockingGet()

            return@withContext EMISConfig.fromJson(decodeJson(dataStore?.value()))
        }

    override suspend fun getTrackedEntityType(program: String) = withContext(Dispatchers.IO) {
        return@withContext d2.programModule().programs()
            .byUid().eq(program)
            .withTrackedEntityType()
            .one().blockingGet()
            ?.trackedEntityType()
            ?.displayName()
    }

    override suspend fun getOptions(
        ou: String?,
        program: String?,
        dataElement: String,
    ): List<DropdownItem> = withContext(Dispatchers.IO) {
        val optionSet = d2.dataElement(dataElement)?.optionSetUid()

        val hideOptions = ruleEngineRepository.applyOptionRules(ou, program.orEmpty(), dataElement)

        val rawOptions = when {
            hideOptions.isEmpty() -> d2.optionByOptionSet(optionSet)
            ou != null -> d2.optionsNotInOptionGroup(hideOptions, optionSet)
            else -> d2.optionsNotInOptionsSets(hideOptions, optionSet)
        }

        return@withContext rawOptions
            .map {
                DropdownItem(
                    id = it.uid(),
                    itemName = it.displayName().orEmpty(),
                    code = it.code().orEmpty(),
                    sortOrder = it.sortOrder()
                )
            }
            .sortedBy { it.sortOrder }
    }

    override suspend fun getOptionsByCode(
        dataElement: String,
        codes: List<String>,
    ): List<DropdownItem> = withContext(Dispatchers.IO) {
        val optionSet = d2.dataElement(dataElement)?.optionSetUid()

        return@withContext d2.optionsByOptionSetAndCode(optionSet, codes).map {
            DropdownItem(
                id = it.uid(),
                itemName = "${it.displayName()}",
                code = it.code() ?: "",
                sortOrder = it.sortOrder(),
            )
        }
    }

    override suspend fun getAttendanceOptions(
        program: String,
    ) = withContext(Dispatchers.IO) {
        val config = getConfig(Constants.KEY)?.find { it.program == program }
            ?.attendance ?: return@withContext emptyList()

        val optionsCode = config.statusOptions?.mapNotNull { it.code } ?: emptyList()

        return@withContext getOptionsByCode("${config.status}", optionsCode).mapNotNull {
            val status = config.statusOptions?.find { status ->
                status.code == it.code
            }

            if (status != null) {
                AttendanceOption(
                    code = it.code,
                    key = status.key,
                    name = it.itemName,
                    dataElement = "${config.status}",
                    icon = Utils.dynamicIcons("${status.icon}"),
                    iconName = "${status.icon}",
                    color = getAttendanceStatusColor("${status.key}", "${status.color}"),
                    actionOrder = it.sortOrder,
                )
            } else {
                null
            }
        }.sortedWith(compareBy { it.actionOrder })
    }

    override suspend fun getDataElement(uid: String): DataElement? =
        withContext(Dispatchers.IO) {
            return@withContext d2.dataElement(uid)
        }

    override suspend fun getTransferredTeis(orgUnit: String) =
        withContext(Dispatchers.IO) {
            try {
                val dataStore = d2.dataStoreModule()
                    .dataStore()
                    .byNamespace().eq("semis")
                    .byKey().eq("transfers")
                    .one().blockingGet()

                val transferred = EMISConfig.translateFromJson<TransferEvent>(decodeJson(dataStore?.value()))
                    ?: return@withContext emptyList()

                val requiredDataElements = listOfNotNull(
                    transferred.academicYear,
                    transferred.enrollment,
                    transferred.trackerId,
                )

                d2.eventModule().events()
                    .byProgramUid().eq(transferred.program)
                    .byOrganisationUnitUid().eq(orgUnit)
                    .withTrackedEntityDataValues()
                    .byDeleted().isFalse
                    .blockingGet()
                    .filter { event ->
                        val values = event.trackedEntityDataValues().orEmpty()

                        val presentElements = values
                            .filter { !it.value().isNullOrEmpty() }
                            .map { it.dataElement() }

                        requiredDataElements.all { it in presentElements }
                    }
                    .flatMap { event ->
                        event.trackedEntityDataValues().orEmpty()
                            .mapNotNull { dataValue ->
                                val de = dataValue.dataElement()
                                val value = dataValue.value()

                                if (de in requiredDataElements && !value.isNullOrEmpty()) {
                                    val type = mapToType(de.orEmpty(), transferred)

                                    type?.let {
                                        TransferredTei(
                                            dataElement = de.orEmpty(),
                                            value = value,
                                            type = it
                                        )
                                    }
                                } else null
                            }
                    }
            } catch (_: Exception) {
                emptyList()
            }
        }

    override fun getTeisBy(
        ou: String,
        program: String,
        stage: String,
        dataElementIds: List<String>,
        dataValues: List<String>,
    ): Flow<List<SearchTeiModel>> = flow {
        val default = getDefaultFromEmisConfig(program)
        val defaultOrder = default?.defaultOrder?.split(":")
        val attrId = defaultOrder?.firstOrNull().orEmpty()
        val order = if (defaultOrder?.lastOrNull()
                .orEmpty().lowercase() == "desc"
        ) RepositoryScope.OrderByDirection.DESC
        else RepositoryScope.OrderByDirection.ASC

        emit(
            d2.trackedEntityModule().trackedEntityInstanceQuery()
                .byOrgUnits().eq(ou)
                .byProgram().eq(program)
                .byProgramStage().eq(stage)
                .orderByAttribute(attrId).eq(order)
                .blockingGet().flatMap {
                    d2.eventsWithTrackedDataValues(
                        ou,
                        program,
                        stage,
                        it.uid()
                    ).filter { event ->
                        val dataElements =
                            event.trackedEntityDataValues()?.associate { trackedEntityDataValue ->
                                Pair(
                                    trackedEntityDataValue.dataElement(),
                                    trackedEntityDataValue.value()
                                )
                            }
                        dataElements?.keys?.containsAll(dataElementIds) == true &&
                            dataElements.values.containsAll(dataValues)
                    }.mapNotNull { event ->
                        d2.enrollment(event.enrollment().orEmpty())
                    }.mapNotNull { enrollment ->
                        val transferred = getTransferredTeis(ou)
                        val dataValues = transferred.map { dataValue -> dataValue.value }

                        if (!dataValues.contains(enrollment.trackedEntityInstance())) {
                            transformations.transform(it, program, enrollment)
                        } else null
                    }
                }
        )
    }.buffer()
        .conflate()
        .flowOn(Dispatchers.IO)

    @Throws(IllegalArgumentException::class)
    override suspend fun getAttendanceEvent(
        program: String,
        programStage: String,
        dataElement: String,
        reasonDataElement: String?,
        teis: List<String>,
        date: String?,
    ) = withContext(Dispatchers.IO) {
        val config = getConfig(Constants.KEY)?.find { it.program == program }
            ?.attendance ?: return@withContext emptyList()

        d2.eventModule().events()
            .byTrackedEntityInstanceUids(teis)
            .byProgramUid().eq(program)
            .byProgramStageUid().eq(programStage)
            .byDeleted().isFalse
            .byEventDate().eq(
                if (date != null) {
                    Date.valueOf(date)
                } else {
                    DateUtils.getInstance().today
                },
            )
            .withTrackedEntityDataValues()
            .blockingGet()
            .mapNotNull {
                transformations.eventTransform(it, dataElement, reasonDataElement)
            }
            .map { attendanceEntity ->
                val status = config.statusOptions?.find { status ->
                    status.code == attendanceEntity.value
                }

                attendanceEntity.withBtnSettings(
                    icon = Utils.dynamicIcons("${status?.icon}"),
                    iconName = "${status?.icon}",
                    iconColor = getAttendanceStatusColor("${status?.key}", "${status?.color}"),
                )
            }
    }

    override suspend fun deleteEvent(
        tei: String,
        enrollment: String,
        eventDate: String
    ) = withContext(Dispatchers.IO) {
        val event = d2.eventModule().events()
            .byTrackedEntityInstanceUids(listOf(tei))
            .byEnrollmentUid().eq(enrollment)
            .byEventDate().eq(Date.valueOf(eventDate))
            .one()
            .blockingGet()

        d2.eventModule().events()
            .uid(event?.uid())
            .blockingDeleteIfExist()
    }

    override suspend fun deleteAllEvents(
        teis: List<String>,
        eventDate: String
    ) = withContext(Dispatchers.IO) {
        d2.eventModule().events()
            .byTrackedEntityInstanceUids(teis)
            .byEventDate().eq(Date.valueOf(eventDate))
            .blockingGetUids()
            .forEach {
                d2.eventModule().events()
                    .uid(it).blockingDeleteIfExist()
            }
    }

    override suspend fun geTeiByAttendanceStatus(
        ou: String,
        program: String,
        stage: String,
        attendanceStage: String,
        attendanceDataElement: String,
        reasonDataElement: String,
        date: String?,
        dataElementIds: List<String>,
        options: List<String>,
    ) = withContext(Dispatchers.IO) {
        val config = getConfig(Constants.KEY)?.find { it.program == program }
            ?.attendance ?: return@withContext emptyMap()

        val attendanceStatus = config.statusOptions?.find { status ->
            status.key == Constants.ABSENT
        }?.code.orEmpty()

        val data = mutableMapOf<SearchTeiModel, AttendanceEntity>()

        return@withContext data
    }

    override suspend fun dateValidation(id: String): SchoolCalendarConfig? =
        withContext(Dispatchers.IO) {
            return@withContext try {
                val dataStore = d2.dataStoreModule()
                    .dataStore()
                    .byNamespace().eq("semis")
                    .byKey().eq(id)
                    .one().blockingGet()

                EMISConfig.translateFromJson<SchoolCalendarConfig>(decodeJson(dataStore?.value()))
            } catch (_: Exception) {
                null
            }
        }

    override suspend fun getSubjects(stage: String) = withContext(Dispatchers.IO) {
        return@withContext d2.programModule().programStageDataElements()
            .byProgramStage().eq(stage)
            .blockingGet()
            .map { stageDl ->
                val dl = d2.dataElement(stageDl.dataElement()?.uid() ?: "")

                Subject(
                    uid = dl?.uid() ?: "",
                    code = dl?.code() ?: "",
                    color = dl?.style()?.color(),
                    displayName = dl?.displayFormName(),
                )
            }
    }

    override suspend fun getTerms(stages: List<ProgramStages>) = withContext(Dispatchers.IO) {
        val stagesIds = stages.mapNotNull { it.programStage }

        return@withContext d2.programModule().programStages()
            .byUid().`in`(stagesIds)
            .orderBySortOrder(RepositoryScope.OrderByDirection.ASC)
            .blockingGet()
            .map {
                DropdownItem(
                    id = it.uid(),
                    itemName = it.displayName() ?: "",
                    code = it.code(),
                )
            }
    }

    private suspend fun getDefaultFromEmisConfig(program: String): Defaults? {
        val config = getConfig(Constants.KEY)?.find { it.program == program }
            ?: return null

        return config.defaults
    }
}
