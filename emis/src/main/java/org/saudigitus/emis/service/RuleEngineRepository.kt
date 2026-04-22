package org.saudigitus.emis.service

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.dhis2.commons.bindings.event
import org.dhis2.commons.bindings.organisationUnit
import org.dhis2.commons.bindings.programStage
import org.dhis2.commons.rules.RuleEngineContextData
import org.dhis2.mobileProgramRules.toRuleDataValue
import org.dhis2.mobileProgramRules.toRuleEngineInstant
import org.dhis2.mobileProgramRules.toRuleEngineLocalDate
import org.dhis2.mobileProgramRules.toRuleEngineObject
import org.dhis2.mobileProgramRules.toRuleVariable
import org.hisp.dhis.android.core.D2
import org.hisp.dhis.android.core.event.EventStatus
import org.hisp.dhis.android.core.program.ProgramRuleActionType
import org.hisp.dhis.rules.api.RuleEngine
import org.hisp.dhis.rules.api.RuleEngineContext
import org.hisp.dhis.rules.models.Rule
import org.hisp.dhis.rules.models.RuleDataValue
import org.hisp.dhis.rules.models.RuleEvent
import org.hisp.dhis.rules.models.RuleEventStatus
import org.hisp.dhis.rules.models.RuleVariable
import org.saudigitus.emis.utils.DateHelper
import java.util.Collections
import javax.inject.Inject

class RuleEngineRepository @Inject constructor(
    private val d2: D2,
) {

    private val ruleEngine by lazy { RuleEngine.getInstance() }

    private suspend fun supplementaryData(ou: String) = withContext(Dispatchers.IO) {
        val suppData = HashMap<String, List<String>>()

        d2.organisationUnitModule().organisationUnits()
            .withOrganisationUnitGroups()
            .uid(ou).blockingGet()
            .let { orgUnit ->
                orgUnit?.organisationUnitGroups()?.mapNotNull {
                    if (it.code() != null) {
                        suppData[it.code()!!] = listOf(orgUnit.uid())
                    }
                    suppData[it.uid()] = listOf(orgUnit.uid())
                }
            }

        return@withContext suppData
    }

    private suspend fun ruleVariables(program: String) = withContext(Dispatchers.IO) {
        return@withContext d2.programModule().programRuleVariables()
            .byProgramUid().eq(program)
            .blockingGet()
            .map {
                it.toRuleVariable(
                    d2.optionModule().options(),
                    d2.trackedEntityModule().trackedEntityAttributes(),
                    d2.dataElementModule().dataElements(),
                )
            }
    }

    suspend fun rules(program: String) = withContext(Dispatchers.IO) {
        return@withContext d2.programModule().programRules()
            .byProgramUid().eq(program)
            .withProgramRuleActions()
            .blockingGet()
            .map {
                it.toRuleEngineObject()
            }
    }

    suspend fun constants() = withContext(Dispatchers.IO) {
        return@withContext d2.constantModule()
            .constants().blockingGet()
            .associate { constant ->
                Pair(constant.uid(), "${constant.value()}")
            }
    }

    @Suppress("DEPRECATION")
    private suspend fun ruleEvents(
        ou: String,
        program: String,
    ) = withContext(Dispatchers.IO) {
        return@withContext d2.eventModule().events()
            .byOrganisationUnitUid().eq(ou)
            .byProgramUid().eq(program)
            .withTrackedEntityDataValues()
            .blockingGet()
            .map { event ->
                RuleEvent(
                    event = event.uid(),
                    programStage = event.programStage()!!,
                    programStageName = d2.programModule().programStages()
                        .uid(event.programStage())
                        .blockingGet()!!.name()!!,
                    status = if (event.status() == EventStatus.VISITED) {
                        RuleEventStatus.ACTIVE
                    } else {
                        RuleEventStatus.valueOf(event.status()!!.name)
                    },
                    eventDate = Instant.fromEpochMilliseconds(event.eventDate()!!.time),
                    dueDate = event.dueDate()?.let {
                        Instant.fromEpochMilliseconds(it.time)
                            .toLocalDateTime(TimeZone.currentSystemDefault()).date
                    },
                    completedDate = event.completedDate()?.let {
                        Instant.fromEpochMilliseconds(it.time)
                            .toLocalDateTime(TimeZone.currentSystemDefault()).date
                    },
                    organisationUnit = event.organisationUnit()!!,
                    organisationUnitCode = d2.organisationUnitModule().organisationUnits()
                        .uid(
                            event.organisationUnit(),
                        ).blockingGet()?.code(),
                    dataValues = event.trackedEntityDataValues()?.toRuleDataValue() ?: emptyList(),
                    createdDate = Instant.fromEpochMilliseconds(event.created()!!.time),
                )
            }
    }

    private suspend fun ruleContext(
        ruleVariables: List<RuleVariable>,
        rules: List<Rule>,
        supplementaryData: Map<String, List<String>> = emptyMap(),
        constants: Map<String, String>,
    ) = withContext(Dispatchers.IO) {
        return@withContext RuleEngineContext(
            rules = rules,
            ruleVariables = ruleVariables,
            supplementaryData = supplementaryData,
            constantsValues = constants,
        )
    }

    private suspend fun executeContext(
        ou: String? = null,
        program: String,
    ) = withContext(Dispatchers.IO) {
        val rules = async { rules(program) }.await()
        val ruleVariables = ruleVariables(program)
        val constants = async { constants() }.await()
        val supplementaryData = ou?.let {
            async { supplementaryData(it) }.await()
        } ?: emptyMap()

        return@withContext ruleContext(
            ruleVariables,
            rules,
            supplementaryData,
            constants,
        )
    }

    private suspend fun ruleEngineContextData(
        ou: String,
        program: String,
    ) = withContext(Dispatchers.IO) {
        val rules = async { rules(program) }.await()
        val ruleVariables = ruleVariables(program)
        val constants = async { constants() }.await()
        val ruleEvents = async { ruleEvents(ou, program) }.await()
        val supplementaryData = async { supplementaryData(ou) }.await()

        return@withContext RuleEngineContextData(
            ruleEngineContext = ruleContext(
                ruleVariables,
                rules,
                supplementaryData,
                constants,
            ),
            ruleEnrollment = null,
            ruleEvents = ruleEvents,
        )
    }

    private fun getRuleEvent(
        eventUid: String,
        dataValues: List<RuleDataValue> = emptyList(),
    ): RuleEvent {
        val event = d2.event(eventUid) ?: throw NullPointerException()
        return RuleEvent(
            event = event.uid(),
            programStage = event.programStage()!!,
            programStageName = d2.programStage(event.programStage()!!)?.name()!!,
            status = RuleEventStatus.valueOf(event.status()!!.name),
            eventDate = event.eventDate()!!.toRuleEngineInstant(),
            dueDate = event.dueDate()?.toRuleEngineLocalDate(),
            completedDate = event.completedDate()?.toRuleEngineLocalDate(),
            organisationUnit = event.organisationUnit()!!,
            organisationUnitCode = d2.organisationUnit(event.organisationUnit()!!)?.code(),
            dataValues = dataValues,
            createdDate = Instant.fromEpochMilliseconds(event.created()!!.time),
        )
    }

    suspend fun applyOptionRules(
        ou: String? = null,
        program: String,
        dataElement: String,
    ) = withContext(Dispatchers.IO) {
        val ruleContext = executeContext(ou, program)
        ruleContext.rules
            .asSequence()
            .flatMap { it.actions.asSequence() }
            .filter {
                it.type in setOf(
                    ProgramRuleActionType.HIDEOPTION.name,
                    ProgramRuleActionType.HIDEOPTIONGROUP.name
                )
            }
            .mapNotNull { action ->
                val fieldMatches = action.values["field"] == dataElement
                if (!fieldMatches) return@mapNotNull null

                when (action.type) {
                    ProgramRuleActionType.HIDEOPTION.name -> action.values["option"]
                    ProgramRuleActionType.HIDEOPTIONGROUP.name -> action.values["optionGroup"]
                    else -> null
                }
            }
            .toList()
    }

    private fun dataEntry(
        dataElement: String,
        value: String,
    ) = RuleDataValue(
        dataElement = dataElement,
        value = value
    )

    suspend fun evaluateDataEntry(
        ou: String,
        program: String,
        stage: String,
        dataElement: String,
        event: String,
        eventDate: String,
        value: String
    ) = evaluate(
        ou,
        program,
        event,
        Collections.singletonList(
            dataEntry(
                dataElement,
                value
            )
        )
    ).find { effect ->
        effect.ruleAction.type == ProgramRuleActionType.SHOWERROR.name
    }

    suspend fun evaluate(
        ou: String,
        program: String,
        event: String,
        dataValues: List<RuleDataValue> = emptyList()
    ) = withContext(Dispatchers.IO) {
        val ruleEngineContextData = ruleEngineContextData(ou, program)
        val events = ruleEngineContextData.ruleEvents.filter {
            it.event != event
        }

        return@withContext ruleEngine.evaluate(
            target = getRuleEvent(event, dataValues),
            ruleEnrollment = ruleEngineContextData.ruleEnrollment,
            ruleEvents = events,
            executionContext = ruleEngineContextData.ruleEngineContext,
        )
    }
}
