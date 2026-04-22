package org.dhis2.usescases.teiDashboard.dashboardfragments.indicators

import dagger.Module
import dagger.Provides
import dhis2.org.analytics.charts.Charts
import org.dhis2.commons.network.NetworkUtils
import org.dhis2.commons.di.dagger.PerFragment
import org.dhis2.commons.resources.ResourceManager
import org.dhis2.commons.schedulers.SchedulerProvider
import org.dhis2.mobileProgramRules.RuleEngineHelper
import org.hisp.dhis.android.core.D2
import org.saudigitus.emis.data.local.AnalyticsRepository
import org.saudigitus.emis.data.local.DataManager
import org.saudigitus.emis.data.local.repository.AnalyticsRepositoryImpl
import org.saudigitus.emis.data.local.repository.DataManagerImpl
import org.saudigitus.emis.service.RuleEngineRepository
import org.saudigitus.emis.ui.home.analytics.AnalyticsViewModel
import org.saudigitus.emis.ui.home.analytics.AnalyticsViewModelFactory
import org.saudigitus.emis.utils.ProgramValidator
import org.saudigitus.emis.utils.Transformations
import javax.inject.Provider

@Module
class IndicatorsModule(
    val programUid: String,
    val recordUid: String,
    val view: IndicatorsView,
    private val visualizationType: VisualizationType,
) {
    @Provides
    @PerFragment
    fun providesPresenter(
        schedulerProvider: SchedulerProvider,
        indicatorRepository: IndicatorRepository,
    ): IndicatorsPresenter = IndicatorsPresenter(schedulerProvider, view, indicatorRepository)

    @Provides
    @PerFragment
    fun provideRepository(
        d2: D2,
        ruleEngineHelper: RuleEngineHelper?,
        charts: Charts?,
        resourceManager: ResourceManager,
    ): IndicatorRepository =
        if (visualizationType == VisualizationType.TRACKER) {
            TrackerAnalyticsRepository(
                d2,
                ruleEngineHelper,
                charts,
                programUid,
                recordUid,
                resourceManager,
            )
        } else {
            EventIndicatorRepository(
                d2,
                ruleEngineHelper,
                programUid,
                recordUid,
                resourceManager,
            )
        }

    @Provides
    @PerFragment
    fun providesTransformations(d2: D2): Transformations = Transformations(d2)

    @Provides
    @PerFragment
    fun providesRuleEngineRepository(d2: D2): RuleEngineRepository = RuleEngineRepository(d2)

    @Provides
    @PerFragment
    fun providesDataManager(
        d2: D2,
        transformations: Transformations,
        networkUtils: NetworkUtils,
        ruleEngineRepository: RuleEngineRepository,
    ): DataManager = DataManagerImpl(d2, transformations, networkUtils, ruleEngineRepository)

    @Provides
    @PerFragment
    fun providesAnalyticsRepository(
        d2: D2,
        dataManager: DataManager,
        resourceManager: ResourceManager
    ): AnalyticsRepository = AnalyticsRepositoryImpl(d2, dataManager, resourceManager)

    @Provides
    @PerFragment
    fun provideAnalyticsViewModelFactory(
        repository: AnalyticsRepository
    ): AnalyticsViewModelFactory {
        return AnalyticsViewModelFactory(repository)
    }

    @Provides
    @PerFragment
    fun provideProgramValidator(d2: D2): ProgramValidator = ProgramValidator(d2)
}
