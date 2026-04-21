package org.saudigitus.emis.data.local.repository

import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.dhis2.commons.resources.ResourceManager
import org.hisp.dhis.android.core.D2
import org.hisp.dhis.android.core.period.DatePeriod
import org.saudigitus.emis.data.local.AnalyticsRepository
import org.saudigitus.emis.data.local.DataManager
import org.saudigitus.emis.data.model.Analytic
import org.saudigitus.emis.data.model.AnalyticGroup
import org.saudigitus.emis.data.model.AnalyticSettings
import org.saudigitus.emis.data.model.AttendanceIndicator
import org.saudigitus.emis.data.model.app_config.EMISConfig
import org.saudigitus.emis.data.model.Indicator
import org.saudigitus.emis.data.model.Visualization
import org.saudigitus.emis.data.model.schoolcalendar_config.AcademicYear
import org.saudigitus.emis.utils.Constants
import org.saudigitus.emis.utils.Utils
import org.saudigitus.emis.utils.decodeJson
import timber.log.Timber
import java.sql.Date
import javax.inject.Inject

class AnalyticsRepositoryImpl
@Inject constructor(
    private val d2: D2,
    private val repository: DataManager,
    private val resourceManager: ResourceManager,
) : AnalyticsRepository {

    private suspend fun getAttendanceStage(program: String): String {
        val config = repository.getConfig(Constants.KEY)?.find { it.program == program }

        return config?.attendance?.programStage.orEmpty()
    }

    private suspend fun getAcademicYearDates(): AcademicYear? {
        val config = repository.dateValidation(Constants.CALENDAR_KEY)
        val default = config?.defaults

        return config?.schoolCalendar?.find {
            it?.academicYear?.code == default?.academicYear
        }?.academicYear
    }

    private fun getAnalyticsSettings(program: String): Analytic? {
        val dataStore = d2.dataStoreModule()
            .dataStore()
            .byNamespace().eq("semis")
            .byKey().eq("analytics")
            .one().blockingGet()

        val settings = EMISConfig.translateFromJson<AnalyticSettings>(decodeJson(dataStore?.value()))
            ?.tei?.find { it.program == program }

        return settings
    }

    private fun getProgramIndicatorColor(programIndicator: String): Color? {
        return try {
            val color = d2.programModule().programIndicators()
                .byUid().eq(programIndicator)
                .withLegendSets()
                .one().blockingGet()
                ?.legendSets()?.let {
                    val uid = it.first().uid()

                    if (it.isNotEmpty()) {
                        val legends = d2.legendSetModule().legends()
                            .byLegendSet().eq(uid).blockingGet()
                        if (legends.isNotEmpty()) {
                            legends.first().color() ?: ""
                        } else {
                            null
                        }
                    } else {
                        null
                    }
                }

            Color(resourceManager.getColorFrom(color))
        } catch (e: Exception) {
            Timber.tag("Color").e("$e")
            null
        }
    }

    private fun fetchIndicators(
        tei: String,
        programStage: String,
        startDate: String,
        endDate: String,
        visualizations: List<Visualization>,
        widgetDisplayName: String,
        type: String
    ): List<Indicator> {
        return visualizations.flatMap { visualization ->
            d2.analyticsModule().eventLineList()
                .byTrackedEntityInstance().eq(tei)
                .byProgramStage().eq(programStage)
                .byEventDate().inDatePeriods(
                    DatePeriod.create(
                        Date.valueOf(startDate),
                        Date.valueOf(endDate)
                    )
                )
                .withProgramIndicator(visualization.programIndicator)
                .blockingEvaluate()
                .flatMap { analytic ->
                    analytic.values.map { data ->
                        Indicator(
                            uid = data.uid,
                            programIndicator = visualization.programIndicator,
                            label = widgetDisplayName,
                            name = data.displayName,
                            value = data.value,
                            type = type,
                        )
                    }
                }
        }
    }

    private fun aggregateIndicators(indicators: List<Indicator>): List<AnalyticGroup> {
        val groupedByLabelAndType = indicators.groupBy { it.label to it.type }

        return groupedByLabelAndType.map { (labelTypePair, items) ->
            val (label, type) = labelTypePair

            val groupedByName = items.groupBy { it.name }

            val attendanceIndicators = groupedByName.map { (name, nameItems) ->
                val aggregatedValue = nameItems.sumOf { indicator ->
                    indicator.value?.toIntOrNull() ?: 0
                }

                val style = nameItems.associate { it.name to it.programIndicator }

                AttendanceIndicator(
                    uid = Utils.generateRandomId(),
                    name = name,
                    value = aggregatedValue.toString(),
                    color = getProgramIndicatorColor(style[name].orEmpty())
                )
            }

            AnalyticGroup(
                uid = Utils.generateRandomId(),
                displayName = label,
                type = type,
                attendanceIndicators = attendanceIndicators
            )
        }
    }

    override suspend fun getAnalyticsGroup(
        tei: String,
        program: String,
    ): List<AnalyticGroup> = withContext(Dispatchers.IO) {
        val academicYear = getAcademicYearDates()
        val analyticSettings = getAnalyticsSettings(program)

        val indicators = analyticSettings?.widgets?.flatMap { widget ->
            widget.visualizations.toSet()
                .groupBy { it.type }
                 .flatMap { (type, visualizations) ->
                    fetchIndicators(
                        tei = tei,
                        programStage = getAttendanceStage(program),
                        startDate = academicYear?.startDate.orEmpty(),
                        endDate = academicYear?.endDate.orEmpty(),
                        visualizations = visualizations,
                        widgetDisplayName = widget.displayName,
                        type = type
                    )
                }
        } ?: emptyList()

        return@withContext aggregateIndicators(indicators)
    }
}